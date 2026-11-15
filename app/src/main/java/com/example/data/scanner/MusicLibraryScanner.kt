package com.example.data.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.OrionDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

data class ScanSummary(
    val totalIndexed: Int,
    val newlyAdded: Int,
    val removedCount: Int,
    val scanDurationMs: Long
)

class MusicLibraryScanner(
    private val context: Context,
    private val database: OrionDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val supportedExtensions = setOf(
        "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "aiff", "alac", "amr", "3ga"
    )

    fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanLibrary(): ScanSummary = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val librarySettings = settingsDataStore.libraryFlow.first()
        val minDurationMs = librarySettings.minDurationSeconds * 1000L
        val excludedPaths = database.folderRuleDao().getExcludedFolderPathsSnapshot()
        val safFolders = database.folderRuleDao().getSafFoldersSnapshot()
        val existingSongs = database.songDao().getAllSongsSnapshot()
        val existingByUri = existingSongs.associateBy { it.contentUri }

        val discovered = mutableListOf<SongEntity>()
        val seenFingerprints = HashSet<String>()
        val seenUris = HashSet<String>()

        if (hasAudioPermission()) {
            val mediaStoreSongs = queryMediaStore(minDurationMs, excludedPaths)
            for (song in mediaStoreSongs) {
                if (librarySettings.duplicateDetection) {
                    val fingerprint = "${song.title.lowercase()}|${song.artist.lowercase()}|${song.durationMs / 1000}|${song.sizeBytes}"
                    if (!seenFingerprints.add(fingerprint)) continue
                }
                if (seenUris.add(song.contentUri)) {
                    discovered.add(song)
                }
            }
        }

        // Scan user-selected Storage Access Framework (SAF) folders
        for (saf in safFolders) {
            val treeUri = runCatching { Uri.parse(saf.treeUri) }.getOrNull() ?: continue
            val rootDoc = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: continue
            scanSafDirectory(
                directory = rootDoc,
                folderLabel = saf.displayName,
                folderPath = saf.treeUri,
                minDurationMs = minDurationMs,
                excludedPaths = excludedPaths,
                existingByUri = existingByUri,
                seenUris = seenUris,
                seenFingerprints = seenFingerprints,
                duplicateDetection = librarySettings.duplicateDetection,
                output = discovered
            )
        }

        val toInsert = mutableListOf<SongEntity>()
        val toUpdate = mutableListOf<SongEntity>()

        for (fresh in discovered) {
            val old = existingByUri[fresh.contentUri]
            if (old == null) {
                toInsert.add(fresh)
            } else {
                val merged = fresh.copy(
                    id = old.id,
                    isFavorite = old.isFavorite,
                    isHidden = old.isHidden,
                    playCount = old.playCount,
                    lastPlayedAt = old.lastPlayedAt,
                    bitrateKbps = if (fresh.bitrateKbps > 0) fresh.bitrateKbps else old.bitrateKbps,
                    sampleRateHz = if (fresh.sampleRateHz > 0) fresh.sampleRateHz else old.sampleRateHz
                )
                if (merged != old) {
                    toUpdate.add(merged)
                }
            }
        }

        val discoveredUriSet = discovered.mapTo(HashSet()) { it.contentUri }
        val removedIds = existingSongs
            .filter { it.contentUri !in discoveredUriSet }
            .map { it.id }

        if (toInsert.isNotEmpty()) {
            database.songDao().insertAll(toInsert)
        }
        if (toUpdate.isNotEmpty()) {
            database.songDao().updateAll(toUpdate)
        }
        if (removedIds.isNotEmpty()) {
            removedIds.chunked(500).forEach { chunk ->
                database.songDao().deleteByIds(chunk)
            }
        }

        settingsDataStore.setLastScanTime(System.currentTimeMillis())

        ScanSummary(
            totalIndexed = discovered.size,
            newlyAdded = toInsert.size,
            removedCount = removedIds.size,
            scanDurationMs = System.currentTimeMillis() - startTime
        )
    }

    private fun queryMediaStore(
        minDurationMs: Long,
        excludedPaths: List<String>
    ): List<SongEntity> {
        val results = mutableListOf<SongEntity>()
        val genreMap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            loadLegacyGenreMap()
        } else {
            emptyMap()
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.GENRE)
        }

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val cursor: Cursor? = runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projectionList.toTypedArray(),
                selection,
                selectionArgs,
                sortOrder
            )
        }.getOrNull()

        cursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dataIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val genreIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else {
                -1
            }

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val filePath = c.getString(dataIdx).orEmpty()
                val mimeType = c.getString(mimeIdx).orEmpty()

                val ext = filePath.substringAfterLast('.', "").lowercase()
                if (ext.isNotEmpty() && ext !in supportedExtensions && !mimeType.startsWith("audio/")) {
                    continue
                }

                val folderPath = if (filePath.isNotBlank()) {
                    File(filePath).parent ?: "/Storage"
                } else {
                    "/Storage"
                }

                if (excludedPaths.any { excluded -> folderPath.startsWith(excluded, ignoreCase = true) }) {
                    continue
                }

                val folderName = File(folderPath).name.ifBlank { "Internal Storage" }
                val rawTitle = c.getString(titleIdx).orEmpty().ifBlank {
                    File(filePath).nameWithoutExtension.ifBlank { "Unknown Track" }
                }
                val rawArtist = c.getString(artistIdx).orEmpty().let {
                    if (it.isBlank() || it == "<unknown>") "Unknown Artist" else it
                }
                val rawAlbum = c.getString(albumIdx).orEmpty().let {
                    if (it.isBlank() || it == "<unknown>") folderName else it
                }
                val albumId = c.getLong(albumIdIdx)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val artworkUri = if (albumId > 0L) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()
                } else {
                    null
                }

                val genre = when {
                    genreIdx >= 0 -> c.getString(genreIdx).orEmpty().ifBlank { "Uncategorized" }
                    else -> genreMap[id] ?: inferGenreFromPathOrMime(filePath, mimeType)
                }

                results.add(
                    SongEntity(
                        mediaStoreId = id,
                        contentUri = contentUri,
                        title = rawTitle,
                        artist = rawArtist,
                        album = rawAlbum,
                        albumId = albumId,
                        genre = genre,
                        durationMs = c.getLong(durationIdx).coerceAtLeast(0L),
                        sizeBytes = c.getLong(sizeIdx).coerceAtLeast(0L),
                        dateAddedSec = c.getLong(dateAddedIdx).coerceAtLeast(0L),
                        trackNumber = c.getInt(trackIdx),
                        year = c.getInt(yearIdx),
                        filePath = filePath,
                        folderName = folderName,
                        folderPath = folderPath,
                        mimeType = mimeType.ifBlank { "audio/*" },
                        artworkUri = artworkUri,
                        isSafDocument = false
                    )
                )
            }
        }
        return results
    }

    private fun loadLegacyGenreMap(): Map<Long, String> {
        val map = HashMap<Long, String>()
        val genreProjection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                genreProjection,
                null,
                null,
                null
            )?.use { genresCursor ->
                val gIdIdx = genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val gNameIdx = genresCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genresCursor.moveToNext()) {
                    val genreId = genresCursor.getLong(gIdIdx)
                    val genreName = genresCursor.getString(gNameIdx).orEmpty().trim()
                    if (genreName.isBlank()) continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { membersCursor ->
                        val audioIdIdx = membersCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (membersCursor.moveToNext()) {
                            map.putIfAbsent(membersCursor.getLong(audioIdIdx), genreName)
                        }
                    }
                }
            }
        }
        return map
    }

    private fun scanSafDirectory(
        directory: DocumentFile,
        folderLabel: String,
        folderPath: String,
        minDurationMs: Long,
        excludedPaths: List<String>,
        existingByUri: Map<String, SongEntity>,
        seenUris: MutableSet<String>,
        seenFingerprints: MutableSet<String>,
        duplicateDetection: Boolean,
        output: MutableList<SongEntity>
    ) {
        if (excludedPaths.any { it.equals(folderPath, ignoreCase = true) }) return

        val children = runCatching { directory.listFiles() }.getOrNull() ?: return
        for (file in children) {
            if (file.isDirectory) {
                val subName = file.name ?: folderLabel
                scanSafDirectory(
                    directory = file,
                    folderLabel = subName,
                    folderPath = file.uri.toString(),
                    minDurationMs = minDurationMs,
                    excludedPaths = excludedPaths,
                    existingByUri = existingByUri,
                    seenUris = seenUris,
                    seenFingerprints = seenFingerprints,
                    duplicateDetection = duplicateDetection,
                    output = output
                )
            } else if (file.isFile) {
                val name = file.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = file.type.orEmpty()
                if (ext !in supportedExtensions && !mime.startsWith("audio/")) continue

                val uriString = file.uri.toString()
                if (uriString in seenUris) continue

                val existing = existingByUri[uriString]
                val songEntity = if (existing != null && existing.sizeBytes == file.length()) {
                    existing
                } else {
                    extractSafMetadata(file, folderLabel, folderPath)
                } ?: continue

                if (songEntity.durationMs < minDurationMs) continue
                if (duplicateDetection) {
                    val fingerprint = "${songEntity.title.lowercase()}|${songEntity.artist.lowercase()}|${songEntity.durationMs / 1000}|${songEntity.sizeBytes}"
                    if (!seenFingerprints.add(fingerprint)) continue
                }

                if (seenUris.add(uriString)) {
                    output.add(songEntity)
                }
            }
        }
    }

    private fun extractSafMetadata(
        file: DocumentFile,
        folderLabel: String,
        folderPath: String
    ): SongEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file.uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: file.name?.substringBeforeLast('.')
                ?: "Unknown Track"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: folderLabel
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }
                ?: "Uncategorized"
            val bitrate = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0
            val track = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 0

            SongEntity(
                mediaStoreId = file.uri.toString().hashCode().toLong(),
                contentUri = file.uri.toString(),
                title = title,
                artist = artist,
                album = album,
                albumId = album.hashCode().toLong(),
                genre = genre,
                durationMs = durationMs,
                sizeBytes = file.length(),
                dateAddedSec = file.lastModified() / 1000L,
                trackNumber = track,
                year = year,
                filePath = file.name ?: file.uri.toString(),
                folderName = folderLabel,
                folderPath = folderPath,
                mimeType = file.type ?: "audio/*",
                bitrateKbps = bitrate,
                artworkUri = file.uri.toString(),
                isSafDocument = true
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractAudioTechnicalDetails(contentUri: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(contentUri))
            val bitrateKbps = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
            val sampleRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
            } else {
                44100
            }
            bitrateKbps to sampleRateHz
        } catch (_: Exception) {
            0 to 0
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun inferGenreFromPathOrMime(path: String, mime: String): String {
        val lower = path.lowercase()
        return when {
            "whatsapp" in lower || "telegram" in lower -> "Voice & Chats"
            "record" in lower -> "Recordings"
            "podcast" in lower -> "Podcasts"
            "flac" in mime || lower.endsWith(".flac") -> "Hi-Res Audio"
            else -> "Uncategorized"
        }
    }
}
