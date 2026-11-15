package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["contentUri"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["artist"]),
        Index(value = ["folderPath"]),
        Index(value = ["isHidden"]),
        Index(value = ["isFavorite"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaStoreId: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val filePath: String,
    val folderName: String,
    val folderPath: String,
    val mimeType: String,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val artworkUri: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isSafDocument: Boolean = false
) {
    fun toDomain(): Song = Song(
        id = id,
        mediaStoreId = mediaStoreId,
        contentUri = contentUri,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        genre = genre,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        dateAddedSec = dateAddedSec,
        trackNumber = trackNumber,
        year = year,
        filePath = filePath,
        folderName = folderName,
        folderPath = folderPath,
        mimeType = mimeType,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        artworkUri = artworkUri,
        isFavorite = isFavorite,
        isHidden = isHidden,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        isSafDocument = isSafDocument
    )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSystemFavorites: Boolean = false
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "position"]),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val excludedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_lyrics")
data class CustomLyricsEntity(
    @PrimaryKey val songUri: String,
    val rawLyrics: String,
    val sourceLabel: String = "Manual",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val name: String,
    val bandLevelsCsv: String,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
