package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AnimationIntensity
import com.example.data.model.ArtworkStyle
import com.example.data.model.LayoutDensity
import com.example.data.model.Song
import com.example.ui.theme.LocalOrionUiConfig
import com.example.ui.util.FormatUtils

@Composable
fun OrionArtwork(
    artworkUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadiusOverride: Dp? = null,
    glowEnabled: Boolean = false
) {
    val uiConfig = LocalOrionUiConfig.current
    val radius = cornerRadiusOverride ?: when (uiConfig.artworkStyle) {
        ArtworkStyle.ROUNDED -> 14.dp
        ArtworkStyle.SQUARE -> 4.dp
    }
    val shape = RoundedCornerShape(radius)
    val context = LocalContext.current

    Box(
        modifier = modifier
            .then(
                if (glowEnabled) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            )
                        ),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .crossfade(uiConfig.animationIntensity != AnimationIntensity.MINIMAL)
                    .error(R.drawable.img_orion_default_cover)
                    .fallback(R.drawable.img_orion_default_cover)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_orion_default_cover),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun OrionAmbientBackdrop(
    modifier: Modifier = Modifier,
    intensity: Float = 0.22f
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = bg)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = intensity),
                    primary.copy(alpha = intensity * 0.25f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.18f, size.height * 0.08f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.18f, size.height * 0.08f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = intensity * 0.75f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.88f, size.height * 0.28f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.88f, size.height * 0.28f)
        )
    }
}

@Composable
fun OrionEqualizerVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val uiConfig = LocalOrionUiConfig.current
    val animate = isPlaying && uiConfig.animationIntensity != AnimationIntensity.MINIMAL

    val infiniteTransition = rememberInfiniteTransition(label = "eq_visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )
    val activeFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.22f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f),
        label = "eq_active"
    )

    Canvas(modifier = modifier) {
        val spacing = size.width * 0.12f
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

        for (i in 0 until barCount) {
            val wave = if (animate) {
                val freq = 1.4f + (i % 3) * 0.6f
                (0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(phase * freq + i * 0.9f))).toFloat()
            } else {
                0.35f + (i % 2) * 0.25f
            }
            val barHeight = (size.height * wave * activeFactor).coerceIn(size.height * 0.16f, size.height)
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, barColor)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun OrionSplashOverlay(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(420)) + scaleOut(
            targetScale = 1.06f,
            animationSpec = tween(420)
        )
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        val transition = rememberInfiniteTransition(label = "splash_glow")
        val pulse by transition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "splash_pulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            OrionAmbientBackdrop(intensity = 0.28f * pulse)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier.size(118.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(primary, secondary, tertiary, primary)
                            ),
                            radius = (size.minDimension / 2f) - strokeWidth,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    OrionEqualizerVisualizer(
                        isPlaying = true,
                        barCount = 5,
                        modifier = Modifier.size(width = 48.dp, height = 40.dp),
                        barColor = primary,
                        secondaryColor = tertiary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ORION MUSIC",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "YOUR MUSIC. YOUR UNIVERSE.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val density = LocalOrionUiConfig.current.layoutDensity
    val verticalPad = if (density == LayoutDensity.COMPACT) 6.dp else 10.dp
    val artSize = if (density == LayoutDensity.COMPACT) 46.dp else 54.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = verticalPad)
            .testTag("song_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(artSize)) {
            OrionArtwork(
                artworkUri = song.artworkUri,
                contentDescription = "${song.title} album art",
                modifier = Modifier.fillMaxSize(),
                glowEnabled = isCurrent
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.52f)),
                    contentAlignment = Alignment.Center
                ) {
                    OrionEqualizerVisualizer(
                        isPlaying = isPlaying,
                        barCount = 4,
                        modifier = Modifier.size(width = 22.dp, height = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "  •  ${FormatUtils.formatDuration(song.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }

        if (onFavoriteClick != null) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("fav_btn_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("song_more_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options for ${song.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    hasPermission: Boolean,
    isScanning: Boolean,
    onRequestPermission: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_orion_empty_library),
            contentDescription = "Empty music library illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(24.dp)
                )
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (hasPermission) "No Offline Tracks Found Yet" else "Unlock Your Offline Music",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasPermission) {
                "ORION MUSIC indexes MP3, FLAC, WAV, AAC, M4A, OGG, and OPUS across Music, Downloads, Recordings, and custom folders."
            } else {
                "Grant local audio access so ORION MUSIC can automatically index songs stored on your device — 100% offline."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))

        if (!hasPermission) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_audio_permission_button")
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Audio Access")
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Button(
                onClick = onRescan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("rescan_library_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Scanning Storage..." else "Scan Device Audio")
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onAddSafFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_saf_folder_button")
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Custom Folder (SAF)")
        }
    }
}
