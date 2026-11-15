package com.example.audio

import android.content.Context
import android.net.Uri
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

object LyricsParser {

    private val lrcTimeRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parseRawLyrics(songId: Long, raw: String, sourceLabel: String): LyricsData {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return LyricsData(
                songId = songId,
                lines = emptyList(),
                isSynced = false,
                rawText = "",
                sourceLabel = sourceLabel
            )
        }

        val syncedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<LyricLine>()

        for (line in trimmed.lines()) {
            val matches = lrcTimeRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                val lyricText = lrcTimeRegex.replace(line, "").trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionRaw = match.groupValues[3]
                    val millis = when (fractionRaw.length) {
                        0 -> 0L
                        1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                        2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                        else -> (fractionRaw.take(3).toLongOrNull() ?: 0L)
                    }
                    val totalMs = minutes * 60_000L + seconds * 1_000L + millis
                    syncedLines.add(LyricLine(timestampMs = totalMs, text = lyricText.ifBlank { "•••" }))
                }
            } else {
                // Skip LRC metadata tags like [ti:...], [ar:...], [al:...], [offset:...]
                val isTagLine = Regex("""^\[[a-zA-Z]+:.*]$""").matches(line.trim())
                if (!isTagLine) {
                    plainLines.add(LyricLine(timestampMs = null, text = line))
                }
            }
        }

        return if (syncedLines.isNotEmpty()) {
            LyricsData(
                songId = songId,
                lines = syncedLines.sortedBy { it.timestampMs ?: 0L },
                isSynced = true,
                rawText = trimmed,
                sourceLabel = sourceLabel
            )
        } else {
            LyricsData(
                songId = songId,
                lines = plainLines,
                isSynced = false,
                rawText = trimmed,
                sourceLabel = sourceLabel
            )
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            val ts = lines[i].timestampMs ?: continue
            if (ts <= positionMs) {
                active = i
            } else {
                break
            }
        }
        return active
    }

    suspend fun loadLyricsForSong(
        context: Context,
        song: Song,
        customSavedLyrics: String?,
        customSourceLabel: String?
    ): LyricsData = withContext(Dispatchers.IO) {
        if (!customSavedLyrics.isNullOrBlank()) {
            return@withContext parseRawLyrics(
                songId = song.id,
                raw = customSavedLyrics,
                sourceLabel = customSourceLabel ?: "Custom"
            )
        }

        // 1. Check sidecar .lrc or .txt file next to audio file
        if (song.filePath.isNotBlank()) {
            val audioFile = File(song.filePath)
            val baseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile
            if (parent != null && parent.exists() && parent.canRead()) {
                val lrcFile = File(parent, "$baseName.lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    val content = runCatching { lrcFile.readText(Charsets.UTF_8) }.getOrNull()
                    if (!content.isNullOrBlank()) {
                        return@withContext parseRawLyrics(song.id, content, "Local .lrc File")
                    }
                }
                val txtFile = File(parent, "$baseName.txt")
                if (txtFile.exists() && txtFile.canRead()) {
                    val content = runCatching { txtFile.readText(Charsets.UTF_8) }.getOrNull()
                    if (!content.isNullOrBlank()) {
                        return@withContext parseRawLyrics(song.id, content, "Local .txt File")
                    }
                }
            }
        }

        // 2. Attempt embedded ID3v2 USLT lyrics extraction directly from local file header
        if (song.filePath.isNotBlank()) {
            val embedded = extractEmbeddedId3Lyrics(File(song.filePath))
            if (!embedded.isNullOrBlank()) {
                return@withContext parseRawLyrics(song.id, embedded, "Embedded ID3 Tags")
            }
        }

        LyricsData(
            songId = song.id,
            lines = emptyList(),
            isSynced = false,
            rawText = "",
            sourceLabel = "None"
        )
    }

    suspend fun readLyricsFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        }.getOrNull()
    }

    private fun extractEmbeddedId3Lyrics(file: File): String? {
        if (!file.exists() || !file.canRead() || file.length() < 64) return null
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(10)
                if (input.read(header) != 10) return null
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                    return null
                }
                val versionMajor = header[3].toInt() and 0xFF
                val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)
                val maxRead = tagSize.coerceAtMost(512 * 1024)
                val tagBytes = ByteArray(maxRead)
                val readCount = input.read(tagBytes)
                if (readCount <= 10) return null

                var offset = 0
                while (offset + 10 <= readCount) {
                    val frameId = String(tagBytes, offset, 4, Charsets.ISO_8859_1)
                    if (frameId.all { it == '\u0000' }) break
                    val frameSize = if (versionMajor >= 4) {
                        ((tagBytes[offset + 4].toInt() and 0x7F) shl 21) or
                            ((tagBytes[offset + 5].toInt() and 0x7F) shl 14) or
                            ((tagBytes[offset + 6].toInt() and 0x7F) shl 7) or
                            (tagBytes[offset + 7].toInt() and 0x7F)
                    } else {
                        ((tagBytes[offset + 4].toInt() and 0xFF) shl 24) or
                            ((tagBytes[offset + 5].toInt() and 0xFF) shl 16) or
                            ((tagBytes[offset + 6].toInt() and 0xFF) shl 8) or
                            (tagBytes[offset + 7].toInt() and 0xFF)
                    }
                    offset += 10
                    if (frameSize <= 0 || offset + frameSize > readCount) break
                    if (frameId == "USLT") {
                        return parseUsltFrame(tagBytes, offset, frameSize)
                    }
                    offset += frameSize
                }
                null
            }
        }.getOrNull()
    }

    private fun parseUsltFrame(data: ByteArray, start: Int, length: Int): String? {
        if (length < 5) return null
        val encoding = data[start].toInt() and 0xFF
        val charset: Charset = when (encoding) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        // Skip encoding (1 byte) + language (3 bytes) + content descriptor up to null terminator
        var pos = start + 4
        val end = start + length
        if (encoding == 1 || encoding == 2) {
            while (pos + 1 < end) {
                if (data[pos] == 0.toByte() && data[pos + 1] == 0.toByte()) {
                    pos += 2
                    break
                }
                pos += 2
            }
        } else {
            while (pos < end) {
                if (data[pos] == 0.toByte()) {
                    pos += 1
                    break
                }
                pos += 1
            }
        }
        if (pos >= end) return null
        return String(data, pos, end - pos, charset).trim().takeIf { it.isNotBlank() }
    }
}
