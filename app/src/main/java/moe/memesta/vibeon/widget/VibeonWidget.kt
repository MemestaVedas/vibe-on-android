package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.layout.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.action.*
import androidx.glance.state.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import moe.memesta.vibeon.MainActivity
import moe.memesta.vibeon.R

private val keyAction = ActionParameters.Key<String>("action")
private const val ACT_PLAY_PAUSE = "play_pause"
private const val ACT_PREV       = "prev"
private const val ACT_NEXT       = "next"
private const val ACT_TOGGLE_OUT = "toggle_output"
private const val ACT_LIKE       = "like"
private const val ACT_TOGGLE_MORE = "toggle_more_options"
private const val ACT_CLOSE_MORE  = "close_more_options"
private const val ACT_SHUFFLE     = "toggle_shuffle"
private const val ACT_REPEAT      = "cycle_repeat"
private const val ACT_VOLUME      = "cycle_volume"

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

private fun createBottomGradientBitmap(colorInt: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(1, 100, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    val transparent = colorInt and 0x00FFFFFF
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, 100f,
        intArrayOf(transparent, transparent, colorInt),
        floatArrayOf(0f, 0.56f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 1f, 100f, paint)
    return bitmap
}

@Composable
private fun WidgetContent(playerInfo: WidgetPlaybackState) {
    if (playerInfo.showingMoreOptions) {
        MoreDetailsContent(playerInfo)
    } else {
        MainWidgetContent(playerInfo)
    }
}

@Composable
private fun MainWidgetContent(playerInfo: WidgetPlaybackState) {
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
    val secondaryContainer = Color(playerInfo.colorSecondaryContainer)
    val onSecondaryContainer = Color(playerInfo.colorOnSecondaryContainer)
    val errorContainer = Color(playerInfo.colorErrorContainer)
    val onErrorContainer = Color(playerInfo.colorOnErrorContainer)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(Color.Black)) // Fallback behind image
    ) {
        // 1. Full-screen Album Art
        if (albumBitmap != null) {
            Image(
                provider = ImageProvider(albumBitmap),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
            
            // Dynamic Bottom Gradient overlay (start 67% -> 100%)
            Image(
                provider = ImageProvider(createBottomGradientBitmap(playerInfo.colorPrimary)),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            // Default background if no album art
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(primaryColor.copy(alpha = 0.4f))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.finalmono),
                    contentDescription = "No Album Art",
                    modifier = GlanceModifier.size(80.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor.copy(alpha = 0.5f)))
                )
            }
        }

        // 2. 3x3 Adaptive Tap Zones Grid
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // ROW 1: Top (Zone 1: Logo | Zone 3: Open App | Zone 2: Output Toggle)
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                // Top Left: Logo (Zone 1 - No Action specified but holds the logo)
                Box(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.finalmono),
                        contentDescription = "Vibe-on Logo",
                        colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor)),
                        modifier = GlanceModifier.size(36.dp)
                    )
                }
                
                // Top Middle: Tap Zone 3 - Open App
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {}

                // Top Right: Phone/PC toggle (Clickable independently to toggle output)
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .cornerRadius(18.dp)
                            .background(
                                ColorProvider(
                                    if (playerInfo.isMobilePlayback) onSecondaryContainer else secondaryContainer
                                )
                            )
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
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(
                                ColorProvider(
                                    if (playerInfo.isMobilePlayback) secondaryContainer else onSecondaryContainer
                                )
                            )
                        )
                    }
                }
            }

            // ROW 2: Middle (Zone 1: Prev | Zone 5: Play/Pause | Zone 2: Next)
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                // Tap Zone 1 - Prev Track
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PREV)
                            )
                        )
                ) {}

                // Tap Zone 5 - Play/Pause
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PLAY_PAUSE)
                            )
                        )
                ) {}

                // Tap Zone 2 - Next Track
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_NEXT)
                            )
                        )
                ) {}
            }

            // ROW 3: Bottom (Text Info | Zone 4: More options | Like Button)
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                // Visual Layer
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    // Song Title & Artist
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(start = 16.dp, bottom = 16.dp, end = 8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = playerInfo.title.ifEmpty { "No Track Playing" },
                                style = TextStyle(
                                    color = ColorProvider(onPrimaryColor),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = playerInfo.artist.ifEmpty { "Unknown Artist" },
                                style = TextStyle(
                                    color = ColorProvider(onPrimaryColor.copy(alpha = 0.8f)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Like Button
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .cornerRadius(18.dp)
                                .background(
                                    ColorProvider(
                                        if (playerInfo.isLiked) errorContainer else secondaryContainer
                                    )
                                )
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
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(
                                    ColorProvider(
                                        if (playerInfo.isLiked) onErrorContainer else onSecondaryContainer
                                    )
                                )
                            )
                        }
                    }
                }

                // Tap Zones Layer (Middle Zone 4)
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {}
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(
                                actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(keyAction to ACT_TOGGLE_MORE)
                                )
                            )
                    ) {}
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {}
                }
            }
        }
    }
}

@Composable
private fun MoreDetailsContent(playerInfo: WidgetPlaybackState) {
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
    val onSecondaryColor = Color(playerInfo.colorOnSecondary)
    val secondaryContainer = Color(playerInfo.colorSecondaryContainer)
    val onSecondaryContainer = Color(playerInfo.colorOnSecondaryContainer)
    val errorContainer = Color(playerInfo.colorErrorContainer)
    val onErrorContainer = Color(playerInfo.colorOnErrorContainer)
    val tertiaryColor = if (playerInfo.isMobilePlayback) Color(playerInfo.colorSecondary) else onSecondaryColor

    Box(modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp)) {
        // Blurred Background
        if (albumBitmap != null) {
            Image(
                provider = ImageProvider(albumBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
        
        // Scrim mimicking frosting
        Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(primaryColor.copy(alpha = 0.65f)))) {}

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // ROW 1: Top (Logo | Open App | Phone/PC) same as main view
            Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.TopStart) {
                    Image(provider = ImageProvider(R.drawable.finalmono), contentDescription = "Vibe-on Logo", colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor)), modifier = GlanceModifier.size(36.dp))
                }
                
                Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.TopEnd) {
                    Box(modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).background(ColorProvider(if (playerInfo.isMobilePlayback) onSecondaryContainer else secondaryContainer)).clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_TOGGLE_OUT))), contentAlignment = Alignment.Center) {
                        Image(provider = if (playerInfo.isMobilePlayback) ImageProvider(R.drawable.ic_widget_phone) else ImageProvider(R.drawable.ic_widget_computer), contentDescription = "Toggle output", modifier = GlanceModifier.size(20.dp), colorFilter = ColorFilter.tint(ColorProvider(if (playerInfo.isMobilePlayback) secondaryContainer else onSecondaryContainer)))
                    }
                }
            }
            
            // Text Layer
            Column(modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp)) {
                Text(
                    text = playerInfo.title.ifEmpty { "No Track Playing" },
                    style = TextStyle(
                        color = ColorProvider(onPrimaryColor),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily("m_plus_rounded_1c_bold")
                    ),
                    maxLines = 1
                )
                Text(
                    text = playerInfo.artist.ifEmpty { "Unknown Artist" },
                    style = TextStyle(
                        color = ColorProvider(onPrimaryColor.copy(alpha = 0.8f)),
                        fontSize = 16.sp,
                        fontFamily = FontFamily("m_plus_rounded_1c_regular")
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ROW 2: Middle - Cookie Shapes
            Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                for (i in 0..2) {
                    Box(modifier = GlanceModifier.defaultWeight().padding(8.dp), contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_cookie_shape),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ColorProvider(tertiaryColor)),
                            modifier = GlanceModifier.fillMaxWidth().height(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ROW 3: Controls
            Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Shuffle
                val isShuffle = playerInfo.isShuffled
                Box(modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp).background(ColorProvider(if (isShuffle) onSecondaryContainer else secondaryContainer)).clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_SHUFFLE))), contentAlignment = Alignment.Center) {
                    Image(provider = ImageProvider(R.drawable.ic_widget_shuffle), contentDescription = "Shuffle", colorFilter = ColorFilter.tint(ColorProvider(if (isShuffle) secondaryContainer else onSecondaryContainer)), modifier = GlanceModifier.size(24.dp))
                }

                Spacer(modifier = GlanceModifier.width(16.dp))
                
                // Repeat
                val isRepeat = playerInfo.repeatMode != "off"
                val repeatIcon = if (playerInfo.repeatMode == "one") R.drawable.ic_widget_repeat_one else R.drawable.ic_widget_repeat
                Box(modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp).background(ColorProvider(if (isRepeat) onSecondaryContainer else secondaryContainer)).clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_REPEAT))), contentAlignment = Alignment.Center) {
                    Image(provider = ImageProvider(repeatIcon), contentDescription = "Repeat", colorFilter = ColorFilter.tint(ColorProvider(if (isRepeat) secondaryContainer else onSecondaryContainer)), modifier = GlanceModifier.size(24.dp))
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Volume
                val volIcon = when (playerInfo.volumeLevel) {
                    0 -> R.drawable.ic_widget_volume_off
                    1 -> R.drawable.ic_widget_volume_mid
                    else -> R.drawable.ic_widget_volume_high
                }
                Box(modifier = GlanceModifier.size(48.dp).cornerRadius(24.dp).background(ColorProvider(secondaryContainer)).clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_VOLUME))), contentAlignment = Alignment.Center) {
                    Image(provider = ImageProvider(volIcon), contentDescription = "Volume", colorFilter = ColorFilter.tint(ColorProvider(if (playerInfo.volumeLevel == 0) errorContainer else onSecondaryContainer)), modifier = GlanceModifier.size(24.dp))
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Like
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .cornerRadius(24.dp)
                        .background(ColorProvider(if (playerInfo.isLiked) errorContainer else secondaryContainer))
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_LIKE))),
                    contentAlignment = Alignment.Center
                ) {
                     Image(
                            provider = if (playerInfo.isLiked) ImageProvider(R.drawable.ic_widget_heart_filled) else ImageProvider(R.drawable.ic_widget_heart_outline),
                            contentDescription = "Like",
                            modifier = GlanceModifier.size(24.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(if (playerInfo.isLiked) onErrorContainer else onSecondaryContainer))
                     )
                }
            }
        }

        // Tap Zones Layer (Middle Zone 4 -> Back to Main)
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {}
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {}
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_CLOSE_MORE)))) {}
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {}
            }
            Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {}
        }
    }
}

