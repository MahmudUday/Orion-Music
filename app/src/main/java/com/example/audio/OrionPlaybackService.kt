package com.example.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.OrionMusicApp
import com.example.R
import com.example.data.local.ResumePlaybackSnapshot
import com.example.data.model.AudioFocusBehavior
import com.example.data.model.PlaybackSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.Charset

@OptIn(UnstableApi::class)
class OrionPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var baseVolumeFactor: Float = 1f
    private var sleepFadeFactor: Float = 1f
    private var crossfadeSeconds: Int = 0
    private var rememberPosition: Boolean = true
    private var lastAppliedCrossfadeItemId: String? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as OrionMusicApp
        val container = app.container

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(container.stereoAudioProcessor))
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setPauseAtEndOfMediaItems(false)
            .build()

        container.effectsController.attachToSession(player.audioSessionId)

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            1001,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(OrionMusicApp.CHANNEL_PLAYBACK)
            .setChannelName(R.string.notification_channel_playback_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_orion_notification)
        setMediaNotificationProvider(notificationProvider)

        container.sleepTimerManager.onVolumeFadeFactor = { factor ->
            sleepFadeFactor = factor
            updateEffectivePlayerVolume()
        }
        container.sleepTimerManager.onTimerTriggered = {
            player.pause()
        }

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                container.effectsController.attachToSession(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    container.sleepTimerManager.notifyTrackEnded()
                ) {
                    player.pause()
                    return
                }
                lastAppliedCrossfadeItemId = null
                baseVolumeFactor = 1f
                updateEffectivePlayerVolume()

                val songId = mediaItem?.mediaId?.toLongOrNull()
                if (songId != null) {
                    serviceScope.launch(Dispatchers.IO) {
                        container.repository.recordSongPlay(songId)
                        val song = container.repository.database.songDao().getSongById(songId)
                        val replayGainDb = song?.filePath?.let { extractReplayGainDb(File(it)) } ?: 0f
                        container.effectsController.setReplayGainDb(replayGainDb)
                    }
                }
                persistResumeSnapshot()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    persistResumeSnapshot()
                }
            }
        })

        // Observe settings changes live in service
        serviceScope.launch {
            container.settingsDataStore.audioEffectsFlow.collect { effects ->
                container.effectsController.applySettings(effects)
            }
        }
        serviceScope.launch {
            container.settingsDataStore.playbackFlow.collect { playback ->
                applyPlaybackSettings(playback)
            }
        }
        serviceScope.launch {
            container.settingsDataStore.notificationsFlow.collect { notif ->
                mediaSession?.setSessionActivity(sessionActivityIntent)
                // Notification controls are updated natively via MediaSession available commands
                val commandsBuilder = Player.Commands.Builder().addAllCommands()
                if (!notif.showSeekActionsInNotification) {
                    commandsBuilder.remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                }
            }
        }

        startCrossfadeAndPersistenceMonitor()
    }

    private fun applyPlaybackSettings(playback: PlaybackSettings) {
        crossfadeSeconds = playback.crossfadeSeconds.coerceIn(0, 12)
        rememberPosition = playback.rememberPosition
        player.pauseAtEndOfMediaItems = !playback.gaplessPlayback
        val handleFocus = playback.audioFocusBehavior == AudioFocusBehavior.PAUSE_ON_LOSS ||
            playback.audioFocusBehavior == AudioFocusBehavior.DUCK_WHEN_POSSIBLE
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            handleFocus
        )
        if (player.playbackParameters.speed != playback.defaultPlaybackSpeed ||
            player.playbackParameters.pitch != playback.defaultPitch
        ) {
            player.playbackParameters = PlaybackParameters(
                playback.defaultPlaybackSpeed.coerceIn(0.5f, 2.0f),
                playback.defaultPitch.coerceIn(0.5f, 2.0f)
            )
        }
    }

    private fun startCrossfadeAndPersistenceMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            var tickCounter = 0
            while (isActive) {
                if (player.isPlaying) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (crossfadeSeconds > 0 && duration > crossfadeSeconds * 2000L && player.hasNextMediaItem()) {
                        val fadeWindowMs = crossfadeSeconds * 1000L
                        val remaining = duration - position
                        if (remaining in 1..fadeWindowMs) {
                            val progressOut = (remaining.toFloat() / fadeWindowMs.toFloat()).coerceIn(0.05f, 1f)
                            baseVolumeFactor = progressOut
                            updateEffectivePlayerVolume()
                            lastAppliedCrossfadeItemId = player.currentMediaItem?.mediaId
                        } else if (position in 0..fadeWindowMs && lastAppliedCrossfadeItemId != null) {
                            val progressIn = (position.toFloat() / fadeWindowMs.toFloat()).coerceIn(0.15f, 1f)
                            baseVolumeFactor = progressIn
                            updateEffectivePlayerVolume()
                        } else if (baseVolumeFactor != 1f) {
                            baseVolumeFactor = 1f
                            updateEffectivePlayerVolume()
                        }
                    } else if (baseVolumeFactor != 1f) {
                        baseVolumeFactor = 1f
                        updateEffectivePlayerVolume()
                    }

                    tickCounter++
                    if (tickCounter % 10 == 0) {
                        persistResumeSnapshot()
                    }
                }
                delay(250L)
            }
        }
    }

    private fun updateEffectivePlayerVolume() {
        player.volume = (baseVolumeFactor * sleepFadeFactor).coerceIn(0f, 1f)
    }

    private fun persistResumeSnapshot() {
        if (!rememberPosition) return
        val app = application as? OrionMusicApp ?: return
        val queueIds = (0 until player.mediaItemCount).mapNotNull { idx ->
            player.getMediaItemAt(idx).mediaId.toLongOrNull()
        }
        val currentId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
        val pos = player.currentPosition.coerceAtLeast(0L)
        val shuffle = player.shuffleModeEnabled
        val repeatName = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> "ALL"
            Player.REPEAT_MODE_ONE -> "ONE"
            else -> "OFF"
        }
        serviceScope.launch(Dispatchers.IO) {
            app.container.repository.saveResumeSnapshot(
                ResumePlaybackSnapshot(
                    queueSongIds = queueIds,
                    currentSongId = currentId,
                    positionMs = pos,
                    shuffleEnabled = shuffle,
                    repeatModeName = repeatName
                )
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistResumeSnapshot()
        monitorJob?.cancel()
        serviceScope.cancel()
        val app = application as? OrionMusicApp
        app?.container?.effectsController?.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun extractReplayGainDb(file: File): Float {
        if (!file.exists() || !file.canRead() || file.length() < 128) return 0f
        return runCatching {
            file.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                val read = input.read(buffer)
                if (read <= 0) return 0f
                val ascii = String(buffer, 0, read, Charset.forName("ISO-8859-1"))
                val regex = Regex("""REPLAYGAIN_TRACK_GAIN[^\d+\-]*([+-]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                val match = regex.find(ascii)
                match?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
            }
        }.getOrDefault(0f)
    }
}
