package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.ui.theme.AccentColorPreset
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Song(
    val id: Long,
    val mediaStoreId: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val filePath: String,
    val folderName: String,
    val folderPath: String,
    val mimeType: String,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val artworkUri: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isSafDocument: Boolean = false
)

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val artworkUri: String?,
    val year: Int,
    val totalDurationMs: Long
)

@Immutable
data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: String?
)

@Immutable
data class Genre(
    val name: String,
    val songCount: Int,
    val artworkUri: String?
)

@Immutable
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val artworkUri: String?,
    val isSafFolder: Boolean = false
)

@Immutable
@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isSystemFavorites: Boolean = false,
    val songCount: Int = 0,
    val artworkUri: String? = null
)

@Immutable
data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>
)

@Immutable
data class LyricLine(
    val timestampMs: Long?,
    val text: String
)

@Immutable
data class LyricsData(
    val songId: Long,
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val rawText: String,
    val sourceLabel: String
)

enum class SongSortOrder(val label: String) {
    ALPHABETICAL("Alphabetical"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    MOST_PLAYED("Most Played"),
    DURATION("Duration"),
    FILE_SIZE("File Size")
}

enum class RepeatModeOption(val label: String) {
    OFF("Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

enum class AnimationIntensity(val label: String) {
    FULL("Full"),
    REDUCED("Reduced"),
    MINIMAL("Minimal")
}

enum class ArtworkStyle(val label: String) {
    ROUNDED("Rounded"),
    SQUARE("Sharp")
}

enum class LayoutDensity(val label: String) {
    SPACIOUS("Spacious"),
    COMPACT("Compact")
}

enum class AudioFocusBehavior(val label: String) {
    PAUSE_ON_LOSS("Pause on interruption"),
    DUCK_WHEN_POSSIBLE("Duck for notifications")
}

enum class EqualizerPresetType(val label: String, val bandMillibels: List<Int>) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    ROCK("Rock", listOf(450, 250, -100, 280, 450)),
    POP("Pop", listOf(-150, 250, 420, 220, -100)),
    JAZZ("Jazz", listOf(320, 150, -150, 220, 350)),
    CLASSICAL("Classical", listOf(380, 220, -50, 280, 400)),
    ELECTRONIC("Electronic", listOf(500, 320, 50, 260, 480)),
    HIP_HOP("Hip-Hop", listOf(550, 380, 80, 120, 300)),
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0))
}

@Serializable
data class AppearanceSettings(
    val amoledBlack: Boolean = true,
    val accentPresetName: String = AccentColorPreset.ORION_ELECTRIC.name,
    val dynamicArtworkColors: Boolean = true,
    val animationIntensityName: String = AnimationIntensity.FULL.name,
    val artworkStyleName: String = ArtworkStyle.ROUNDED.name,
    val layoutDensityName: String = LayoutDensity.SPACIOUS.name
) {
    val accentPreset: AccentColorPreset
        get() = AccentColorPreset.entries.find { it.name == accentPresetName } ?: AccentColorPreset.ORION_ELECTRIC
    val animationIntensity: AnimationIntensity
        get() = AnimationIntensity.entries.find { it.name == animationIntensityName } ?: AnimationIntensity.FULL
    val artworkStyle: ArtworkStyle
        get() = ArtworkStyle.entries.find { it.name == artworkStyleName } ?: ArtworkStyle.ROUNDED
    val layoutDensity: LayoutDensity
        get() = LayoutDensity.entries.find { it.name == layoutDensityName } ?: LayoutDensity.SPACIOUS
}

@Serializable
data class PlaybackSettings(
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val resumeLastSong: Boolean = true,
    val rememberPosition: Boolean = true,
    val defaultShuffle: Boolean = false,
    val defaultRepeatName: String = RepeatModeOption.OFF.name,
    val audioFocusBehaviorName: String = AudioFocusBehavior.PAUSE_ON_LOSS.name,
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultPitch: Float = 1.0f
) {
    val defaultRepeat: RepeatModeOption
        get() = RepeatModeOption.entries.find { it.name == defaultRepeatName } ?: RepeatModeOption.OFF
    val audioFocusBehavior: AudioFocusBehavior
        get() = AudioFocusBehavior.entries.find { it.name == audioFocusBehaviorName } ?: AudioFocusBehavior.PAUSE_ON_LOSS
}

@Serializable
data class LibrarySettings(
    val minDurationSeconds: Int = 10,
    val duplicateDetection: Boolean = true,
    val sortOrderName: String = SongSortOrder.ALPHABETICAL.name,
    val sortAscending: Boolean = true
) {
    val sortOrder: SongSortOrder
        get() = SongSortOrder.entries.find { it.name == sortOrderName } ?: SongSortOrder.ALPHABETICAL
}

@Serializable
data class AudioEffectSettings(
    val eqEnabled: Boolean = false,
    val presetName: String = EqualizerPresetType.FLAT.name,
    val customBandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val balance: Float = 0f,
    val monoAudio: Boolean = false,
    val volumeNormalization: Boolean = false,
    val replayGainEnabled: Boolean = true
)

@Serializable
data class NotificationSettings(
    val showLockScreenControls: Boolean = true,
    val showArtworkInNotification: Boolean = true,
    val showSeekActionsInNotification: Boolean = true
)

@Immutable
data class SleepTimerState(
    val isActive: Boolean = false,
    val endTimeMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val stopAfterCurrentSong: Boolean = false,
    val fadeOutEnabled: Boolean = true,
    val modeLabel: String = ""
)

@Immutable
data class LibraryStats(
    val totalSongs: Int = 0,
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0,
    val totalPlaylists: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val hiddenSongsCount: Int = 0,
    val excludedFoldersCount: Int = 0,
    val artworkCacheBytes: Long = 0L
)

@Serializable
data class OrionBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val playback: PlaybackSettings = PlaybackSettings(),
    val library: LibrarySettings = LibrarySettings(),
    val audioEffects: AudioEffectSettings = AudioEffectSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val favoriteSongUris: List<String> = emptyList()
)

@Serializable
data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val songUris: List<String>
)
