package moe.memesta.vibeon.ui.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberResolvedAlbumMainColor(
    context: Context,
    albumName: String,
    artistName: String,
    coverUrl: String?,
    storedColor: Int?,
    onPersistColor: (Int) -> Unit
): Int? {
    val persistCallback by rememberUpdatedState(onPersistColor)
    var resolvedColor by remember(albumName, artistName, coverUrl, storedColor) {
        mutableStateOf(storedColor)
    }

    LaunchedEffect(albumName, artistName, coverUrl, storedColor) {
        if (resolvedColor != null) return@LaunchedEffect
        val sourceUrl = coverUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val cacheKey = "$albumName|$artistName|$sourceUrl"

        val cached = AlbumArtColorCache.get(cacheKey)?.toArgb()
        val computedColor = cached ?: withContext(Dispatchers.IO) {
            resolveAlbumMainColorFromCover(context, sourceUrl)
        }

        if (computedColor != null) {
            resolvedColor = computedColor
            AlbumArtColorCache.put(cacheKey, Color(computedColor))
            persistCallback(computedColor)
        }
    }

    return resolvedColor
}

private suspend fun resolveAlbumMainColorFromCover(
    context: Context,
    coverUrl: String
): Int? = withContext(Dispatchers.IO) {
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()

        val result = context.imageLoader.execute(request)
        val drawable = result.drawable ?: return@runCatching null
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> drawable.toBitmap()
        }

        val palette = Palette.from(bitmap).generate()
        val rgb = palette.dominantSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb

        rgb?.let { 0xFF000000.toInt() or (it and 0x00FFFFFF) }
    }.getOrNull()
}
