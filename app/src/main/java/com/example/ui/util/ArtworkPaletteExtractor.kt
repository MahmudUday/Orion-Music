package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicArtworkColors(
    val primary: Color,
    val secondary: Color
)

object ArtworkPaletteExtractor {
    private val cache = LruCache<String, DynamicArtworkColors>(64)

    suspend fun extractColors(
        context: Context,
        artworkUri: String?,
        contentUri: String?
    ): DynamicArtworkColors? = withContext(Dispatchers.IO) {
        val key = artworkUri ?: contentUri ?: return@withContext null
        cache.get(key)?.let { return@withContext it }

        val bitmap = loadSmallBitmap(context, artworkUri, contentUri) ?: return@withContext null
        val palette = runCatching { Palette.from(bitmap).maximumColorCount(16).generate() }.getOrNull()
        bitmap.recycle()
        if (palette == null) return@withContext null

        val primaryInt = palette.getVibrantColor(
            palette.getLightVibrantColor(
                palette.getDominantColor(0xFF2979FF.toInt())
            )
        )
        val secondaryInt = palette.getDarkVibrantColor(
            palette.getMutedColor(0xFF7C4DFF.toInt())
        )

        val result = DynamicArtworkColors(
            primary = boostSaturationForDarkSurface(Color(primaryInt)),
            secondary = boostSaturationForDarkSurface(Color(secondaryInt))
        )
        cache.put(key, result)
        result
    }

    private fun loadSmallBitmap(
        context: Context,
        artworkUri: String?,
        contentUri: String?
    ): Bitmap? {
        if (!artworkUri.isNullOrBlank()) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    return BitmapFactory.decodeStream(stream, null, opts)
                }
            }
        }
        if (!contentUri.isNullOrBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(contentUri))
                val art = retriever.embeddedPicture
                if (art != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    return BitmapFactory.decodeByteArray(art, 0, art.size, opts)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
        return null
    }

    private fun boostSaturationForDarkSurface(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsv
        )
        hsv[1] = hsv[1].coerceAtLeast(0.55f)
        hsv[2] = hsv[2].coerceIn(0.68f, 0.96f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
