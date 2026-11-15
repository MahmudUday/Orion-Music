package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.SafFolderEntity
import com.example.data.model.AnimationIntensity
import com.example.data.model.AppearanceSettings
import com.example.data.model.ArtworkStyle
import com.example.data.model.AudioFocusBehavior
import com.example.data.model.LayoutDensity
import com.example.data.model.LibrarySettings
import com.example.data.model.NotificationSettings
import com.example.data.model.PlaybackSettings
import com.example.data.model.RepeatModeOption
import com.example.data.model.SleepTimerState
import com.example.data.model.Song
import com.example.ui.theme.AccentColorPreset
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    appearance: AppearanceSettings,
    playback: PlaybackSettings,
    library: LibrarySettings,
    notifications: NotificationSettings,
    libraryState: LibraryUiState,
    sleepTimerState: SleepTimerState,
    excludedFolders: List<ExcludedFolderEntity>,
    safFolders: List<SafFolderEntity>,
    hiddenSongs: List<Song>,
    onUpdateAppearance: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    onUpdatePlayback: ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    onUpdateLibrary: ((LibrarySettings) -> LibrarySettings) -> Unit,
    onUpdateNotifications: ((NotificationSettings) -> NotificationSettings) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onRescanLibrary: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onRemoveExcludedFolder: (String) -> Unit,
    onUnhideSong: (Long) -> Unit,
    onUnhideAll: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetSettings: () -> Unit,
    contentBottomPadding: PaddingValues
) {
    var showExcludedDialog by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = contentBottomPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displayMedium)
            Text(
                "Offline configuration • All preferences saved locally",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Audio Studio & Sleep Timer shortcuts
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_eq_btn")
                ) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Audio Studio")
                }
                OutlinedButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_timer_btn")
                ) {
                    Icon(Icons.Rounded.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (sleepTimerState.isActive) "Timer Active" else "Sleep Timer")
                }
            }
        }

        // APPEARANCE
        item {
            SettingsCard(title = "Appearance & Theme") {
                SettingSwitchRow(
                    title = "AMOLED Pure Black (#000000)",
                    subtitle = "Maximum contrast and battery efficiency on OLED displays",
                    checked = appearance.amoledBlack,
                    onCheckedChange = { checked -> onUpdateAppearance { it.copy(amoledBlack = checked) } }
                )
                SettingSwitchRow(
                    title = "Dynamic Album Artwork Colors",
                    subtitle = "Adapts neon accents and ambient lighting to current song",
                    checked = appearance.dynamicArtworkColors,
                    onCheckedChange = { checked -> onUpdateAppearance { it.copy(dynamicArtworkColors = checked) } }
                )

                Text("Accent Palette", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentColorPreset.entries.forEach { preset ->
                        val selected = appearance.accentPreset == preset
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preset.primary)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable { onUpdateAppearance { it.copy(accentPresetName = preset.name) } }
                        )
                    }
                }

                Text("Animation Intensity", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationIntensity.entries.forEach { mode ->
                        FilterChip(
                            selected = appearance.animationIntensity == mode,
                            onClick = { onUpdateAppearance { it.copy(animationIntensityName = mode.name) } },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text("Album Artwork Style", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtworkStyle.entries.forEach { style ->
                        FilterChip(
                            selected = appearance.artworkStyle == style,
                            onClick = { onUpdateAppearance { it.copy(artworkStyleName = style.name) } },
                            label = { Text(style.label) }
                        )
                    }
                }

                Text("List Layout Density", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayoutDensity.entries.forEach { density ->
                        FilterChip(
                            selected = appearance.layoutDensity == density,
                            onClick = { onUpdateAppearance { it.copy(layoutDensityName = density.name) } },
                            label = { Text(density.label) }
                        )
                    }
                }
            }
        }

        // PLAYBACK
        item {
            SettingsCard(title = "Playback Engine") {
                SettingSwitchRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between consecutive album tracks",
                    checked = playback.gaplessPlayback,
                    onCheckedChange = { checked -> onUpdatePlayback { it.copy(gaplessPlayback = checked) } }
                )

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Crossfade Duration", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (playback.crossfadeSeconds == 0) "Off" else "${playback.crossfadeSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = playback.crossfadeSeconds.toFloat(),
                        onValueChange = { v -> onUpdatePlayback { it.copy(crossfadeSeconds = v.toInt()) } },
                        valueRange = 0f..12f,
                        steps = 11
                    )
                }

                SettingSwitchRow(
                    title = "Resume Last Played Song",
                    subtitle = "Restore queue automatically on app startup",
                    checked = playback.resumeLastSong,
                    onCheckedChange = { checked -> onUpdatePlayback { it.copy(resumeLastSong = checked) } }
                )
                SettingSwitchRow(
                    title = "Remember Playback Position",
                    subtitle = "Save exact timestamp of active track",
                    checked = playback.rememberPosition,
                    onCheckedChange = { checked -> onUpdatePlayback { it.copy(rememberPosition = checked) } }
                )
                SettingSwitchRow(
                    title = "Default Shuffle Mode",
                    subtitle = "Prefer shuffled order for new sessions",
                    checked = playback.defaultShuffle,
                    onCheckedChange = { checked -> onUpdatePlayback { it.copy(defaultShuffle = checked) } }
                )

                Text("Default Repeat Behavior", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatModeOption.entries.forEach { rep ->
                        FilterChip(
                            selected = playback.defaultRepeat == rep,
                            onClick = { onUpdatePlayback { it.copy(defaultRepeatName = rep.name) } },
                            label = { Text(rep.label) }
                        )
                    }
                }

                Text("Audio Focus Behavior", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioFocusBehavior.entries.forEach { focus ->
                        FilterChip(
                            selected = playback.audioFocusBehavior == focus,
                            onClick = { onUpdatePlayback { it.copy(audioFocusBehaviorName = focus.name) } },
                            label = { Text(focus.label) }
                        )
                    }
                }
            }
        }

        // LIBRARY
        item {
            SettingsCard(title = "Library & Storage Scanner") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRescanLibrary, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan Now")
                    }
                    OutlinedButton(onClick = onAddSafFolder, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Folder")
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Minimum Track Duration Filter", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${library.minDurationSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = library.minDurationSeconds.toFloat(),
                        onValueChange = { v -> onUpdateLibrary { it.copy(minDurationSeconds = v.toInt()) } },
                        valueRange = 0f..60f,
                        steps = 11
                    )
                }

                SettingSwitchRow(
                    title = "Duplicate Song Detection",
                    subtitle = "Skip identical title/artist/duration/size duplicates",
                    checked = library.duplicateDetection,
                    onCheckedChange = { checked -> onUpdateLibrary { it.copy(duplicateDetection = checked) } }
                )

                if (safFolders.isNotEmpty()) {
                    Text("Custom SAF Folders (${safFolders.size})", style = MaterialTheme.typography.titleSmall)
                    safFolders.forEach { saf ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(saf.displayName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveSafFolder(saf.treeUri) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove SAF folder", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showExcludedDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Excluded (${excludedFolders.size})")
                    }
                    OutlinedButton(onClick = { showHiddenDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("Hidden (${hiddenSongs.size})")
                    }
                }
            }
        }

        // NOTIFICATIONS
        item {
            SettingsCard(title = "Notifications & Lock Screen") {
                SettingSwitchRow(
                    title = "Lock Screen Media Controls",
                    subtitle = "Expose MediaSession transport buttons when device is locked",
                    checked = notifications.showLockScreenControls,
                    onCheckedChange = { c -> onUpdateNotifications { it.copy(showLockScreenControls = c) } }
                )
                SettingSwitchRow(
                    title = "Notification Album Artwork",
                    subtitle = "Display embedded album cover inside system media shade",
                    checked = notifications.showArtworkInNotification,
                    onCheckedChange = { c -> onUpdateNotifications { it.copy(showArtworkInNotification = c) } }
                )
                SettingSwitchRow(
                    title = "Seek Scrubbing in Notification",
                    subtitle = "Allow seeking directly from system playback progress bar",
                    checked = notifications.showSeekActionsInNotification,
                    onCheckedChange = { c -> onUpdateNotifications { it.copy(showSeekActionsInNotification = c) } }
                )
            }
        }

        // STORAGE, BACKUP & ADVANCED
        item {
            SettingsCard(title = "Storage, Backup & Diagnostics") {
                StatRow("Total Songs", "${libraryState.stats.totalSongs}")
                StatRow("Total Albums", "${libraryState.stats.totalAlbums}")
                StatRow("Total Artists", "${libraryState.stats.totalArtists}")
                StatRow("Total Playlists", "${libraryState.stats.totalPlaylists}")
                StatRow("Total Music Duration", FormatUtils.formatTotalHoursMinutes(libraryState.stats.totalDurationMs))
                StatRow("Library Audio Size", FormatUtils.formatFileSize(libraryState.stats.totalSizeBytes))
                StatRow("Cached Temp / Artwork", FormatUtils.formatFileSize(libraryState.stats.artworkCacheBytes))

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup")
                    }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Backup")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClearCache, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Cache")
                    }
                    OutlinedButton(onClick = { showResetConfirm = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Settings")
                    }
                }
            }
        }

        item {
            val runtime = Runtime.getRuntime()
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            SettingsCard(title = "About & Performance") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Engine Diagnostics", style = MaterialTheme.typography.titleSmall)
                }
                StatRow("App Version", "ORION MUSIC v${BuildConfig.VERSION_NAME}")
                StatRow("Audio Pipeline", "Media3 ExoPlayer + PCM Stereo DSP")
                StatRow("Offline Mode", "100% Local (No Internet Permission)")
                StatRow("Heap Footprint", "$usedMb MB active")
            }
        }
    }

    if (showExcludedDialog) {
        AlertDialog(
            onDismissRequest = { showExcludedDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Excluded Folders") },
            text = {
                if (excludedFolders.isEmpty()) {
                    Text("No folders are currently excluded.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        excludedFolders.forEach { folder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.folderName, style = MaterialTheme.typography.titleSmall)
                                    Text(folder.folderPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { onRemoveExcludedFolder(folder.folderPath) }) {
                                    Text("Include")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExcludedDialog = false }) { Text("Close") }
            }
        )
    }

    if (showHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Hidden Audio Files") },
            text = {
                if (hiddenSongs.isEmpty()) {
                    Text("No audio files are hidden.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        hiddenSongs.forEach { song ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onUnhideSong(song.id) }) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = "Unhide ${song.title}")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (hiddenSongs.isNotEmpty()) {
                    TextButton(onClick = { onUnhideAll(); showHiddenDialog = false }) {
                        Text("Unhide All")
                    }
                }
                TextButton(onClick = { showHiddenDialog = false }) { Text("Close") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Reset All Settings?") },
            text = { Text("Restores appearance, playback, equalizer, and notification preferences to defaults. Your music and playlists stay intact.") },
            confirmButton = {
                Button(onClick = { onResetSettings(); showResetConfirm = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
