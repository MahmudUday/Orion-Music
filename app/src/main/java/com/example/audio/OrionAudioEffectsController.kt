package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.model.AudioEffectSettings
import com.example.data.model.EqualizerPresetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
)

data class EqualizerHardwareState(
    val isAttached: Boolean = false,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val bands: List<EqBandInfo> = listOf(
        EqBandInfo(0, 60, 0),
        EqBandInfo(1, 230, 0),
        EqBandInfo(2, 910, 0),
        EqBandInfo(3, 3600, 0),
        EqBandInfo(4, 14000, 0)
    ),
    val bassBoostSupported: Boolean = true,
    val virtualizerSupported: Boolean = true,
    val loudnessSupported: Boolean = true
)

class OrionAudioEffectsController(
    private val stereoProcessor: OrionStereoAudioProcessor
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0
    private var latestSettings: AudioEffectSettings = AudioEffectSettings()
    private var currentReplayGainDb: Float = 0f

    private val _hardwareState = MutableStateFlow(EqualizerHardwareState())
    val hardwareState: StateFlow<EqualizerHardwareState> = _hardwareState.asStateFlow()

    @Synchronized
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) {
            applySettings(latestSettings)
            return
        }
        releaseEffects()
        currentSessionId = audioSessionId

        runCatching {
            equalizer = Equalizer(0, audioSessionId)
        }
        runCatching {
            bassBoost = BassBoost(0, audioSessionId)
        }
        runCatching {
            virtualizer = Virtualizer(0, audioSessionId)
        }
        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        }

        refreshHardwareDescriptor()
        applySettings(latestSettings)
    }

    @Synchronized
    fun setReplayGainDb(gainDb: Float) {
        currentReplayGainDb = gainDb.coerceIn(-12f, 12f)
        applyLoudnessAndNormalization(latestSettings)
    }

    @Synchronized
    fun applySettings(settings: AudioEffectSettings) {
        latestSettings = settings
        stereoProcessor.balance = settings.balance
        stereoProcessor.monoAudio = settings.monoAudio

        val eq = equalizer
        if (eq != null) {
            runCatching {
                eq.enabled = settings.eqEnabled
                val preset = EqualizerPresetType.entries.find { it.name == settings.presetName }
                val targetBands = if (preset != null && preset != EqualizerPresetType.CUSTOM) {
                    preset.bandMillibels
                } else {
                    settings.customBandLevelsMb
                }
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                for (i in 0 until numBands) {
                    val requested = targetBands.getOrElse(i) { 0 }.coerceIn(minMb, maxMb)
                    eq.setBandLevel(i.toShort(), requested.toShort())
                }
            }
        }

        bassBoost?.let { bb ->
            runCatching {
                val strength = settings.bassBoostStrength.coerceIn(0, 1000)
                bb.enabled = strength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(strength.toShort())
                }
            }
        }

        virtualizer?.let { virt ->
            runCatching {
                val strength = settings.virtualizerStrength.coerceIn(0, 1000)
                virt.enabled = strength > 0
                if (virt.strengthSupported) {
                    virt.setStrength(strength.toShort())
                }
            }
        }

        applyLoudnessAndNormalization(settings)
        refreshHardwareDescriptor()
    }

    private fun applyLoudnessAndNormalization(settings: AudioEffectSettings) {
        val le = loudnessEnhancer ?: return
        runCatching {
            val replayGainMb = if (settings.replayGainEnabled) (currentReplayGainDb * 100f).toInt() else 0
            val normalizationMb = if (settings.volumeNormalization) 350 else 0
            val totalGainMb = (replayGainMb + normalizationMb).coerceIn(-1200, 1200)
            le.enabled = totalGainMb != 0
            le.setTargetGain(totalGainMb)
        }
    }

    private fun refreshHardwareDescriptor() {
        val eq = equalizer
        if (eq == null) {
            val fallbackBands = latestSettings.customBandLevelsMb.mapIndexed { idx, lvl ->
                val defaultFreqs = listOf(60, 230, 910, 3600, 14000)
                EqBandInfo(idx, defaultFreqs.getOrElse(idx) { 1000 * (idx + 1) }, lvl)
            }
            _hardwareState.value = EqualizerHardwareState(
                isAttached = false,
                bands = fallbackBands,
                bassBoostSupported = true,
                virtualizerSupported = true,
                loudnessSupported = true
            )
            return
        }

        runCatching {
            val range = eq.bandLevelRange
            val numBands = eq.numberOfBands.toInt()
            val bands = (0 until numBands).map { i ->
                EqBandInfo(
                    index = i,
                    centerFreqHz = (eq.getCenterFreq(i.toShort()) / 1000).coerceAtLeast(20),
                    levelMb = eq.getBandLevel(i.toShort()).toInt()
                )
            }
            _hardwareState.value = EqualizerHardwareState(
                isAttached = true,
                minLevelMb = range[0].toInt(),
                maxLevelMb = range[1].toInt(),
                bands = bands,
                bassBoostSupported = bassBoost?.strengthSupported ?: false,
                virtualizerSupported = virtualizer?.strengthSupported ?: false,
                loudnessSupported = loudnessEnhancer != null
            )
        }
    }

    @Synchronized
    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
    }
}
