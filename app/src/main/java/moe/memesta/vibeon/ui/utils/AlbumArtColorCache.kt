package moe.memesta.vibeon.ui.utils

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Bounded cache for dominant colors derived from album or artist artwork.
 */
object AlbumArtColorCache {
    private const val MAX_ENTRIES = 64

    private val cache = object : LruCache<String, Int>(MAX_ENTRIES) {}

    fun get(key: String): Color? = cache.get(key)?.let(::Color)

    fun put(key: String, color: Color) {
        cache.put(key, color.toArgb())
    }

    fun clear() {
        cache.evictAll()
    }
}