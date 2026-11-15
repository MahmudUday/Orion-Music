package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppearanceSettings
import com.example.data.model.AudioEffectSettings
import com.example.data.model.LibrarySettings
import com.example.data.model.NotificationSettings
import com.example.data.model.PlaybackSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.orionDataStore: DataStore<Preferences> by preferencesDataStore(name = "orion_settings")

@Serializable
data class ResumePlaybackSnapshot(
    val queueSongIds: List<Long> = emptyList(),
    val currentSongId: Long = -1L,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatModeName: String = "OFF"
)

class SettingsDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val APPEARANCE = stringPreferencesKey("appearance_json")
        val PLAYBACK = stringPreferencesKey("playback_json")
        val LIBRARY = stringPreferencesKey("library_json")
        val AUDIO_EFFECTS = stringPreferencesKey("audio_effects_json")
        val NOTIFICATIONS = stringPreferencesKey("notifications_json")
        val RESUME_SNAPSHOT = stringPreferencesKey("resume_snapshot_json")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
        val LAST_SCAN_MS = longPreferencesKey("last_scan_ms")
    }

    val appearanceFlow: Flow<AppearanceSettings> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.APPEARANCE], AppearanceSettings())
    }

    val playbackFlow: Flow<PlaybackSettings> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.PLAYBACK], PlaybackSettings())
    }

    val libraryFlow: Flow<LibrarySettings> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.LIBRARY], LibrarySettings())
    }

    val audioEffectsFlow: Flow<AudioEffectSettings> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.AUDIO_EFFECTS], AudioEffectSettings())
    }

    val notificationsFlow: Flow<NotificationSettings> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.NOTIFICATIONS], NotificationSettings())
    }

    val resumeSnapshotFlow: Flow<ResumePlaybackSnapshot> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.RESUME_SNAPSHOT], ResumePlaybackSnapshot())
    }

    val recentSearchesFlow: Flow<List<String>> = context.orionDataStore.data.map { prefs ->
        decodeOrDefault(prefs[Keys.RECENT_SEARCHES], emptyList())
    }

    val lastScanTimeFlow: Flow<Long> = context.orionDataStore.data.map { prefs ->
        prefs[Keys.LAST_SCAN_MS] ?: 0L
    }

    suspend fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[Keys.APPEARANCE], AppearanceSettings())
            prefs[Keys.APPEARANCE] = json.encodeToString(transform(current))
        }
    }

    suspend fun updatePlayback(transform: (PlaybackSettings) -> PlaybackSettings) {
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[Keys.PLAYBACK], PlaybackSettings())
            prefs[Keys.PLAYBACK] = json.encodeToString(transform(current))
        }
    }

    suspend fun updateLibrary(transform: (LibrarySettings) -> LibrarySettings) {
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[Keys.LIBRARY], LibrarySettings())
            prefs[Keys.LIBRARY] = json.encodeToString(transform(current))
        }
    }

    suspend fun updateAudioEffects(transform: (AudioEffectSettings) -> AudioEffectSettings) {
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[Keys.AUDIO_EFFECTS], AudioEffectSettings())
            prefs[Keys.AUDIO_EFFECTS] = json.encodeToString(transform(current))
        }
    }

    suspend fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings) {
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[Keys.NOTIFICATIONS], NotificationSettings())
            prefs[Keys.NOTIFICATIONS] = json.encodeToString(transform(current))
        }
    }

    suspend fun saveResumeSnapshot(snapshot: ResumePlaybackSnapshot) {
        context.orionDataStore.edit { prefs ->
            prefs[Keys.RESUME_SNAPSHOT] = json.encodeToString(snapshot)
        }
    }

    suspend fun getResumeSnapshotOnce(): ResumePlaybackSnapshot {
        val prefs = context.orionDataStore.data.first()
        return decodeOrDefault(prefs[Keys.RESUME_SNAPSHOT], ResumePlaybackSnapshot())
    }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.orionDataStore.edit { prefs ->
            val current = decodeOrDefault<List<String>>(prefs[Keys.RECENT_SEARCHES], emptyList())
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(8)
            prefs[Keys.RECENT_SEARCHES] = json.encodeToString(updated)
        }
    }

    suspend fun clearSearchHistory() {
        context.orionDataStore.edit { prefs ->
            prefs.remove(Keys.RECENT_SEARCHES)
        }
    }

    suspend fun setLastScanTime(timestamp: Long) {
        context.orionDataStore.edit { prefs ->
            prefs[Keys.LAST_SCAN_MS] = timestamp
        }
    }

    suspend fun resetAllSettings() {
        context.orionDataStore.edit { prefs ->
            prefs.remove(Keys.APPEARANCE)
            prefs.remove(Keys.PLAYBACK)
            prefs.remove(Keys.LIBRARY)
            prefs.remove(Keys.AUDIO_EFFECTS)
            prefs.remove(Keys.NOTIFICATIONS)
        }
    }

    private inline fun <reified T> decodeOrDefault(raw: String?, defaultValue: T): T {
        if (raw.isNullOrBlank()) return defaultValue
        return runCatching { json.decodeFromString<T>(raw) }.getOrDefault(defaultValue)
    }
}
