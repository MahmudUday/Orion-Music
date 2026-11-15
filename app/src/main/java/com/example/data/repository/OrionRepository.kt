package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.audio.LyricsParser
import com.example.data.local.CustomEqPresetEntity
import com.example.data.local.CustomLyricsEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.OrionDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.ResumePlaybackSnapshot
import com.example.data.local.SafFolderEntity
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppearanceSettings
import com.example.data.model.AudioEffectSettings
import com.example.data.model.BackupPlaylist
import com.example.data.model.LibrarySettings
import com.example.data.model.LyricsData
import com.example.data.model.NotificationSettings
import com.example.data.model.OrionBackupPayload
import com.example.data.model.PlaybackSettings
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.scanner.LibraryScanWorker
import com.example.data.scanner.MusicLibraryScanner
import com.example.data.scanner.ScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class OrionRepository(
    private val context: Context,
    val database: OrionDatabase,
    val settingsDataStore: SettingsDataStore,
    val scanner: MusicLibraryScanner
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val visibleSongsFlow: Flow<List<Song>> = database.songDao().observeVisibleSongs()
        .map { list -> list.map { it.toDomain() } }

    val hiddenSongsFlow: Flow<List<Song>> = database.songDao().observeHiddenSongs()
        .map { list -> list.map { it.toDomain() } }

    val favoritesFlow: Flow<List<Song>> = database.songDao().observeFavorites()
        .map { list -> list.map { it.toDomain() } }

    val recentlyPlayedFlow: Flow<List<Song>> = database.songDao().observeRecentlyPlayed()
        .map { list -> list.map { it.toDomain() } }

    val recentlyAddedFlow: Flow<List<Song>> = database.songDao().observeRecentlyAdded()
        .map { list -> list.map { it.toDomain() } }

    val mostPlayedFlow: Flow<List<Song>> = database.songDao().observeMostPlayed()
        .map { list -> list.map { it.toDomain() } }

    val playlistsFlow: Flow<List<Playlist>> = combine(
        database.playlistDao().observePlaylists(),
        database.playlistDao().observeCrossRefsOrdered(),
        visibleSongsFlow
    ) { playlists, crossRefs, songs ->
        val songById = songs.associateBy { it.id }
        val refsByPlaylist = crossRefs.groupBy { it.playlistId }
        playlists.map { entity ->
            if (entity.isSystemFavorites) {
                val favSongs = songs.filter { it.isFavorite }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    isSystemFavorites = true,
                    songCount = favSongs.size,
                    artworkUri = favSongs.firstOrNull { it.artworkUri != null }?.artworkUri
                )
            } else {
                val plSongs = refsByPlaylist[entity.id].orEmpty().mapNotNull { songById[it.songId] }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    isSystemFavorites = false,
                    songCount = plSongs.size,
                    artworkUri = plSongs.firstOrNull { it.artworkUri != null }?.artworkUri
                )
            }
        }
    }

    val excludedFoldersFlow: Flow<List<ExcludedFolderEntity>> = database.folderRuleDao().observeExcludedFolders()
    val safFoldersFlow: Flow<List<SafFolderEntity>> = database.folderRuleDao().observeSafFolders()
    val customEqPresetsFlow: Flow<List<CustomEqPresetEntity>> = database.customEqPresetDao().observeCustomPresets()

    suspend fun ensureFavoritesPlaylist() {
        if (database.playlistDao().getFavoritesPlaylist() == null) {
            database.playlistDao().insertPlaylist(
                PlaylistEntity(
                    name = "Favorites",
                    isSystemFavorites = true
                )
            )
        }
    }

    suspend fun scanLibraryNow(): ScanSummary {
        ensureFavoritesPlaylist()
        return scanner.scanLibrary()
    }

    fun scheduleBackgroundScan() {
        LibraryScanWorker.enqueueImmediateScan(context)
        LibraryScanWorker.schedulePeriodicScan(context)
    }

    suspend fun toggleFavorite(songId: Long) {
        val current = database.songDao().getSongById(songId) ?: return
        database.songDao().setFavorite(songId, !current.isFavorite)
    }

    suspend fun setHidden(songId: Long, hidden: Boolean) {
        database.songDao().setHidden(songId, hidden)
    }

    suspend fun unhideAllSongs() {
        database.songDao().unhideAllSongs()
    }

    suspend fun recordSongPlay(songId: Long) {
        database.songDao().recordPlay(songId, System.currentTimeMillis())
    }

    suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().ifBlank { "Untitled Playlist" }
        return database.playlistDao().insertPlaylist(
            PlaylistEntity(name = clean, isSystemFavorites = false)
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            database.playlistDao().renamePlaylist(playlistId, clean)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        database.playlistDao().deletePlaylist(playlistId)
    }

    fun observeSongsForPlaylist(playlistId: Long, isSystemFavorites: Boolean): Flow<List<Song>> {
        return if (isSystemFavorites) {
            favoritesFlow
        } else {
            database.playlistDao().observeSongsForPlaylist(playlistId).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val playlist = database.playlistDao().getPlaylistsSnapshot().find { it.id == playlistId }
        if (playlist?.isSystemFavorites == true) {
            database.songDao().setFavorite(songId, true)
            return
        }
        val nextPos = database.playlistDao().getMaxPosition(playlistId) + 1
        database.playlistDao().insertCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = nextPos)
        )
    }

    suspend fun addSongToPlaylists(playlistIds: List<Long>, songId: Long) {
        playlistIds.forEach { pid -> addSongToPlaylist(pid, songId) }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        database.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        database.playlistDao().replacePlaylistOrder(playlistId, orderedSongIds)
    }

    suspend fun excludeFolder(folderPath: String, folderName: String) {
        database.folderRuleDao().excludeFolder(
            ExcludedFolderEntity(folderPath = folderPath, folderName = folderName)
        )
    }

    suspend fun removeExcludedFolder(folderPath: String) {
        database.folderRuleDao().removeExcludedFolder(folderPath)
    }

    suspend fun addSafFolder(treeUri: String, displayName: String) {
        database.folderRuleDao().insertSafFolder(
            SafFolderEntity(treeUri = treeUri, displayName = displayName)
        )
    }

    suspend fun removeSafFolder(treeUri: String) {
        database.folderRuleDao().removeSafFolder(treeUri)
    }

    suspend fun loadLyricsForSong(song: Song): LyricsData {
        val custom = database.customLyricsDao().getLyricsForSong(song.contentUri)
        return LyricsParser.loadLyricsForSong(
            context = context,
            song = song,
            customSavedLyrics = custom?.rawLyrics,
            customSourceLabel = custom?.sourceLabel
        )
    }

    suspend fun saveCustomLyrics(songUri: String, rawText: String, sourceLabel: String) {
        database.customLyricsDao().saveLyrics(
            CustomLyricsEntity(songUri = songUri, rawLyrics = rawText, sourceLabel = sourceLabel)
        )
    }

    suspend fun clearCustomLyrics(songUri: String) {
        database.customLyricsDao().deleteLyrics(songUri)
    }

    suspend fun saveCustomEqPreset(name: String, bands: List<Int>, bass: Int, virtualizer: Int) {
        database.customEqPresetDao().savePreset(
            CustomEqPresetEntity(
                name = name.trim().ifBlank { "Custom EQ" },
                bandLevelsCsv = bands.joinToString(","),
                bassBoostStrength = bass,
                virtualizerStrength = virtualizer
            )
        )
    }

    suspend fun deleteCustomEqPreset(name: String) {
        database.customEqPresetDao().deletePreset(name)
    }

    suspend fun refreshSongTechInfo(song: Song): Song {
        if (song.bitrateKbps > 0 && song.sampleRateHz > 0) return song
        val (bitrate, sampleRate) = scanner.extractAudioTechnicalDetails(song.contentUri)
        if (bitrate > 0 || sampleRate > 0) {
            database.songDao().updateAudioTechInfo(song.id, bitrate, sampleRate)
            return song.copy(bitrateKbps = bitrate, sampleRateHz = sampleRate)
        }
        return song
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        fun dirSize(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        dirSize(context.cacheDir)
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            true
        }.getOrDefault(false)
    }

    suspend fun exportBackupJson(): String {
        val appearance = settingsDataStore.appearanceFlow.first()
        val playback = settingsDataStore.playbackFlow.first()
        val library = settingsDataStore.libraryFlow.first()
        val audioEffects = settingsDataStore.audioEffectsFlow.first()
        val notifications = settingsDataStore.notificationsFlow.first()
        val songs = database.songDao().getAllSongsSnapshot()
        val songUriById = songs.associate { it.id to it.contentUri }
        val playlists = database.playlistDao().getPlaylistsSnapshot().filterNot { it.isSystemFavorites }
        val backupPlaylists = playlists.map { pl ->
            val refs = database.playlistDao().getPlaylistCrossRefs(pl.id)
            BackupPlaylist(
                name = pl.name,
                createdAt = pl.createdAt,
                songUris = refs.mapNotNull { songUriById[it.songId] }
            )
        }
        val favUris = songs.filter { it.isFavorite }.map { it.contentUri }

        val payload = OrionBackupPayload(
            appearance = appearance,
            playback = playback,
            library = library,
            audioEffects = audioEffects,
            notifications = notifications,
            playlists = backupPlaylists,
            favoriteSongUris = favUris
        )
        return json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = runCatching {
        val payload = json.decodeFromString<OrionBackupPayload>(rawJson)
        settingsDataStore.updateAppearance { payload.appearance }
        settingsDataStore.updatePlayback { payload.playback }
        settingsDataStore.updateLibrary { payload.library }
        settingsDataStore.updateAudioEffects { payload.audioEffects }
        settingsDataStore.updateNotifications { payload.notifications }

        val songs = database.songDao().getAllSongsSnapshot()
        val songIdByUri = songs.associate { it.contentUri to it.id }

        payload.favoriteSongUris.forEach { uri ->
            songIdByUri[uri]?.let { id ->
                database.songDao().setFavorite(id, true)
            }
        }

        var restoredCount = 0
        for (bp in payload.playlists) {
            val playlistId = createPlaylist(bp.name)
            val orderedIds = bp.songUris.mapNotNull { songIdByUri[it] }
            if (orderedIds.isNotEmpty()) {
                database.playlistDao().replacePlaylistOrder(playlistId, orderedIds)
            }
            restoredCount++
        }
        restoredCount
    }

    suspend fun saveResumeSnapshot(snapshot: ResumePlaybackSnapshot) {
        settingsDataStore.saveResumeSnapshot(snapshot)
    }
}
