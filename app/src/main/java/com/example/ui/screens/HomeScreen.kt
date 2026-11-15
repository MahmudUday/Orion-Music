package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.Playlist
import com.example.data.model.SleepTimerState
import com.example.data.model.Song
import com.example.ui.components.EmptyLibraryView
import com.example.ui.components.OrionArtwork
import com.example.ui.components.OrionEqualizerVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.SongRowItem
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.PlaybackUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    libraryState: LibraryUiState,
    playbackState: PlaybackUiState,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favorites: List<Song>,
    playlists: List<Playlist>,
    sleepTimerState: SleepTimerState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRescanLibrary: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSongMore: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onNavigateToLibraryTab: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    contentBottomPadding: PaddingValues
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Late Night Session"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        // Branded Header
        item(key = "home_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ORION MUSIC",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sleepTimerState.isActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenSleepTimer)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.NightlightRound,
                                    contentDescription = "Sleep timer active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sleepTimerState.stopAfterCurrentSong) "1 track" else FormatUtils.formatDuration(sleepTimerState.remainingMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_eq_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Audio Equalizer",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onRescanLibrary,
                        enabled = !libraryState.isScanning,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_rescan_btn")
                    ) {
                        if (libraryState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Rescan library",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Quick-access Currently Playing Hero Card or Shuffle All banner
        item(key = "home_hero") {
            val activeSong = playbackState.currentSong
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        if (activeSong != null) onExpandNowPlaying()
                        else if (libraryState.sortedSongs.isNotEmpty()) onPlaySongs(libraryState.sortedSongs, 0, true)
                    }
                    .padding(16.dp)
                    .testTag("home_hero_card")
            ) {
                if (activeSong != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrionArtwork(
                            artworkUri = activeSong.artworkUri,
                            contentDescription = activeSong.title,
                            modifier = Modifier.size(68.dp),
                            glowEnabled = playbackState.isPlaying
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OrionEqualizerVisualizer(
                                    isPlaying = playbackState.isPlaying,
                                    barCount = 4,
                                    modifier = Modifier.size(16.dp, 12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (playbackState.isPlaying) "NOW PLAYING" else "PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeSong.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(onClick = onTogglePlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${libraryState.stats.totalSongs} Offline Tracks Ready",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "100% local hi-fi playback • Zero cloud dependency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Empty Library State
        if (libraryState.allSongs.isEmpty()) {
            item(key = "home_empty") {
                EmptyLibraryView(
                    hasPermission = hasAudioPermission,
                    isScanning = libraryState.isScanning,
                    onRequestPermission = onRequestPermission,
                    onAddSafFolder = onAddSafFolder,
                    onRescan = onRescanLibrary
                )
            }
        } else {
            // Recently Played
            if (recentlyPlayed.isNotEmpty()) {
                item(key = "sec_recent_played") {
                    SectionHeader(title = "Recently Played", subtitle = "Jump back in")
                    HorizontalSongCarousel(
                        songs = recentlyPlayed,
                        currentSongId = playbackState.currentSong?.id,
                        onPlaySong = { idx -> onPlaySongs(recentlyPlayed, idx, false) }
                    )
                }
            }

            // Recently Added
            if (recentlyAdded.isNotEmpty()) {
                item(key = "sec_recent_added") {
                    SectionHeader(
                        title = "Recently Added",
                        actionLabel = "Play All",
                        onActionClick = { onPlaySongs(recentlyAdded, 0, false) }
                    )
                    HorizontalSongCarousel(
                        songs = recentlyAdded,
                        currentSongId = playbackState.currentSong?.id,
                        onPlaySong = { idx -> onPlaySongs(recentlyAdded, idx, false) }
                    )
                }
            }

            // Most Played
            if (mostPlayed.isNotEmpty()) {
                item(key = "sec_most_played") {
                    SectionHeader(title = "Most Played", subtitle = "Your heavy rotation")
                    HorizontalSongCarousel(
                        songs = mostPlayed,
                        currentSongId = playbackState.currentSong?.id,
                        onPlaySong = { idx -> onPlaySongs(mostPlayed, idx, false) }
                    )
                }
            }

            // Favorite Tracks
            if (favorites.isNotEmpty()) {
                item(key = "sec_favorites") {
                    SectionHeader(
                        title = "Favorite Tracks",
                        subtitle = "${favorites.size} loved songs",
                        actionLabel = "Shuffle",
                        onActionClick = { onPlaySongs(favorites, 0, true) }
                    )
                    HorizontalSongCarousel(
                        songs = favorites,
                        currentSongId = playbackState.currentSong?.id,
                        onPlaySong = { idx -> onPlaySongs(favorites, idx, false) }
                    )
                }
            }

            // Your Playlists
            if (playlists.isNotEmpty()) {
                item(key = "sec_playlists") {
                    SectionHeader(title = "Your Playlists")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Column(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { onOpenPlaylist(pl) }
                                    .padding(10.dp)
                            ) {
                                OrionArtwork(
                                    artworkUri = pl.artworkUri,
                                    contentDescription = pl.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(112.dp),
                                    cornerRadiusOverride = 12.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pl.songCount} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Albums
            if (libraryState.albums.isNotEmpty()) {
                item(key = "sec_albums") {
                    SectionHeader(title = "Albums", actionLabel = "Browse", onActionClick = onNavigateToLibraryTab)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(libraryState.albums.take(16), key = { "${it.id}_${it.name}" }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onOpenAlbum(album) }
                            ) {
                                OrionArtwork(
                                    artworkUri = album.artworkUri,
                                    contentDescription = album.name,
                                    modifier = Modifier.size(140.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Artists
            if (libraryState.artists.isNotEmpty()) {
                item(key = "sec_artists") {
                    SectionHeader(title = "Artists")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(libraryState.artists.take(16), key = { it.name }) { artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenArtist(artist) }
                                    .padding(4.dp)
                            ) {
                                if (artist.artworkUri != null) {
                                    OrionArtwork(
                                        artworkUri = artist.artworkUri,
                                        contentDescription = artist.name,
                                        modifier = Modifier.size(80.dp),
                                        cornerRadiusOverride = 40.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = artist.name, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Genres
            if (libraryState.genres.isNotEmpty()) {
                item(key = "sec_genres") {
                    SectionHeader(title = "Genres")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(libraryState.genres.take(14), key = { it.name }) { genre ->
                            FilterChip(
                                selected = false,
                                onClick = { onOpenGenre(genre) },
                                label = { Text("${genre.name} (${genre.songCount})") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // All Songs quick list
            item(key = "sec_all_songs_header") {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "All Songs",
                    subtitle = "${libraryState.sortedSongs.size} tracks on device",
                    actionLabel = "View All",
                    onActionClick = onNavigateToLibraryTab
                )
            }

            itemsIndexed(
                items = libraryState.sortedSongs.take(15),
                key = { _, s -> "home_song_${s.id}" }
            ) { index, song ->
                SongRowItem(
                    song = song,
                    isCurrent = playbackState.currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlaySongs(libraryState.sortedSongs, index, false) },
                    onFavoriteClick = { onToggleFavorite(song.id) },
                    onMoreClick = { onOpenSongMore(song) },
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HorizontalSongCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    onPlaySong: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(songs.take(15), key = { idx, s -> "${s.id}_$idx" }) { index, song ->
            val isCurrent = song.id == currentSongId
            Column(
                modifier = Modifier
                    .width(134.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onPlaySong(index) }
            ) {
                OrionArtwork(
                    artworkUri = song.artworkUri,
                    contentDescription = song.title,
                    modifier = Modifier.size(134.dp),
                    glowEnabled = isCurrent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
