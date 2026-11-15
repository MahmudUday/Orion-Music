package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isHidden = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeVisibleSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isHidden = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeHiddenSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSnapshot(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isHidden = 0 AND isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isHidden = 0 AND lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 30")
    fun observeRecentlyPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isHidden = 0 ORDER BY dateAddedSec DESC LIMIT 30")
    fun observeRecentlyAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isHidden = 0 AND playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT 30")
    fun observeMostPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getSongByUri(contentUri: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun updateAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE songs SET isFavorite = :favorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, favorite: Boolean)

    @Query("UPDATE songs SET isHidden = :hidden WHERE id = :songId")
    suspend fun setHidden(songId: Long, hidden: Boolean)

    @Query("UPDATE songs SET isHidden = 0 WHERE isHidden = 1")
    suspend fun unhideAllSongs()

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun recordPlay(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET bitrateKbps = :bitrateKbps, sampleRateHz = :sampleRateHz WHERE id = :songId")
    suspend fun updateAudioTechInfo(songId: Long, bitrateKbps: Int, sampleRateHz: Int)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isSystemFavorites DESC, createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylistsSnapshot(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE isSystemFavorites = 1 LIMIT 1")
    suspend fun getFavoritesPlaylist(): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId AND isSystemFavorites = 0")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId AND isSystemFavorites = 0")
    suspend fun deletePlaylist(playlistId: Long)

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId AND s.isHidden = 0
        ORDER BY ps.position ASC
        """
    )
    fun observeSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistCrossRefs(playlistId: Long): List<PlaylistSongCrossRef>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Transaction
    suspend fun replacePlaylistOrder(playlistId: Long, orderedSongIds: List<Long>) {
        clearPlaylistSongs(playlistId)
        val refs = orderedSongIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = index
            )
        }
        if (refs.isNotEmpty()) {
            insertCrossRefs(refs)
        }
    }

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId ASC, position ASC")
    fun observeCrossRefsOrdered(): Flow<List<PlaylistSongCrossRef>>
}

@Dao
interface FolderRuleDao {
    @Query("SELECT * FROM excluded_folders ORDER BY folderName ASC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT folderPath FROM excluded_folders")
    suspend fun getExcludedFolderPathsSnapshot(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun excludeFolder(entity: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun removeExcludedFolder(folderPath: String)

    @Query("SELECT * FROM saf_folders ORDER BY addedAt DESC")
    fun observeSafFolders(): Flow<List<SafFolderEntity>>

    @Query("SELECT * FROM saf_folders")
    suspend fun getSafFoldersSnapshot(): List<SafFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafFolder(entity: SafFolderEntity)

    @Query("DELETE FROM saf_folders WHERE treeUri = :treeUri")
    suspend fun removeSafFolder(treeUri: String)
}

@Dao
interface CustomLyricsDao {
    @Query("SELECT * FROM custom_lyrics WHERE songUri = :songUri LIMIT 1")
    fun observeLyricsForSong(songUri: String): Flow<CustomLyricsEntity?>

    @Query("SELECT * FROM custom_lyrics WHERE songUri = :songUri LIMIT 1")
    suspend fun getLyricsForSong(songUri: String): CustomLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entity: CustomLyricsEntity)

    @Query("DELETE FROM custom_lyrics WHERE songUri = :songUri")
    suspend fun deleteLyrics(songUri: String)
}

@Dao
interface CustomEqPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY updatedAt DESC")
    fun observeCustomPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(entity: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE name = :name")
    suspend fun deletePreset(name: String)
}
