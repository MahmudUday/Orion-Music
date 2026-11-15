package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Playlist
import com.example.data.model.SleepTimerState
import com.example.data.model.Song
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylists: () -> Unit,
    onViewAlbum: () -> Unit,
    onViewArtist: () -> Unit,
    onShowDetails: () -> Unit,
    onExcludeFolder: () -> Unit,
    onHideSong: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrionArtwork(
                    artworkUri = song.artworkUri,
                    contentDescription = song.title,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))

            SheetActionRow(Icons.Rounded.PlayArrow, "Play Next") { onPlayNext(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to Queue") { onAddToQueue(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Playlist(s)") { onDismiss(); onAddToPlaylists() }
            SheetActionRow(Icons.Rounded.Album, "Go to Album") { onDismiss(); onViewAlbum() }
            SheetActionRow(Icons.Rounded.Person, "Go to Artist") { onDismiss(); onViewArtist() }
            SheetActionRow(Icons.Rounded.Info, "File Info & Location") { onDismiss(); onShowDetails() }
            SheetActionRow(Icons.Rounded.Block, "Exclude Folder (${song.folderName})") { onExcludeFolder(); onDismiss() }
            SheetActionRow(Icons.Rounded.VisibilityOff, "Hide Audio File") { onHideSong(); onDismiss() }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SongDetailsDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Track & File Details", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailField("Title", song.title)
                DetailField("Artist", song.artist)
                DetailField("Album", song.album)
                DetailField("Genre", song.genre)
                DetailField("Duration", FormatUtils.formatDuration(song.durationMs))
                DetailField("File Size", FormatUtils.formatFileSize(song.sizeBytes))
                DetailField("Format / MIME", song.mimeType)
                if (song.bitrateKbps > 0) DetailField("Bitrate", "${song.bitrateKbps} kbps")
                if (song.sampleRateHz > 0) DetailField("Sample Rate", "${song.sampleRateHz} Hz")
                DetailField("Folder", song.folderName)
                DetailField("Path / URI", song.filePath.ifBlank { song.contentUri })
                DetailField("Plays", "${song.playCount}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddToPlaylistsDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    onConfirmSelection: (List<Long>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Add to Playlists") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))

                playlists.filterNot { it.isSystemFavorites }.forEach { pl ->
                    val checked = pl.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (checked) selectedIds.remove(pl.id) else selectedIds.add(pl.id)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (it) selectedIds.add(pl.id) else selectedIds.remove(pl.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${pl.name} (${pl.songCount})", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateNewPlaylist(newPlaylistName)
                                newPlaylistName = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSelection(selectedIds.toList())
                    onDismiss()
                },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Add (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LyricsEditorDialog(
    initialRawText: String,
    onDismiss: () -> Unit,
    onPickLrcFile: () -> Unit,
    onSaveLyrics: (String) -> Unit
) {
    var text by remember(initialRawText) { mutableStateOf(initialRawText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Offline Lyrics Editor") },
        text = {
            Column {
                Text(
                    text = "Paste plain text or synchronized [mm:ss.xx] LRC lyrics, or pick a local .lrc/.txt file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onPickLrcFile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Local .lrc / .txt File")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Lyrics content") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lyrics_editor_input")
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveLyrics(text)
                onDismiss()
            }) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Lyrics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onDismiss: () -> Unit,
    onStartMinutes: (Int, Boolean) -> Unit,
    onStartClockTime: (Int, Int, Boolean) -> Unit,
    onStartEndOfTrack: (Boolean) -> Unit,
    onCancelTimer: () -> Unit
) {
    var fadeOut by remember(state.fadeOutEnabled) { mutableStateOf(state.fadeOutEnabled) }
    var customMinutesText by remember { mutableStateOf("25") }
    var showClockPicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NightlightRound,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (state.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (state.stopAfterCurrentSong) "Stopping after current track" else "Active: ${state.modeLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!state.stopAfterCurrentSong) {
                            Text(
                                text = "Remaining: ${FormatUtils.formatDuration(state.remainingMillis)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    TextButton(onClick = onCancelTimer) { Text("Cancel") }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gradual Volume Fade-Out", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Smoothly lowers volume over the final 15 seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = fadeOut, onCheckedChange = { fadeOut = it })
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Quick Presets", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 30, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = false,
                        onClick = { onStartMinutes(mins, fadeOut); onDismiss() },
                        label = { Text("$mins min") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Custom minutes") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull() ?: 0
                        if (mins > 0) {
                            onStartMinutes(mins, fadeOut)
                            onDismiss()
                        }
                    }
                ) {
                    Text("Start")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onStartEndOfTrack(fadeOut); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("After Current Song", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showClockPicker = !showClockPicker },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Time")
                }
            }

            if (showClockPicker) {
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
                Button(
                    onClick = {
                        onStartClockTime(timePickerState.hour, timePickerState.minute, fadeOut)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Playback at ${String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)}")
                }
            }
        }
    }
}
