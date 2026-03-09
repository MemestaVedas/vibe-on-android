package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import moe.memesta.vibeon.MainActivity
import moe.memesta.vibeon.R

private val keyAction = ActionParameters.Key<String>("action")
private const val ACT_PLAY_PAUSE = "play_pause"
private const val ACT_PREV       = "prev"
private const val ACT_NEXT       = "next"
private const val ACT_TOGGLE_OUT = "toggle_output"
private const val ACT_LIKE       = "like"

/**
 * Main Vibe-on widget displaying current track and playback controls.
 */
class VibeonWidget : GlanceAppWidget() {
    
    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<WidgetPlaybackState>()
            GlanceTheme {
                WidgetContent(playerInfo = playerInfo)
            }
        }
    }
}

/**
 * LRU cache for decoded album art bitmaps to avoid repeated decoding.
 */
private object AlbumArtCache {
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024 // 4 MiB
    
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = lruCache.get(key)
    
    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            lruCache.put(key, bitmap)
        }
    }
}

@Composable
private fun WidgetContent(playerInfo: WidgetPlaybackState) {
    val albumBitmap = playerInfo.albumArtBitmapData?.let { data ->
        try {
            val cacheKey = data.contentHashCode().toString()
            var bitmap = AlbumArtCache.get(cacheKey)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.let { AlbumArtCache.put(cacheKey, it) }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    val primaryColor = Color(playerInfo.colorPrimary)
    val onPrimaryColor = Color(playerInfo.colorOnPrimary)
    val secondaryColor = Color(playerInfo.colorSecondary)
    val secondaryContainer = Color(playerInfo.colorSecondaryContainer)
    val errorContainer = Color(playerInfo.colorErrorContainer)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(primaryColor.copy(alpha = 0.15f)))
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Album Art
            Box(
                modifier = GlanceModifier
                    .size(120.dp)
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (albumBitmap != null) {
                    Image(
                        provider = ImageProvider(albumBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(16.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(16.dp)
                            .background(ColorProvider(primaryColor.copy(alpha = 0.4f))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.finalmono),
                            contentDescription = "No Album Art",
                            modifier = GlanceModifier.size(60.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor))
                        )
                    }
                }
            }

            // Song Info
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerInfo.title.ifEmpty { "No Track Playing" },
                    style = TextStyle(
                        color = ColorProvider(onPrimaryColor),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = playerInfo.artist.ifEmpty { "Unknown Artist" },
                    style = TextStyle(
                        color = ColorProvider(secondaryColor),
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Playback Controls - Previous, Play/Pause, Next
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(48.dp)
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PREV)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_music_note),
                        contentDescription = "Previous",
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor))
                    )
                }

                // Play/Pause
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(48.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(secondaryContainer))
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PLAY_PAUSE)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_play_star),
                        contentDescription = if (playerInfo.isPlaying) "Pause" else "Play",
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(playerInfo.colorOnSecondaryContainer)))
                    )
                }

                // Next
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(48.dp)
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_NEXT)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_music_note),
                        contentDescription = "Next",
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Status Row - Shuffle, Like, Output
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                if (playerInfo.isShuffled) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_shuffle),
                        contentDescription = "Shuffle",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(secondaryColor))
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Like button
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_LIKE)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = if (playerInfo.isLiked)
                            ImageProvider(R.drawable.ic_widget_heart_filled)
                        else
                            ImageProvider(R.drawable.ic_widget_heart_outline),
                        contentDescription = if (playerInfo.isLiked) "Unlike" else "Like",
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                if (playerInfo.isLiked) errorContainer else secondaryColor
                            )
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Output toggle
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .cornerRadius(22.dp)
                        .background(ColorProvider(secondaryContainer))
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_TOGGLE_OUT)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = if (playerInfo.isMobilePlayback)
                            ImageProvider(R.drawable.ic_widget_phone)
                        else
                            ImageProvider(R.drawable.ic_widget_computer),
                        contentDescription = if (playerInfo.isMobilePlayback) "Mobile" else "PC",
                        modifier = GlanceModifier.size(22.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(playerInfo.colorOnSecondaryContainer)))
                    )
                }
            }
        }
    }
}

