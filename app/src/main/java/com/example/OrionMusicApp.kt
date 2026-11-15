package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.audio.OrionAudioEffectsController
import com.example.audio.OrionStereoAudioProcessor
import com.example.audio.SleepTimerManager
import com.example.data.local.OrionDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.OrionRepository
import com.example.data.scanner.MusicLibraryScanner

class AppContainer(context: Context) {
    val database: OrionDatabase = OrionDatabase.getInstance(context)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val scanner: MusicLibraryScanner = MusicLibraryScanner(context, database, settingsDataStore)
    val repository: OrionRepository = OrionRepository(context, database, settingsDataStore, scanner)
    val stereoAudioProcessor: OrionStereoAudioProcessor = OrionStereoAudioProcessor()
    val effectsController: OrionAudioEffectsController = OrionAudioEffectsController(stereoAudioProcessor)
    val sleepTimerManager: SleepTimerManager = SleepTimerManager()
}

class OrionMusicApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val playbackChannel = NotificationChannel(
            CHANNEL_PLAYBACK,
            getString(R.string.notification_channel_playback_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_playback_desc)
            setShowBadge(false)
        }
        val scanChannel = NotificationChannel(
            CHANNEL_SCANNER,
            getString(R.string.notification_channel_scan_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_channel_scan_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(playbackChannel)
        manager.createNotificationChannel(scanChannel)
    }

    companion object {
        const val CHANNEL_PLAYBACK = "orion_playback_channel"
        const val CHANNEL_SCANNER = "orion_scanner_channel"
    }
}
