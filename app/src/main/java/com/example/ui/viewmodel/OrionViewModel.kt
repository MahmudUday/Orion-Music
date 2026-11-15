package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.OrionMusicApp
import com.example.audio.EqualizerHardwareState
import com.example.audio.LyricsParser
import com.example.audio.OrionPlaybackService
import com.example.data.local.CustomEqPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.SafFolderEntity
import com.example.data.model.Album
import com.example.data.model.AnimationIntensity
import com.example.data.model.AppearanceSettings
import com.example.data.model.Artist
import com.example.data.model.ArtworkStyle
import com.example.data.model.AudioEffectSettings
import com.example.data.model.AudioFocusBehavior
import com.example.data.model.EqualizerPresetType
import com.example.data.model.Genre
import com.example.data.model.LayoutDensity
import com.example.data.model.LibrarySettings
import com.example.data.model.LibraryStats
import com.example.data.model.MusicFolder
import com.example.data.model.NotificationSettings
import com.example.data.model.PlaybackSettings
import com.example.data.model.Playlist
import com.example.data.model.RepeatModeOption
import com.example.data.model.SleepTimerState
import com.example.data.model.Song
import com.example.data.model.SongSortOrder
import com.example.ui.theme.AccentColorPreset
import com.example.ui.util.ArtworkPaletteExtractor
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OrionMusicApp
    private val container = app.container
    private val repository = container.repository
    private val settings = container.settingsDataStore
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTrackerJob: Job? = null
    private var hasRestoredSession = false

    private val _playbackState = MutableStateFlow(
        PlaybackUiState(deviceVolume = readNormalizedVolume())
    )
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    private val _cacheBytes = MutableStateFlow(0L)

    val appearanceSettings: StateFlow<AppearanceSettings> = settings.appearanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceSettings())

    val playbackSettings: StateFlow<PlaybackSettings> = settings.playbackFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackSettings())

    val librarySettings: StateFlow<LibrarySettings> = settings.libraryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibrarySettings())

    val audioEffectSettings: StateFlow<AudioEffectSettings> = settings.audioEffectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioEffectSettings())

    val notificationSettings: StateFlow<NotificationSettings> = settings.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettings())

    val equalizerHardwareState: StateFlow<EqualizerHardwareState> = container.effectsController.hardwareState
    val sleepTimerState: StateFlow<SleepTimerState> = container.sleepTimerManager.state

    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAddedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenSongs: StateFlow<List<Song>> = repository.hiddenSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = repository.excludedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safFolders: StateFlow<List<SafFolderEntity>> = repository.safFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEqPresets: StateFlow<List<CustomEqPresetEntity>> = repository.customEqPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<String>> = settings.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryState: StateFlow<LibraryUiState> = combine(
        repository.visibleSongsFlow,
        librarySettings,
        playlists,
        combine(hiddenSongs, excludedFolders, _cacheBytes) { h, e, c -> Triple(h, e, c) },
        combine(_isScanning, _scanStatusMessage) { s, m -> s to m }
    ) { songs, libSettings, plList, metaTriple, scanPair ->
        val (hidden, excluded, cacheSize) = metaTriple
        val (scanning, scanMsg) = scanPair
        val sorted = sortSongs(songs, libSettings.sortOrder, libSettings.sortAscending)

        val albums = songs.groupBy { it.albumId to it.album }
            .map { (key, group) ->
                val first = group.first()
                Album(
                    id = key.first,
                    name = key.second,
                    artist = group.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key ?: first.artist,
                    songCount = group.size,
                    artworkUri = group.firstOrNull { it.artworkUri != null }?.artworkUri,
                    year = group.maxOfOrNull { it.year } ?: 0,
                    totalDurationMs = group.sumOf { it.durationMs }
                )
            }
            .sortedBy { it.name.lowercase() }

        val artists = songs.groupBy { it.artist }
            .map { (name, group) ->
                Artist(
                    name = name,
                    songCount = group.size,
                    albumCount = group.map { it.album }.distinct().size,
                    artworkUri = group.firstOrNull { it.artworkUri != null }?.artworkUri
                )
            }
            .sortedBy { it.name.lowercase() }

        val genres = songs.groupBy { it.genre }
            .map { (name, group) ->
                Genre(
                    name = name,
                    songCount = group.size,
                    artworkUri = group.firstOrNull { it.artworkUri != null }?.artworkUri
                )
            }
            .sortedByDescending { it.songCount }

        val folders = songs.groupBy { it.folderPath }
            .map { (path, group) ->
                val first = group.first()
                MusicFolder(
                    path = path,
                    name = first.folderName,
                    songCount = group.size,
                    artworkUri = group.firstOrNull { it.artworkUri != null }?.artworkUri,
                    isSafFolder = first.isSafDocument
                )
            }
            .sortedBy { it.name.lowercase() }

        val stats = LibraryStats(
            totalSongs = songs.size,
            totalAlbums = albums.size,
            totalArtists = artists.size,
            totalPlaylists = plList.size,
            totalDurationMs = songs.sumOf { it.durationMs },
            totalSizeBytes = songs.sumOf { it.sizeBytes },
            hiddenSongsCount = hidden.size,
            excludedFoldersCount = excluded.size,
            artworkCacheBytes = cacheSize
        )

        LibraryUiState(
            allSongs = songs,
            sortedSongs = sorted,
            albums = albums,
            artists = artists,
            genres = genres,
            folders = folders,
            stats = stats,
            isScanning = scanning,
            lastScanMessage = scanMsg
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        viewModelScope.launch {
            repository.ensureFavoritesPlaylist()
            refreshCacheSize()
        }
        connectMediaController()
    }

    private fun connectMediaController() {
        val token = SessionToken(app, ComponentName(app, OrionPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            mediaController = controller
            attachPlayerListener(controller)
            syncControllerToState(controller)
            startPositionTracker()
            viewModelScope.launch { restoreLastSessionIfNeeded(controller) }
        }, MoreExecutors.directExecutor())
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncControllerToState(controller)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                syncControllerToState(controller)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.update { it.copy(repeatMode = mapRepeatMode(repeatMode)) }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackState.update {
                    it.copy(playbackSpeed = playbackParameters.speed, pitch = playbackParameters.pitch)
                }
            }
        })
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L }
                        ?: _playbackState.value.currentSong?.durationMs
                        ?: 0L
                    _playbackState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            isPlaying = controller.isPlaying,
                            deviceVolume = readNormalizedVolume()
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private suspend fun restoreLastSessionIfNeeded(controller: MediaController) {
        if (hasRestoredSession) return
        hasRestoredSession = true
        if (controller.mediaItemCount > 0) {
            syncControllerToState(controller)
            return
        }
        val pbSettings = settings.playbackFlow.first()
        if (!pbSettings.resumeLastSong) return
        val snapshot = settings.getResumeSnapshotOnce()
        if (snapshot.queueSongIds.isEmpty()) return

        val allSongs = repository.visibleSongsFlow.first()
        val byId = allSongs.associateBy { it.id }
        val queue = snapshot.queueSongIds.mapNotNull { byId[it] }
        if (queue.isEmpty()) return

        val startIdx = queue.indexOfFirst { it.id == snapshot.currentSongId }.coerceAtLeast(0)
        val startPos = if (pbSettings.rememberPosition) snapshot.positionMs else 0L

        controller.setMediaItems(queue.map { it.toMediaItem() }, startIdx, startPos)
        controller.shuffleModeEnabled = snapshot.shuffleEnabled
        controller.repeatMode = when (snapshot.repeatModeName) {
            "ALL" -> Player.REPEAT_MODE_ALL
            "ONE" -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.prepare()
        syncControllerToState(controller)
    }

    private fun syncControllerToState(controller: MediaController) {
        val allSongs = libraryState.value.allSongs
        val byId = allSongs.associateBy { it.id }
        val queue = (0 until controller.mediaItemCount).mapNotNull { idx ->
            val item = controller.getMediaItemAt(idx)
            val id = item.mediaId.toLongOrNull()
            id?.let { byId[it] } ?: item.toFallbackSong()
        }
        val currentIdx = controller.currentMediaItemIndex.coerceIn(-1, queue.lastIndex)
        val currentSong = queue.getOrNull(currentIdx)

        val previousSongId = _playbackState.value.currentSong?.id
        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                queue = queue,
                currentIndex = currentIdx,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.takeIf { d -> d > 0L } ?: currentSong?.durationMs ?: 0L,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = mapRepeatMode(controller.repeatMode),
                playbackSpeed = controller.playbackParameters.speed,
                pitch = controller.playbackParameters.pitch
            )
        }

        if (currentSong != null && currentSong.id != previousSongId) {
            refreshColorsAndLyrics(currentSong)
        } else if (currentSong == null) {
            _playbackState.update { it.copy(dynamicPrimaryColor = null, dynamicSecondaryColor = null, lyrics = null) }
        }
    }

    private fun refreshColorsAndLyrics(song: Song) {
        viewModelScope.launch {
            val colors = ArtworkPaletteExtractor.extractColors(app, song.artworkUri, song.contentUri)
            _playbackState.update {
                it.copy(
                    dynamicPrimaryColor = colors?.primary,
                    dynamicSecondaryColor = colors?.secondary
                )
            }
        }
        viewModelScope.launch {
            val lyrics = repository.loadLyricsForSong(song)
            _playbackState.update { it.copy(lyrics = lyrics) }
        }
        viewModelScope.launch {
            val updated = repository.refreshSongTechInfo(song)
            if (updated != song && _playbackState.value.currentSong?.id == song.id) {
                _playbackState.update { it.copy(currentSong = updated) }
            }
        }
    }

    fun scanLibrary(scheduleBackground: Boolean = false) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _scanStatusMessage.value = "Scanning local storage..."
            val summary = repository.scanLibraryNow()
            _isScanning.value = false
            _scanStatusMessage.value = "Indexed ${summary.totalIndexed} tracks (${summary.newlyAdded} new)"
            if (scheduleBackground) {
                repository.scheduleBackgroundScan()
            }
            refreshCacheSize()
        }
    }

    fun clearScanMessage() {
        _scanStatusMessage.value = null
    }

    fun playSongList(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return
        val safeIdx = startIndex.coerceIn(0, songs.lastIndex)
        controller.setMediaItems(songs.map { it.toMediaItem() }, safeIdx, 0L)
        controller.shuffleModeEnabled = shuffle
        controller.prepare()
        controller.play()
        syncControllerToState(controller)
    }

    fun playExternalAudioUri(uri: Uri) {
        val controller = mediaController ?: return
        val item = MediaItem.Builder()
            .setMediaId("-999")
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(uri.lastPathSegment ?: "Audio Track")
                    .setArtist("External File")
                    .build()
            )
            .build()
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
        syncControllerToState(controller)
    }

    fun playNextInQueue(song: Song) {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            playSongList(listOf(song), 0, false)
            return
        }
        val insertPos = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertPos, song.toMediaItem())
        syncControllerToState(controller)
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            playSongList(listOf(song), 0, false)
            return
        }
        controller.addMediaItem(song.toMediaItem())
        syncControllerToState(controller)
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
            syncControllerToState(controller)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
            syncControllerToState(controller)
        }
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            syncControllerToState(controller)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0) {
                val all = libraryState.value.sortedSongs
                if (all.isNotEmpty()) {
                    playSongList(all, 0, false)
                }
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _playbackState.update { it.copy(positionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        val current = _playbackState.value.positionMs
        val dur = _playbackState.value.durationMs
        seekTo((current + deltaMs).coerceIn(0L, dur.coerceAtLeast(0L)))
    }

    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safe = speed.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackParameters(
            PlaybackParameters(safe, _playbackState.value.pitch)
        )
        viewModelScope.launch {
            settings.updatePlayback { it.copy(defaultPlaybackSpeed = safe) }
        }
    }

    fun setPlaybackPitch(pitch: Float) {
        val safe = pitch.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackParameters(
            PlaybackParameters(_playbackState.value.playbackSpeed, safe)
        )
        viewModelScope.launch {
            settings.updatePlayback { it.copy(defaultPitch = safe) }
        }
    }

    fun setDeviceVolume(normalized: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (normalized.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        _playbackState.update { it.copy(deviceVolume = readNormalizedVolume()) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
            val current = _playbackState.value.currentSong
            if (current != null && current.id == songId) {
                _playbackState.update { it.copy(currentSong = current.copy(isFavorite = !current.isFavorite)) }
            }
        }
    }

    fun hideSong(songId: Long, hidden: Boolean) {
        viewModelScope.launch { repository.setHidden(songId, hidden) }
    }

    fun unhideAllSongs() {
        viewModelScope.launch { repository.unhideAllSongs() }
    }

    fun excludeFolder(folderPath: String, folderName: String) {
        viewModelScope.launch {
            repository.excludeFolder(folderPath, folderName)
            scanLibrary()
        }
    }

    fun removeExcludedFolder(folderPath: String) {
        viewModelScope.launch {
            repository.removeExcludedFolder(folderPath)
            scanLibrary()
        }
    }

    fun addSafFolder(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            repository.addSafFolder(treeUri.toString(), displayName)
            scanLibrary()
        }
    }

    fun removeSafFolder(treeUri: String) {
        viewModelScope.launch {
            repository.removeSafFolder(treeUri)
            scanLibrary()
        }
    }

    fun observeSongsForPlaylist(playlist: Playlist): Flow<List<Song>> =
        repository.observeSongsForPlaylist(playlist.id, playlist.isSystemFavorites)

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, newName) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun addSongToPlaylists(playlistIds: List<Long>, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylists(playlistIds, songId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch { repository.reorderPlaylistSongs(playlistId, orderedSongIds) }
    }

    fun saveCustomLyrics(song: Song, rawText: String, sourceLabel: String = "Manual") {
        viewModelScope.launch {
            repository.saveCustomLyrics(song.contentUri, rawText, sourceLabel)
            val parsed = repository.loadLyricsForSong(song)
            if (_playbackState.value.currentSong?.id == song.id) {
                _playbackState.update { it.copy(lyrics = parsed) }
            }
        }
    }

    fun importLyricsFile(song: Song, uri: Uri) {
        viewModelScope.launch {
            val content = LyricsParser.readLyricsFromUri(app, uri) ?: return@launch
            saveCustomLyrics(song, content, "Imported .lrc")
        }
    }

    fun addSearchHistory(query: String) {
        viewModelScope.launch { settings.addSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { settings.clearSearchHistory() }
    }

    // Equalizer & Audio Effects updates
    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.updateAudioEffects { it.copy(eqEnabled = enabled) }
        }
    }

    fun selectEqPreset(preset: EqualizerPresetType) {
        viewModelScope.launch {
            settings.updateAudioEffects {
                it.copy(
                    eqEnabled = true,
                    presetName = preset.name,
                    customBandLevelsMb = if (preset == EqualizerPresetType.CUSTOM) it.customBandLevelsMb else preset.bandMillibels
                )
            }
        }
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        viewModelScope.launch {
            settings.updateAudioEffects { current ->
                val list = current.customBandLevelsMb.toMutableList()
                while (list.size <= bandIndex) list.add(0)
                list[bandIndex] = levelMb
                current.copy(
                    eqEnabled = true,
                    presetName = EqualizerPresetType.CUSTOM.name,
                    customBandLevelsMb = list
                )
            }
        }
    }

    fun saveCustomEqPreset(name: String) {
        viewModelScope.launch {
            val current = audioEffectSettings.value
            repository.saveCustomEqPreset(
                name = name,
                bands = current.customBandLevelsMb,
                bass = current.bassBoostStrength,
                virtualizer = current.virtualizerStrength
            )
        }
    }

    fun applyCustomEqPreset(preset: CustomEqPresetEntity) {
        val bands = preset.bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        viewModelScope.launch {
            settings.updateAudioEffects {
                it.copy(
                    eqEnabled = true,
                    presetName = EqualizerPresetType.CUSTOM.name,
                    customBandLevelsMb = if (bands.isNotEmpty()) bands else it.customBandLevelsMb,
                    bassBoostStrength = preset.bassBoostStrength,
                    virtualizerStrength = preset.virtualizerStrength
                )
            }
        }
    }

    fun deleteCustomEqPreset(name: String) {
        viewModelScope.launch { repository.deleteCustomEqPreset(name) }
    }

    fun updateAudioEffects(transform: (AudioEffectSettings) -> AudioEffectSettings) {
        viewModelScope.launch { settings.updateAudioEffects(transform) }
    }

    fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { settings.updateAppearance(transform) }
    }

    fun updatePlaybackSettings(transform: (PlaybackSettings) -> PlaybackSettings) {
        viewModelScope.launch { settings.updatePlayback(transform) }
    }

    fun updateLibrarySettings(transform: (LibrarySettings) -> LibrarySettings) {
        viewModelScope.launch { settings.updateLibrary(transform) }
    }

    fun updateNotificationSettings(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { settings.updateNotifications(transform) }
    }

    // Sleep Timer controls
    fun startSleepTimerMinutes(minutes: Int, fadeOut: Boolean) {
        container.sleepTimerManager.startMinutesTimer(minutes, fadeOut)
    }

    fun startSleepTimerAtClock(hour24: Int, minute: Int, fadeOut: Boolean) {
        container.sleepTimerManager.startStopAtClockTime(hour24, minute, fadeOut)
    }

    fun startSleepTimerEndOfTrack(fadeOut: Boolean) {
        container.sleepTimerManager.startStopAfterCurrentSong(fadeOut)
    }

    fun cancelSleepTimer() {
        container.sleepTimerManager.cancel()
    }

    fun refreshCacheSize() {
        viewModelScope.launch { _cacheBytes.value = repository.getCacheSizeBytes() }
    }

    fun clearArtworkAndTempCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshCacheSize()
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch { settings.resetAllSettings() }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun importBackupJson(rawJson: String): Result<Int> = repository.importBackupJson(rawJson)

    private fun readNormalizedVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return cur.toFloat() / max.toFloat()
    }

    private fun mapRepeatMode(mode: Int): RepeatModeOption = when (mode) {
        Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
        Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
        else -> RepeatModeOption.OFF
    }

    private fun sortSongs(songs: List<Song>, order: SongSortOrder, ascending: Boolean): List<Song> {
        val sorted = when (order) {
            SongSortOrder.ALPHABETICAL -> songs.sortedBy { it.title.lowercase() }
            SongSortOrder.RECENTLY_ADDED -> songs.sortedBy { it.dateAddedSec }
            SongSortOrder.RECENTLY_PLAYED -> songs.sortedBy { it.lastPlayedAt }
            SongSortOrder.MOST_PLAYED -> songs.sortedBy { it.playCount }
            SongSortOrder.DURATION -> songs.sortedBy { it.durationMs }
            SongSortOrder.FILE_SIZE -> songs.sortedBy { it.sizeBytes }
        }
        return if (ascending) sorted else sorted.reversed()
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toFallbackSong(): Song {
        val meta = mediaMetadata
        return Song(
            id = mediaId.toLongOrNull() ?: -1L,
            mediaStoreId = -1L,
            contentUri = localConfiguration?.uri?.toString().orEmpty(),
            title = meta.title?.toString() ?: "Audio Track",
            artist = meta.artist?.toString() ?: "Unknown Artist",
            album = meta.albumTitle?.toString() ?: "Single",
            albumId = -1L,
            genre = "Uncategorized",
            durationMs = 0L,
            sizeBytes = 0L,
            dateAddedSec = 0L,
            trackNumber = 0,
            year = 0,
            filePath = "",
            folderName = "External",
            folderPath = "",
            mimeType = "audio/*",
            artworkUri = meta.artworkUri?.toString()
        )
    }

    override fun onCleared() {
        positionTrackerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
}
