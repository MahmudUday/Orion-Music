package com.example.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.LyricsParser
import com.example.data.model.LyricsData
import com.example.data.model.RepeatModeOption
import com.example.data.model.SleepTimerState
import com.example.data.model.Song
import com.example.ui.components.OrionAmbientBackdrop
import com.example.ui.components.OrionArtwork
import com.example.ui.components.OrionEqualizerVisualizer
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.PlaybackUiState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    sleepTimerState: SleepTimerState,
    onMinimize: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenLyricsEditor: () -> Unit,
    onOpenMoreOptions: (Song) -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    BackHandler { onMinimize() }

    val song = playbackState.currentSong ?: return
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showVolumeRow by remember { mutableStateOf(false) }
    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> verticalDrag += dragAmount },
                    onDragEnd = {
                        if (verticalDrag > 150f) onMinimize()
                        verticalDrag = 0f
                    }
                )
            }
            .testTag("now_playing_screen")
    ) {
        OrionAmbientBackdrop(intensity = 0.34f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("minimize_player_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onOpenMoreOptions(song) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("now_playing_more_btn")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Song options")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main center stage: Artwork OR Synchronized Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics) {
                    SyncedLyricsStage(
                        lyricsData = playbackState.lyrics,
                        positionMs = playbackState.positionMs,
                        onSeekTo = onSeekTo,
                        onOpenEditor = onOpenLyricsEditor,
                        onCloseLyrics = { showLyrics = false }
                    )
                } else {
                    ArtworkStage(
                        song = song,
                        isPlaying = playbackState.isPlaying,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Song title, artist, tech badge, favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = song.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "np_title"
                    ) { titleText ->
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrionEqualizerVisualizer(
                            isPlaying = playbackState.isPlaying,
                            barCount = 5,
                            modifier = Modifier.size(width = 24.dp, height = 14.dp)
                        )
                        val codec = song.mimeType.substringAfter('/').uppercase().ifBlank { "AUDIO" }
                        val techInfo = buildString {
                            append(codec)
                            if (song.bitrateKbps > 0) append(" • ${song.bitrateKbps} kbps")
                            if (playbackState.playbackSpeed != 1.0f) append(" • ${playbackState.playbackSpeed}x")
                        }
                        Text(
                            text = techInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleFavorite(song.id) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("now_playing_favorite_btn")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seek bar with elapsed / remaining & quick -10s / +10s fine scrub buttons
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            var scrubbingPosition by remember(song.id) { mutableFloatStateOf(-1f) }
            val displayedPositionMs = if (scrubbingPosition >= 0f) {
                (scrubbingPosition * duration).toLong()
            } else {
                playbackState.positionMs.coerceIn(0L, duration)
            }
            val sliderFraction = if (scrubbingPosition >= 0f) {
                scrubbingPosition
            } else {
                (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }

            Slider(
                value = sliderFraction,
                onValueChange = { scrubbingPosition = it },
                onValueChangeFinished = {
                    if (scrubbingPosition >= 0f) {
                        onSeekTo((scrubbingPosition * duration).toLong())
                        scrubbingPosition = -1f
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_seekbar")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FormatUtils.formatDuration(displayedPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onSeekBy(-10_000L) }
                    )
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Forward 10 seconds",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onSeekBy(10_000L) }
                    )
                }
                Text(
                    text = "-${FormatUtils.formatDuration((duration - displayedPositionMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("np_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("np_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("np_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_repeat_btn")
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatModeOption.ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat mode ${playbackState.repeatMode.label}",
                        tint = if (playbackState.repeatMode != RepeatModeOption.OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (showVolumeRow) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = playbackState.deviceVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Utility strip: Volume, Equalizer, Lyrics, Sleep Timer, Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showVolumeRow = !showVolumeRow },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Volume control",
                        tint = if (showVolumeRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_equalizer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Audio Equalizer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showLyrics = !showLyrics },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_lyrics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Toggle Lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_sleep_timer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NightlightRound,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Playback Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            isPlaying = playbackState.isPlaying,
            onDismiss = { showQueueSheet = false },
            onJumpToIndex = onJumpToQueueIndex,
            onMoveItem = onMoveQueueItem,
            onRemoveItem = onRemoveQueueItem
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ArtworkStage(
    song: Song,
    isPlaying: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 240f),
        label = "artwork_breathing"
    )
    var dragDelta by remember { mutableFloatStateOf(0f) }

    AnimatedContent(
        targetState = song,
        transitionSpec = {
            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                (slideOutHorizontally { -it / 3 } + fadeOut())
        },
        label = "np_artwork_switch",
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .aspectRatio(1f)
            .scale(scale)
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDelta = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragDelta += dragAmount },
                    onDragEnd = {
                        if (dragDelta < -75f) onNext()
                        else if (dragDelta > 75f) onPrevious()
                        dragDelta = 0f
                    }
                )
            }
    ) { activeSong ->
        val baseModifier = Modifier
            .fillMaxSize()
            .let { mod ->
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        mod.sharedElement(
                            state = rememberSharedContentState(key = "player_artwork"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    mod
                }
            }

        OrionArtwork(
            artworkUri = activeSong.artworkUri,
            contentDescription = "${activeSong.title} artwork",
            modifier = baseModifier,
            cornerRadiusOverride = 28.dp,
            glowEnabled = isPlaying
        )
    }
}

@Composable
private fun SyncedLyricsStage(
    lyricsData: LyricsData?,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    onCloseLyrics: () -> Unit
) {
    val lines = lyricsData?.lines.orEmpty()
    val activeIdx = remember(lines, positionMs) {
        if (lyricsData?.isSynced == true) LyricsParser.findActiveLineIndex(lines, positionMs) else -1
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && activeIdx < lines.size) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxSize()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lyricsData?.sourceLabel ?: "Offline Lyrics",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    TextButton(onClick = onOpenEditor) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit / .LRC")
                    }
                    TextButton(onClick = onCloseLyrics) {
                        Text("Art")
                    }
                }
            }

            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No offline lyrics embedded or attached",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add a local .lrc file or paste lyrics manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onOpenEditor) {
                            Text("Add Lyrics")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = index == activeIdx
                        Text(
                            text = line.text,
                            style = if (isActive) {
                                MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                lyricsData?.isSynced == true -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = line.timestampMs != null) {
                                    line.timestampMs?.let(onSeekTo)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
