package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.EqualizerHardwareState
import com.example.data.local.CustomEqPresetEntity
import com.example.data.model.AudioEffectSettings
import com.example.data.model.EqualizerPresetType
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    effectSettings: AudioEffectSettings,
    hardwareState: EqualizerHardwareState,
    customPresets: List<CustomEqPresetEntity>,
    playbackSpeed: Float,
    pitch: Float,
    onBack: () -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPresetType) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onApplyCustomPreset: (CustomEqPresetEntity) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onUpdateEffects: ((AudioEffectSettings) -> AudioEffectSettings) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    BackHandler { onBack() }

    var showSaveDialog by remember { mutableStateOf(false) }

    val minMb = hardwareState.minLevelMb.toFloat()
    val maxMb = hardwareState.maxLevelMb.toFloat().coerceAtLeast(minMb + 1f)
    val bandCount = hardwareState.bands.size.coerceAtLeast(5)
    val activePreset = EqualizerPresetType.entries.find { it.name == effectSettings.presetName } ?: EqualizerPresetType.FLAT
    val activeBandLevels = remember(effectSettings, activePreset, bandCount) {
        (0 until bandCount).map { idx ->
            if (activePreset == EqualizerPresetType.CUSTOM) {
                effectSettings.customBandLevelsMb.getOrElse(idx) { 0 }
            } else {
                activePreset.bandMillibels.getOrElse(idx) { 0 }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("equalizer_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Audio Engine & EQ", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = if (hardwareState.isAttached) "Hardware AudioFX Session Active" else "DSP Pre-configured (Plays with Track)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = effectSettings.eqEnabled,
                    onCheckedChange = onToggleEq,
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

        // Real-time Frequency Curve Visualizer
        item {
            val primary = MaterialTheme.colorScheme.primary
            val tertiary = MaterialTheme.colorScheme.tertiary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midY = size.height / 2f
                    // 0 dB center reference line
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f
                    )

                    val pts = activeBandLevels.mapIndexed { idx, mb ->
                        val x = if (activeBandLevels.size <= 1) size.width / 2f
                        else (idx.toFloat() / (activeBandLevels.size - 1)) * size.width
                        val norm = ((mb - minMb) / (maxMb - minMb)).coerceIn(0f, 1f)
                        val y = size.height * (1f - norm)
                        Offset(x, y)
                    }

                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 0 until pts.lastIndex) {
                                val p0 = pts[i]
                                val p1 = pts[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(primary, tertiary)),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    pts.forEach { pt ->
                        drawCircle(color = tertiary, radius = 5.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // Presets + Custom Saved Profiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer Presets", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Profile")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPresetType.entries.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label) }
                    )
                }
                customPresets.forEach { custom ->
                    FilterChip(
                        selected = false,
                        onClick = { onApplyCustomPreset(custom) },
                        label = { Text(custom.name) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Delete preset",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeleteCustomPreset(custom.name) }
                            )
                        }
                    )
                }
            }
        }

        // Band Sliders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Frequency Bands", style = MaterialTheme.typography.titleMedium)
                hardwareState.bands.forEachIndexed { idx, band ->
                    val currentMb = activeBandLevels.getOrElse(idx) { 0 }
                    val db = currentMb / 100f
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = FormatUtils.formatFrequency(band.centerFreqHz),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = String.format("%+.1f dB", db),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = currentMb.toFloat(),
                            onValueChange = { onBandLevelChange(idx, it.toInt()) },
                            valueRange = minMb..maxMb
                        )
                    }
                }
            }
        }

        // Spatial, Bass Boost, Balance, Mono, Normalization
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Spatial & Dynamics", style = MaterialTheme.typography.titleMedium)

                SliderSettingRow(
                    label = "Bass Boost",
                    valueText = "${effectSettings.bassBoostStrength / 10}%",
                    value = effectSettings.bassBoostStrength.toFloat(),
                    range = 0f..1000f,
                    onValueChange = { v -> onUpdateEffects { it.copy(bassBoostStrength = v.toInt()) } }
                )

                SliderSettingRow(
                    label = "Virtualizer / Spatial",
                    valueText = "${effectSettings.virtualizerStrength / 10}%",
                    value = effectSettings.virtualizerStrength.toFloat(),
                    range = 0f..1000f,
                    onValueChange = { v -> onUpdateEffects { it.copy(virtualizerStrength = v.toInt()) } }
                )

                SliderSettingRow(
                    label = "Stereo Balance (L / R)",
                    valueText = when {
                        effectSettings.balance < -0.05f -> "L ${(kotlin.math.abs(effectSettings.balance) * 100).toInt()}%"
                        effectSettings.balance > 0.05f -> "R ${(effectSettings.balance * 100).toInt()}%"
                        else -> "Center"
                    },
                    value = effectSettings.balance,
                    range = -1f..1f,
                    onValueChange = { v ->
                        val snapped = if (kotlin.math.abs(v) < 0.06f) 0f else v
                        onUpdateEffects { it.copy(balance = snapped) }
                    }
                )

                EqSwitchRow(
                    title = "Mono Audio Downmix",
                    subtitle = "Combine left & right channels into single mono feed",
                    checked = effectSettings.monoAudio,
                    onCheckedChange = { checked -> onUpdateEffects { it.copy(monoAudio = checked) } }
                )

                EqSwitchRow(
                    title = "Volume Normalization",
                    subtitle = "Applies smart loudness leveling across tracks",
                    checked = effectSettings.volumeNormalization,
                    onCheckedChange = { checked -> onUpdateEffects { it.copy(volumeNormalization = checked) } }
                )

                EqSwitchRow(
                    title = "ReplayGain Tag Support",
                    subtitle = "Reads REPLAYGAIN_TRACK_GAIN metadata when embedded",
                    checked = effectSettings.replayGainEnabled,
                    onCheckedChange = { checked -> onUpdateEffects { it.copy(replayGainEnabled = checked) } }
                )
            }
        }

        // Playback Speed & Pitch
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tempo & Pitch", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onSpeedChange(1.0f); onPitchChange(1.0f) }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset 1.0x")
                    }
                }

                SliderSettingRow(
                    label = "Playback Speed",
                    valueText = String.format("%.2fx", playbackSpeed),
                    value = playbackSpeed,
                    range = 0.5f..2.0f,
                    onValueChange = onSpeedChange
                )

                SliderSettingRow(
                    label = "Pitch Control",
                    valueText = String.format("%.2fx", pitch),
                    value = pitch,
                    range = 0.5f..2.0f,
                    onValueChange = onPitchChange
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showSaveDialog) {
        var profileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Save Custom EQ Profile") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onSaveCustomPreset(profileName)
                            showSaveDialog = false
                        }
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun EqSwitchRow(
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
