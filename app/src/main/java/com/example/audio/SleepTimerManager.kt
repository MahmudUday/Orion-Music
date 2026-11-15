package com.example.audio

import com.example.data.model.SleepTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SleepTimerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    var onVolumeFadeFactor: ((Float) -> Unit)? = null
    var onTimerTriggered: (() -> Unit)? = null

    fun startMinutesTimer(minutes: Int, fadeOutEnabled: Boolean) {
        if (minutes <= 0) {
            cancel()
            return
        }
        val durationMs = minutes * 60_000L
        val endMillis = System.currentTimeMillis() + durationMs
        launchCountdown(
            endMillis = endMillis,
            fadeOutEnabled = fadeOutEnabled,
            label = "$minutes min"
        )
    }

    fun startStopAtClockTime(hour24: Int, minute: Int, fadeOutEnabled: Boolean) {
        val now = LocalDateTime.now()
        var target = now.with(LocalTime.of(hour24.coerceIn(0, 23), minute.coerceIn(0, 59), 0))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val endMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formatted = target.format(DateTimeFormatter.ofPattern("HH:mm"))
        launchCountdown(
            endMillis = endMillis,
            fadeOutEnabled = fadeOutEnabled,
            label = "Until $formatted"
        )
    }

    fun startStopAfterCurrentSong(fadeOutEnabled: Boolean) {
        timerJob?.cancel()
        onVolumeFadeFactor?.invoke(1f)
        _state.value = SleepTimerState(
            isActive = true,
            endTimeMillis = 0L,
            remainingMillis = 0L,
            stopAfterCurrentSong = true,
            fadeOutEnabled = fadeOutEnabled,
            modeLabel = "End of track"
        )
    }

    fun notifyTrackEnded(): Boolean {
        val current = _state.value
        if (current.isActive && current.stopAfterCurrentSong) {
            cancel()
            return true
        }
        return false
    }

    private fun launchCountdown(endMillis: Long, fadeOutEnabled: Boolean, label: String) {
        timerJob?.cancel()
        onVolumeFadeFactor?.invoke(1f)
        _state.value = SleepTimerState(
            isActive = true,
            endTimeMillis = endMillis,
            remainingMillis = (endMillis - System.currentTimeMillis()).coerceAtLeast(0L),
            stopAfterCurrentSong = false,
            fadeOutEnabled = fadeOutEnabled,
            modeLabel = label
        )

        timerJob = scope.launch {
            while (true) {
                val remaining = endMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    onVolumeFadeFactor?.invoke(1f)
                    _state.value = SleepTimerState()
                    onTimerTriggered?.invoke()
                    break
                }
                if (fadeOutEnabled && remaining <= 15_000L) {
                    val factor = (remaining / 15_000f).coerceIn(0.05f, 1f)
                    onVolumeFadeFactor?.invoke(factor)
                } else {
                    onVolumeFadeFactor?.invoke(1f)
                }
                _state.value = _state.value.copy(remainingMillis = remaining)
                delay(500L)
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        onVolumeFadeFactor?.invoke(1f)
        _state.value = SleepTimerState()
    }
}
