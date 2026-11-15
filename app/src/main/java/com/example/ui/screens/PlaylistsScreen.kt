package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.OrionArtwork
import com.example.ui.components.SongRowItem
import com.example.ui.viewmodel.PlaybackUiState
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    selectedPlaylist: Playlist?,
    playbackState: PlaybackUiState,
    observePlaylistSongs: (Playlist) -> Flow<List<Song>>,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onAddSongsToPlaylist: (Long, List<Long>) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onReorderPlaylistSongs: (Long, List<Long>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMore: (Song) -> Unit,
    contentBottomPadding: PaddingValues
) {
    if (selectedPlaylist != null) {
        BackHandler { onSelectPlaylist(null) }
        val activePlaylist = playlists.find { it.id == selectedPlaylist.id } ?: selectedPlaylist
        val songs by remember(activePlaylist.id, activePlaylist.isSystemFavorites) {
            observePlaylistSongs(activePlaylist)
        }.collectAsStateWithLifecycle(initialValue = emptyList())

        PlaylistDetailView(
            playlist = activePlaylist,
            songs = songs,
            allLibrarySongs = allSongs,
            playbackState = playbackState,
            onBack = { onSelectPlaylist(null) },
            onRename = { newName -> onRenamePlaylist(activePlaylist.id, newName) },
            onDelete = {
                onDeletePlaylist(activePlaylist.id)
                onSelectPlaylist(null)
            },
            onPlaySongs = onPlaySongs,
            onAddSongs = { ids -> onAddSongsToPlaylist(activePlaylist.id, ids) },
            onRemoveSong = { songId ->
                if (activePlaylist.isSystemFavorites) onToggleFavorite(songId)
                else onRemoveSongFromPlaylist(activePlaylist.id, songId)
            },
            onReorder = { orderedIds -> onReorderPlaylistSongs(activePlaylist.id, orderedIds) },
            onToggleFavorite = onToggleFavorite,
            onOpenSongMore = onOpenSongMore,
            contentBottomPadding = contentBottomPadding
        )
        return
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = contentBottomPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Playlists", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "${playlists.size} local collections",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_playlist_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New")
                    }
                }
            }

            items(playlists, key = { it.id }) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onSelectPlaylist(pl) }
                        .padding(12.dp)
                        .testTag("playlist_card_${pl.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pl.isSystemFavorites && pl.artworkUri == null) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = "Favorites", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        OrionArtwork(
                            artworkUri = pl.artworkUri,
                            contentDescription = pl.name,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pl.isSystemFavorites) "Auto Favorites • ${pl.songCount} tracks" else "${pl.songCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentBottomPadding.calculateBottomPadding() + 16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Create playlist")
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Create Playlist",
            initialName = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    allLibrarySongs: List<Song>,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onAddSongs: (List<Long>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongMore: (Song) -> Unit,
    contentBottomPadding: PaddingValues
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    // Local mutable order for smooth drag-and-drop reordering
    val editableSongs = remember { mutableStateListOf<Song>() }
    LaunchedEffect(songs) {
        editableSongs.clear()
        editableSongs.addAll(songs)
    }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 74.dp.toPx() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentBottomPadding.calculateBottomPadding() + 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to playlists")
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!playlist.isSystemFavorites) {
                    IconButton(onClick = { showAddSongsDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add songs")
                    }
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPlaySongs(editableSongs.toList(), 0, false) },
                    enabled = editableSongs.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All")
                }
                OutlinedButton(
                    onClick = { onPlaySongs(editableSongs.toList(), 0, true) },
                    enabled = editableSongs.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
            if (!playlist.isSystemFavorites && editableSongs.size > 1) {
                Text(
                    text = "Tip: Long-press and drag tracks to reorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        itemsIndexed(editableSongs, key = { _, s -> "pl_${playlist.id}_${s.id}" }) { index, song ->
            val isDragging = draggingIndex == index
            val offsetY = if (isDragging) dragOffsetY.roundToInt() else 0

            SongRowItem(
                song = song,
                isCurrent = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlaySongs(editableSongs.toList(), index, false) },
                onFavoriteClick = { onToggleFavorite(song.id) },
                onMoreClick = { onOpenSongMore(song) },
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { IntOffset(0, offsetY) }
                    .then(
                        if (!playlist.isSystemFavorites) {
                            Modifier.pointerInput(editableSongs.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = editableSongs.indexOfFirst { it.id == song.id }
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggingIndex < 0) return@detectDragGesturesAfterLongPress
                                        dragOffsetY += dragAmount.y
                                        val targetDelta = (dragOffsetY / itemHeightPx).toInt()
                                        if (targetDelta != 0) {
                                            val newIdx = (draggingIndex + targetDelta).coerceIn(0, editableSongs.lastIndex)
                                            if (newIdx != draggingIndex) {
                                                val moved = editableSongs.removeAt(draggingIndex)
                                                editableSongs.add(newIdx, moved)
                                                dragOffsetY -= (newIdx - draggingIndex) * itemHeightPx
                                                draggingIndex = newIdx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                        onReorder(editableSongs.map { it.id })
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                        } else Modifier
                    ),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!playlist.isSystemFavorites) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { onRemoveSong(song.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Remove from playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = "Rename Playlist",
            initialName = playlist.name,
            confirmLabel = "Save",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    if (showAddSongsDialog) {
        val existingIds = remember(editableSongs.size) { editableSongs.mapTo(HashSet()) { it.id } }
        val candidates = remember(allLibrarySongs, existingIds) { allLibrarySongs.filter { it.id !in existingIds } }
        val selected = remember { mutableStateListOf<Long>() }

        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Add Songs to ${playlist.name}") },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(candidates, key = { it.id }) { s ->
                        val checked = s.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (checked) selected.remove(s.id) else selected.add(s.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { if (it) selected.add(s.id) else selected.remove(s.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text(s.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSongs(selected.toList())
                        showAddSongsDialog = false
                    },
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Add (${selected.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
