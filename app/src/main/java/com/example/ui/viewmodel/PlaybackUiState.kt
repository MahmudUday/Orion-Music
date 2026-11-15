package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.LibraryStats
import com.example.data.model.LyricsData
import com.example.data.model.MusicFolder
import com.example.data.model.RepeatModeOption
import com.example.data.model.Song

@Immutable
data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val deviceVolume: Float = 1.0f,
    val dynamicPrimaryColor: Color? = null,
    val dynamicSecondaryColor: Color? = null,
    val lyrics: LyricsData? = null
)

@Immutable
data class LibraryUiState(
    val allSongs: List<Song> = emptyList(),
    val sortedSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val stats: LibraryStats = LibraryStats(),
    val isScanning: Boolean = false,
    val lastScanMessage: String? = null
)
