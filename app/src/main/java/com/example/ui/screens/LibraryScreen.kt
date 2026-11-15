package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.LibrarySettings
import com.example.data.model.MusicFolder
import com.example.data.model.Song
import com.example.data.model.SongSortOrder
import com.example.ui.components.EmptyLibraryView
import com.example.ui.components.OrionArtwork
import com.example.ui.components.SongRowItem
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.PlaybackUiState

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders")
}

sealed class LibraryDrillDown {
    data class AlbumDetail(val album: Album) : LibraryDrillDown()
    data class ArtistDetail(val artist: Artist) : LibraryDrillDown()
    data class GenreDetail(val genre: Genre) : LibraryDrillDown()
    data class FolderDetail(val folder: MusicFolder) : LibraryDrillDown()
}

@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    librarySettings: LibrarySettings,
    playbackState: PlaybackUiState,
    drillDown: LibraryDrillDown?,
    hasAudioPermission: Boolean,
    onSetDrillDown: (LibraryDrillDown?) -> Unit,
    onRequestPermission: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRescanLibrary: () -> Unit,
    onUpdateSort: (SongSortOrder, Boolean) -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMore: (Song) -> Unit,
    onExcludeFolder: (MusicFolder) -> Unit,
    contentBottomPadding: PaddingValues
) {
    if (drillDown != null) {
        BackHandler { onSetDrillDown(null) }
        LibraryDrillDownView(
            drillDown = drillDown,
            allSongs = libraryState.sortedSongs,
            playbackState = playbackState,
            onBack = { onSetDrillDown(null) },
            onPlaySongs = onPlaySongs,
            onToggleFavorite = onToggleFavorite,
            onOpenSongMore = onOpenSongMore,
            onExcludeFolder = onExcludeFolder,
            contentBottomPadding = contentBottomPadding
        )
        return
    }

    var activeTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Header + Sort/Scan actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Library", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = "${libraryState.stats.totalSongs} tracks • ${FormatUtils.formatTotalHoursMinutes(libraryState.stats.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAddSafFolder,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Add custom SAF folder")
                }

                IconButton(
                    onClick = onRescanLibrary,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Rescan library")
                }

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("library_sort_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort songs")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SongSortOrder.entries.forEach { order ->
                            val selected = librarySettings.sortOrder == order
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = if (librarySettings.sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                onClick = {
                                    val newAsc = if (selected) !librarySettings.sortAscending else true
                                    onUpdateSort(order, newAsc)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Category Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (libraryState.allSongs.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 16.dp)
            ) {
                item {
                    EmptyLibraryView(
                        hasPermission = hasAudioPermission,
                        isScanning = libraryState.isScanning,
                        onRequestPermission = onRequestPermission,
                        onAddSafFolder = onAddSafFolder,
                        onRescan = onRescanLibrary
                    )
                }
            }
        } else {
            val bottomPad = contentBottomPadding.calculateBottomPadding() + 16.dp
            when (activeTab) {
                LibraryTab.SONGS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPad)
                    ) {
                        item(key = "songs_actions") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onPlaySongs(libraryState.sortedSongs, 0, false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play All")
                                }
                                OutlinedButton(
                                    onClick = { onPlaySongs(libraryState.sortedSongs, 0, true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                        itemsIndexed(
                            items = libraryState.sortedSongs,
                            key = { _, s -> s.id }
                        ) { idx, song ->
                            SongRowItem(
                                song = song,
                                isCurrent = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                onClick = { onPlaySongs(libraryState.sortedSongs, idx, false) },
                                onFavoriteClick = { onToggleFavorite(song.id) },
                                onMoreClick = { onOpenSongMore(song) },
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPad),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(libraryState.albums, key = { "${it.id}_${it.name}" }) { album ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSetDrillDown(LibraryDrillDown.AlbumDetail(album)) }
                            ) {
                                OrionArtwork(
                                    artworkUri = album.artworkUri,
                                    contentDescription = album.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.artist} • ${album.songCount} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(libraryState.artists, key = { it.name }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { onSetDrillDown(LibraryDrillDown.ArtistDetail(artist)) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (artist.artworkUri != null) {
                                    OrionArtwork(
                                        artworkUri = artist.artworkUri,
                                        contentDescription = artist.name,
                                        modifier = Modifier.size(54.dp),
                                        cornerRadiusOverride = 27.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = artist.name, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${artist.albumCount} albums • ${artist.songCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.GENRES -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(libraryState.genres, key = { it.name }) { genre ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { onSetDrillDown(LibraryDrillDown.GenreDetail(genre)) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = genre.name, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(genre.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${genre.songCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.FOLDERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(libraryState.folders, key = { it.path }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { onSetDrillDown(LibraryDrillDown.FolderDetail(folder)) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (folder.artworkUri != null) {
                                    OrionArtwork(
                                        artworkUri = folder.artworkUri,
                                        contentDescription = folder.name,
                                        modifier = Modifier.size(52.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Folder, contentDescription = folder.name, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name + if (folder.isSafFolder) " (SAF)" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${folder.songCount} tracks • ${folder.path}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onExcludeFolder(folder) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.Block,
                                        contentDescription = "Exclude folder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryDrillDownView(
    drillDown: LibraryDrillDown,
    allSongs: List<Song>,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMore: (Song) -> Unit,
    onExcludeFolder: (MusicFolder) -> Unit,
    contentBottomPadding: PaddingValues
) {
    val (title, subtitle, artUri, songs) = remember(drillDown, allSongs) {
        when (drillDown) {
            is LibraryDrillDown.AlbumDetail -> {
                val filtered = allSongs.filter { it.albumId == drillDown.album.id && it.album == drillDown.album.name }
                    .sortedBy { it.trackNumber }
                listOf(drillDown.album.name, drillDown.album.artist, drillDown.album.artworkUri, filtered)
            }
            is LibraryDrillDown.ArtistDetail -> {
                val filtered = allSongs.filter { it.artist == drillDown.artist.name }
                listOf(drillDown.artist.name, "${filtered.size} tracks • ${drillDown.artist.albumCount} albums", drillDown.artist.artworkUri, filtered)
            }
            is LibraryDrillDown.GenreDetail -> {
                val filtered = allSongs.filter { it.genre == drillDown.genre.name }
                listOf(drillDown.genre.name, "${filtered.size} tracks", drillDown.genre.artworkUri, filtered)
            }
            is LibraryDrillDown.FolderDetail -> {
                val filtered = allSongs.filter { it.folderPath == drillDown.folder.path }
                listOf(drillDown.folder.name, drillDown.folder.path, drillDown.folder.artworkUri, filtered)
            }
        }
    }
    @Suppress("UNCHECKED_CAST")
    val trackList = songs as List<Song>

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = title as String,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (drillDown is LibraryDrillDown.FolderDetail) {
                    IconButton(onClick = { onExcludeFolder(drillDown.folder); onBack() }) {
                        Icon(Icons.Rounded.Block, contentDescription = "Exclude folder")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrionArtwork(
                    artworkUri = artUri as String?,
                    contentDescription = title as String,
                    modifier = Modifier.size(96.dp),
                    glowEnabled = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle as String,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onPlaySongs(trackList, 0, false) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play")
                        }
                        OutlinedButton(onClick = { onPlaySongs(trackList, 0, true) }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }
        }

        itemsIndexed(trackList, key = { _, s -> "drill_${s.id}" }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrent = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlaySongs(trackList, idx, false) },
                onFavoriteClick = { onToggleFavorite(song.id) },
                onMoreClick = { onOpenSongMore(song) },
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
