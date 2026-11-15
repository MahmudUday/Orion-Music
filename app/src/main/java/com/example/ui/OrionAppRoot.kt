package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.AddToPlaylistsDialog
import com.example.ui.components.LyricsEditorDialog
import com.example.ui.components.OrionAmbientBackdrop
import com.example.ui.components.OrionSplashOverlay
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.SongDetailsDialog
import com.example.ui.components.SongOptionsBottomSheet
import com.example.ui.player.NowPlayingScreen
import com.example.ui.player.OrionMiniPlayer
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryDrillDown
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.OrionMusicTheme
import com.example.ui.theme.OrionUiConfig
import com.example.ui.viewmodel.OrionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OrionAppRoot(
    viewModel: OrionViewModel,
    initialAudioUri: Uri? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val appearance by viewModel.appearanceSettings.collectAsStateWithLifecycle()
    val playbackSettings by viewModel.playbackSettings.collectAsStateWithLifecycle()
    val librarySettings by viewModel.librarySettings.collectAsStateWithLifecycle()
    val audioEffectSettings by viewModel.audioEffectSettings.collectAsStateWithLifecycle()
    val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val eqHardwareState by viewModel.equalizerHardwareState.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val hiddenSongs by viewModel.hiddenSongs.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val safFolders by viewModel.safFolders.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.scanLibrary(scheduleBackground = true)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Custom Folder"
            viewModel.addSafFolder(uri, displayName)
        }
    }

    var lyricsTargetSong by remember { mutableStateOf<Song?>(null) }
    val safLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = lyricsTargetSong ?: playbackState.currentSong
        if (uri != null && target != null) {
            viewModel.importLyricsFile(target, uri)
            scope.launch { snackbarHostState.showSnackbar("Lyrics imported from file") }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val jsonText = viewModel.exportBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(jsonText.toByteArray(Charsets.UTF_8))
                        }
                        true
                    }.getOrDefault(false)
                }
                snackbarHostState.showSnackbar(if (ok) "Backup exported successfully" else "Export failed")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw != null) {
                    val result = viewModel.importBackupJson(raw)
                    val msg = result.fold(
                        onSuccess = { count -> "Restored settings & $count playlists" },
                        onFailure = { "Invalid backup file" }
                    )
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(850L)
        showSplash = false
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(audioPermission)
        } else {
            viewModel.scanLibrary(scheduleBackground = true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(initialAudioUri) {
        if (initialAudioUri != null) {
            viewModel.playExternalAudioUri(initialAudioUri)
        }
    }

    LaunchedEffect(libraryState.lastScanMessage) {
        val msg = libraryState.lastScanMessage
        if (!msg.isNullOrBlank() && !libraryState.isScanning) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearScanMessage()
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var libraryDrillDown by remember { mutableStateOf<LibraryDrillDown?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    var moreOptionsSong by remember { mutableStateOf<Song?>(null) }
    var detailsSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var showLyricsEditor by remember { mutableStateOf(false) }

    val uiConfig = OrionUiConfig(
        amoledBlack = appearance.amoledBlack,
        accentPreset = appearance.accentPreset,
        dynamicArtworkColors = appearance.dynamicArtworkColors,
        animationIntensity = appearance.animationIntensity,
        artworkStyle = appearance.artworkStyle,
        layoutDensity = appearance.layoutDensity,
        activeArtworkPrimary = playbackState.dynamicPrimaryColor,
        activeArtworkSecondary = playbackState.dynamicSecondaryColor
    )

    OrionMusicTheme(uiConfig = uiConfig) {
        SharedTransitionLayout {
            Box(modifier = Modifier.fillMaxSize()) {
                OrionAmbientBackdrop(intensity = 0.18f)

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!isWideScreen) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    AnimatedVisibility(
                                        visible = playbackState.currentSong != null && !isNowPlayingExpanded && !showEqualizer
                                    ) {
                                        OrionMiniPlayer(
                                            playbackState = playbackState,
                                            onExpand = { isNowPlayingExpanded = true },
                                            onTogglePlayPause = viewModel::togglePlayPause,
                                            onNext = viewModel::skipNext,
                                            onPrevious = viewModel::skipPrevious,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this
                                        )
                                    }
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                        windowInsets = WindowInsets.navigationBars
                                    ) {
                                        MainNavTab.entries.forEach { tab ->
                                            val selected = currentTab == tab
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    currentTab = tab
                                                    if (tab != MainNavTab.LIBRARY) libraryDrillDown = null
                                                    if (tab != MainNavTab.PLAYLISTS) selectedPlaylist = null
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label, maxLines = 1) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                ),
                                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding())
                        ) {
                            if (isWideScreen) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    MainNavTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                currentTab = tab
                                                if (tab != MainNavTab.LIBRARY) libraryDrillDown = null
                                                if (tab != MainNavTab.PLAYLISTS) selectedPlaylist = null
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                val contentBottomPad = PaddingValues(
                                    bottom = innerPadding.calculateBottomPadding()
                                )

                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "main_tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        MainNavTab.HOME -> HomeScreen(
                                            libraryState = libraryState,
                                            playbackState = playbackState,
                                            recentlyPlayed = recentlyPlayed,
                                            recentlyAdded = recentlyAdded,
                                            mostPlayed = mostPlayed,
                                            favorites = favorites,
                                            playlists = playlists,
                                            sleepTimerState = sleepTimerState,
                                            hasAudioPermission = hasAudioPermission,
                                            onRequestPermission = { audioPermissionLauncher.launch(audioPermission) },
                                            onAddSafFolder = { safFolderLauncher.launch(null) },
                                            onRescanLibrary = { viewModel.scanLibrary(scheduleBackground = true) },
                                            onPlaySongs = viewModel::playSongList,
                                            onTogglePlayPause = viewModel::togglePlayPause,
                                            onExpandNowPlaying = { isNowPlayingExpanded = true },
                                            onOpenEqualizer = { showEqualizer = true },
                                            onOpenSleepTimer = { showSleepTimerSheet = true },
                                            onOpenSongMore = { moreOptionsSong = it },
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onNavigateToLibraryTab = { currentTab = MainNavTab.LIBRARY },
                                            onOpenAlbum = { album ->
                                                libraryDrillDown = LibraryDrillDown.AlbumDetail(album)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenArtist = { artist ->
                                                libraryDrillDown = LibraryDrillDown.ArtistDetail(artist)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenGenre = { genre ->
                                                libraryDrillDown = LibraryDrillDown.GenreDetail(genre)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenPlaylist = { pl ->
                                                selectedPlaylist = pl
                                                currentTab = MainNavTab.PLAYLISTS
                                            },
                                            contentBottomPadding = contentBottomPad
                                        )

                                        MainNavTab.LIBRARY -> LibraryScreen(
                                            libraryState = libraryState,
                                            librarySettings = librarySettings,
                                            playbackState = playbackState,
                                            drillDown = libraryDrillDown,
                                            hasAudioPermission = hasAudioPermission,
                                            onSetDrillDown = { libraryDrillDown = it },
                                            onRequestPermission = { audioPermissionLauncher.launch(audioPermission) },
                                            onAddSafFolder = { safFolderLauncher.launch(null) },
                                            onRescanLibrary = { viewModel.scanLibrary(scheduleBackground = true) },
                                            onUpdateSort = { order, asc ->
                                                viewModel.updateLibrarySettings {
                                                    it.copy(sortOrderName = order.name, sortAscending = asc)
                                                }
                                            },
                                            onPlaySongs = viewModel::playSongList,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onOpenSongMore = { moreOptionsSong = it },
                                            onExcludeFolder = { folder ->
                                                viewModel.excludeFolder(folder.path, folder.name)
                                            },
                                            contentBottomPadding = contentBottomPad
                                        )

                                        MainNavTab.SEARCH -> SearchScreen(
                                            libraryState = libraryState,
                                            playlists = playlists,
                                            recentSearches = recentSearches,
                                            playbackState = playbackState,
                                            onRecordSearch = viewModel::addSearchHistory,
                                            onClearSearchHistory = viewModel::clearSearchHistory,
                                            onPlaySongs = viewModel::playSongList,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onOpenSongMore = { moreOptionsSong = it },
                                            onOpenAlbum = { album ->
                                                libraryDrillDown = LibraryDrillDown.AlbumDetail(album)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenArtist = { artist ->
                                                libraryDrillDown = LibraryDrillDown.ArtistDetail(artist)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenGenre = { genre ->
                                                libraryDrillDown = LibraryDrillDown.GenreDetail(genre)
                                                currentTab = MainNavTab.LIBRARY
                                            },
                                            onOpenPlaylist = { pl ->
                                                selectedPlaylist = pl
                                                currentTab = MainNavTab.PLAYLISTS
                                            },
                                            contentBottomPadding = contentBottomPad
                                        )

                                        MainNavTab.PLAYLISTS -> PlaylistsScreen(
                                            playlists = playlists,
                                            allSongs = libraryState.sortedSongs,
                                            selectedPlaylist = selectedPlaylist,
                                            playbackState = playbackState,
                                            observePlaylistSongs = viewModel::observeSongsForPlaylist,
                                            onSelectPlaylist = { selectedPlaylist = it },
                                            onCreatePlaylist = viewModel::createPlaylist,
                                            onRenamePlaylist = viewModel::renamePlaylist,
                                            onDeletePlaylist = viewModel::deletePlaylist,
                                            onPlaySongs = viewModel::playSongList,
                                            onAddSongsToPlaylist = { pid, ids ->
                                                ids.forEach { sid -> viewModel.addSongToPlaylists(listOf(pid), sid) }
                                            },
                                            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                                            onReorderPlaylistSongs = viewModel::reorderPlaylistSongs,
                                            onToggleFavorite = viewModel::toggleFavorite,
                                            onOpenSongMore = { moreOptionsSong = it },
                                            contentBottomPadding = contentBottomPad
                                        )

                                        MainNavTab.SETTINGS -> SettingsScreen(
                                            appearance = appearance,
                                            playback = playbackSettings,
                                            library = librarySettings,
                                            notifications = notificationSettings,
                                            libraryState = libraryState,
                                            sleepTimerState = sleepTimerState,
                                            excludedFolders = excludedFolders,
                                            safFolders = safFolders,
                                            hiddenSongs = hiddenSongs,
                                            onUpdateAppearance = viewModel::updateAppearance,
                                            onUpdatePlayback = viewModel::updatePlaybackSettings,
                                            onUpdateLibrary = viewModel::updateLibrarySettings,
                                            onUpdateNotifications = viewModel::updateNotificationSettings,
                                            onOpenEqualizer = { showEqualizer = true },
                                            onOpenSleepTimer = { showSleepTimerSheet = true },
                                            onRescanLibrary = { viewModel.scanLibrary(scheduleBackground = true) },
                                            onAddSafFolder = { safFolderLauncher.launch(null) },
                                            onRemoveSafFolder = viewModel::removeSafFolder,
                                            onRemoveExcludedFolder = viewModel::removeExcludedFolder,
                                            onUnhideSong = { songId -> viewModel.hideSong(songId, false) },
                                            onUnhideAll = viewModel::unhideAllSongs,
                                            onClearCache = {
                                                viewModel.clearArtworkAndTempCache()
                                                scope.launch { snackbarHostState.showSnackbar("Cache cleared") }
                                            },
                                            onExportBackup = {
                                                exportBackupLauncher.launch("orion_music_backup.json")
                                            },
                                            onImportBackup = {
                                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                            },
                                            onResetSettings = {
                                                viewModel.resetAllSettings()
                                                scope.launch { snackbarHostState.showSnackbar("Settings reset to defaults") }
                                            },
                                            contentBottomPadding = contentBottomPad
                                        )
                                    }
                                }

                                if (isWideScreen && playbackState.currentSong != null && !isNowPlayingExpanded && !showEqualizer) {
                                    OrionMiniPlayer(
                                        playbackState = playbackState,
                                        onExpand = { isNowPlayingExpanded = true },
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onNext = viewModel::skipNext,
                                        onPrevious = viewModel::skipPrevious,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Full-screen Now Playing overlay with spring transition & shared artwork
                    AnimatedVisibility(
                        visible = isNowPlayingExpanded && playbackState.currentSong != null,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 380f)
                        ) + fadeOut()
                    ) {
                        NowPlayingScreen(
                            playbackState = playbackState,
                            sleepTimerState = sleepTimerState,
                            onMinimize = { isNowPlayingExpanded = false },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::skipNext,
                            onPrevious = viewModel::skipPrevious,
                            onSeekTo = viewModel::seekTo,
                            onSeekBy = viewModel::seekBy,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onCycleRepeat = viewModel::cycleRepeatMode,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onVolumeChange = viewModel::setDeviceVolume,
                            onOpenEqualizer = { showEqualizer = true },
                            onOpenSleepTimer = { showSleepTimerSheet = true },
                            onOpenLyricsEditor = {
                                lyricsTargetSong = playbackState.currentSong
                                showLyricsEditor = true
                            },
                            onOpenMoreOptions = { moreOptionsSong = it },
                            onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                            onMoveQueueItem = viewModel::moveQueueItem,
                            onRemoveQueueItem = viewModel::removeQueueItem,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    // Dedicated Audio Equalizer & DSP Studio overlay
                    AnimatedVisibility(
                        visible = showEqualizer,
                        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            EqualizerScreen(
                                effectSettings = audioEffectSettings,
                                hardwareState = eqHardwareState,
                                customPresets = customEqPresets,
                                playbackSpeed = playbackState.playbackSpeed,
                                pitch = playbackState.pitch,
                                onBack = { showEqualizer = false },
                                onToggleEq = viewModel::setEqEnabled,
                                onSelectPreset = viewModel::selectEqPreset,
                                onBandLevelChange = viewModel::setEqBandLevel,
                                onSaveCustomPreset = viewModel::saveCustomEqPreset,
                                onApplyCustomPreset = viewModel::applyCustomEqPreset,
                                onDeleteCustomPreset = viewModel::deleteCustomEqPreset,
                                onUpdateEffects = viewModel::updateAudioEffects,
                                onSpeedChange = viewModel::setPlaybackSpeed,
                                onPitchChange = viewModel::setPlaybackPitch
                            )
                        }
                    }

                    // Animated ORION MUSIC Splash overlay
                    OrionSplashOverlay(visible = showSplash)
                }
            }
        }
    }

    moreOptionsSong?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            onDismiss = { moreOptionsSong = null },
            onPlayNext = { viewModel.playNextInQueue(song) },
            onAddToQueue = { viewModel.addToQueue(song) },
            onAddToPlaylists = { playlistPickerSong = song },
            onViewAlbum = {
                val foundAlbum = libraryState.albums.find { it.id == song.albumId && it.name == song.album }
                if (foundAlbum != null) {
                    isNowPlayingExpanded = false
                    libraryDrillDown = LibraryDrillDown.AlbumDetail(foundAlbum)
                    currentTab = MainNavTab.LIBRARY
                }
            },
            onViewArtist = {
                val foundArtist = libraryState.artists.find { it.name == song.artist }
                if (foundArtist != null) {
                    isNowPlayingExpanded = false
                    libraryDrillDown = LibraryDrillDown.ArtistDetail(foundArtist)
                    currentTab = MainNavTab.LIBRARY
                }
            },
            onShowDetails = { detailsSong = song },
            onExcludeFolder = { viewModel.excludeFolder(song.folderPath, song.folderName) },
            onHideSong = { viewModel.hideSong(song.id, true) }
        )
    }

    detailsSong?.let { song ->
        SongDetailsDialog(song = song, onDismiss = { detailsSong = null })
    }

    playlistPickerSong?.let { song ->
        AddToPlaylistsDialog(
            song = song,
            playlists = playlists,
            onDismiss = { playlistPickerSong = null },
            onCreateNewPlaylist = viewModel::createPlaylist,
            onConfirmSelection = { ids -> viewModel.addSongToPlaylists(ids, song.id) }
        )
    }

    if (showLyricsEditor) {
        val target = lyricsTargetSong ?: playbackState.currentSong
        if (target != null) {
            LyricsEditorDialog(
                initialRawText = playbackState.lyrics?.rawText.orEmpty(),
                onDismiss = { showLyricsEditor = false },
                onPickLrcFile = { safLyricsLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*")) },
                onSaveLyrics = { raw -> viewModel.saveCustomLyrics(target, raw, "Manual") }
            )
        }
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            state = sleepTimerState,
            onDismiss = { showSleepTimerSheet = false },
            onStartMinutes = viewModel::startSleepTimerMinutes,
            onStartClockTime = viewModel::startSleepTimerAtClock,
            onStartEndOfTrack = viewModel::startSleepTimerEndOfTrack,
            onCancelTimer = viewModel::cancelSleepTimer
        )
    }
}
