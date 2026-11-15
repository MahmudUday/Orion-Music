package com.example.ui.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {
    fun formatDuration(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(safeMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatTotalHoursMinutes(ms: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(ms.coerceAtLeast(0L))
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatFrequency(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f == 0f) "${khz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", khz)
        } else {
            "$hz Hz"
        }
    }
}
