package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
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
import kotlin.math.roundToInt

private val keyAction = ActionParameters.Key<String>("action")
private const val ACT_PLAY_PAUSE = "play_pause"
private const val ACT_PREV       = "prev"
private const val ACT_NEXT       = "next"
private const val ACT_TOGGLE_OUT = "toggle_output"
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

/**
 * LRU cache for rendered title bitmaps to avoid re-rasterizing identical text.
 */
private object TitleBitmapCache {
    private const val CACHE_SIZE_BYTES = 2 * 1024 * 1024 // 2 MiB

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

private fun titleWeightForLength(charCount: Int): Int = when {
    charCount <= 10 -> 920
    charCount <= 20 -> ((920f + (420f - 920f) * ((charCount - 10) / 10f))).toInt()
    charCount <= 34 -> ((420f + (320f - 420f) * ((charCount - 20) / 14f))).toInt()
    else -> 300
}

private fun titleRoundnessForLength(charCount: Int): Int = when {
    charCount <= 10 -> 160
    charCount <= 24 -> 140
    else -> 120
}

private fun createRoundedGoogleSansFlexBitmap(
    context: Context,
    text: String,
    colorInt: Int,
    textSizeSp: Float,
    maxWidthDp: Float,
    fontMode: String,
    manualWidth: Int,
    manualWeight: Int,
    manualRoundness: Int
): Bitmap {
    val safeText = text.ifEmpty { "No Track Playing" }
    val isManual = fontMode.equals("manual", ignoreCase = true)
    val textWeight = if (isManual) manualWeight.coerceIn(300, 900) else titleWeightForLength(safeText.length)
    val textRoundness = if (isManual) manualRoundness.coerceIn(0, 200) else titleRoundnessForLength(safeText.length)
    val textWidthAxis = if (isManual) manualWidth.coerceIn(75, 125) else 100
    val roundnessFontRes = when {
        textRoundness >= 140 -> R.font.norline_rounded
        textRoundness >= 80 -> R.font.m_plus_rounded_1c_regular
        else -> R.font.google_sans_flex
    }
    val density = context.resources.displayMetrics.density
    val textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        textSizeSp,
        context.resources.displayMetrics
    )
    val maxWidthPx = (maxWidthDp * density).roundToInt().coerceAtLeast(1)
    val cacheKey = "$safeText|$colorInt|$textWeight|$textRoundness|$textWidthAxis|$roundnessFontRes|$textSizePx|$maxWidthPx"

    TitleBitmapCache.get(cacheKey)?.let { return it }

    val baseTypeface = ResourcesCompat.getFont(context, roundnessFontRes)

    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        color = colorInt
        typeface = baseTypeface ?: Typeface.DEFAULT
        textScaleX = textWidthAxis / 100f
        isFakeBoldText = textWeight >= 760
        isSubpixelText = true
    }

    val displayText = TextUtils.ellipsize(
        safeText,
        paint,
        maxWidthPx.toFloat(),
        TextUtils.TruncateAt.END
    ).toString()

    val textWidth = paint.measureText(displayText).roundToInt().coerceAtLeast(1)
    val fontMetrics = paint.fontMetricsInt
    val textHeight = (fontMetrics.descent - fontMetrics.ascent).coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888)
    bitmap.density = context.resources.displayMetrics.densityDpi
    val canvas = Canvas(bitmap)
    canvas.drawText(displayText, 0f, -fontMetrics.ascent.toFloat(), paint)

    TitleBitmapCache.put(cacheKey, bitmap)
    return bitmap
}

private fun createBottomGradientBitmap(colorInt: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(1, 100, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    val rgb = colorInt and 0x00FFFFFF
    val transparent = rgb
    val alpha30 = ((0.30f * 255f).roundToInt().coerceIn(0, 255) shl 24) or rgb
    val alpha70 = ((0.70f * 255f).roundToInt().coerceIn(0, 255) shl 24) or rgb
    val opaque = (0xFF shl 24) or rgb

    // Requested transparency curve:
    // 50% -> 0% opacity, 60% -> 30% opacity, 70% -> 70% opacity.
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, 100f,
        intArrayOf(transparent, transparent, alpha30, alpha70, opaque),
        floatArrayOf(0f, 0.50f, 0.60f, 0.70f, 1f),
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
    val context = LocalContext.current
    val albumBitmap = playerInfo.albumArtBitmapData?.let { data ->
        try {
            val cacheKey = data.contentHashCode().toString()
            var bitmap = AlbumArtCache.get(cacheKey)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.let { AlbumArtCache.put(cacheKey, it) }
            }
            bitmap
        } catch (e: Exception) {
            Log.w("VibeonWidget", "Failed to decode album art for main widget", e)
            null
        }
    }

    val primaryColor = Color(playerInfo.colorPrimary)
    val onPrimaryColor = Color(playerInfo.colorOnPrimary)
    val mainTitle = playerInfo.title.ifEmpty { "No Track Playing" }
    val mainTitleBitmap = createRoundedGoogleSansFlexBitmap(
        context = context,
        text = mainTitle,
        colorInt = onPrimaryColor.toArgb(),
        textSizeSp = 20f,
        maxWidthDp = 236f,
        fontMode = playerInfo.widgetFontMode,
        manualWidth = playerInfo.widgetManualWidth,
        manualWeight = playerInfo.widgetManualWeight,
        manualRoundness = playerInfo.widgetManualRoundness
    )

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
                ) {}
                
                // Top Middle: Tap Zone 3 - Open App
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {}

                // Top Right: PC/Mobile toggle — circle bg changes colour per state
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(top = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(42.dp)
                            .cornerRadius(21.dp)
                            .background(
                                ColorProvider(
                                    if (playerInfo.isMobilePlayback)
                                        onPrimaryColor.copy(alpha = 1.0f)  // light circle when using phone
                                    else
                                        primaryColor.copy(alpha = 1.0f) // invert phone colors when using PC
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
                            modifier = GlanceModifier.size(22.dp),
                            colorFilter = ColorFilter.tint(
                                ColorProvider(
                                    if (playerInfo.isMobilePlayback)
                                        primaryColor  // dark icon on light bg
                                    else
                                        onPrimaryColor // invert phone colors when using PC
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

            // ROW 3: Bottom (Text anchored to bottom of section)
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.BottomStart
            ) {
                // Visual: title and artist locked to bottom baseline
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Title + Artist
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Image(
                            provider = ImageProvider(mainTitleBitmap),
                            contentDescription = mainTitle,
                            modifier = GlanceModifier.height(26.dp)
                        )
                        Text(
                            text = playerInfo.artist.ifEmpty { "Unknown Artist" },
                            style = TextStyle(
                                color = ColorProvider(onPrimaryColor.copy(alpha = 0.8f)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily("google_sans_flex")
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = 2.dp)
                        )
                    }
                }

                // Invisible tap zones overlay (fullsize, covers the section)
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
    val context = LocalContext.current
    val albumBitmap = playerInfo.albumArtBitmapData?.let { data ->
        try {
            val cacheKey = data.contentHashCode().toString()
            var bitmap = AlbumArtCache.get(cacheKey)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.let { AlbumArtCache.put(cacheKey, it) }
            }
            bitmap
        } catch (e: Exception) {
            Log.w("VibeonWidget", "Failed to decode album art for detail widget", e)
            null
        }
    }

    // M3 colour tokens from user-generated palette
    val primaryColor          = Color(playerInfo.colorPrimary)
    val onPrimaryColor        = Color(playerInfo.colorOnPrimary)
    val primaryContainer      = Color(playerInfo.colorPrimaryContainer)
    val onPrimaryContainer    = Color(playerInfo.colorOnPrimaryContainer)
    val secondaryContainer    = Color(playerInfo.colorSecondaryContainer)
    val onSecondaryContainer  = Color(playerInfo.colorOnSecondaryContainer)
    val errorContainer        = Color(playerInfo.colorErrorContainer)
    val onErrorContainer      = Color(playerInfo.colorOnErrorContainer)
    // Used for the cookie-shape tile frame when center tile
    val onSecondaryColor      = Color(playerInfo.colorOnSecondary)
    val detailTitle = playerInfo.title.ifEmpty { "No Track Playing" }
    val detailTitleBitmap = createRoundedGoogleSansFlexBitmap(
        context = context,
        text = detailTitle,
        colorInt = onPrimaryColor.toArgb(),
        textSizeSp = 30f,
        maxWidthDp = 320f,
        fontMode = playerInfo.widgetFontMode,
        manualWidth = playerInfo.widgetManualWidth,
        manualWeight = playerInfo.widgetManualWeight,
        manualRoundness = playerInfo.widgetManualRoundness
    )

    Box(modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp)) {

        // ── Background: blurred album art ──────────────────────────────
        if (albumBitmap != null) {
            Image(
                provider = ImageProvider(albumBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        }

        // ── Frosted scrim ─────────────────────────────────────────────
        Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(primaryColor.copy(alpha = 0.65f)))) {}

        // ── Content Column ────────────────────────────────────────────
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp)) {

            // ROW 1 ─ Toggle (top-right, circle bg changes colour per state)
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(42.dp)
                        .cornerRadius(21.dp)
                        .background(
                            ColorProvider(
                                if (playerInfo.isMobilePlayback)
                                    onPrimaryColor.copy(alpha = 0.88f)
                                else
                                    primaryColor.copy(alpha = 0.88f)
                            )
                        )
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_TOGGLE_OUT))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = if (playerInfo.isMobilePlayback) ImageProvider(R.drawable.ic_widget_phone)
                                   else ImageProvider(R.drawable.ic_widget_computer),
                        contentDescription = "Toggle output",
                        modifier = GlanceModifier.size(22.dp),
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                if (playerInfo.isMobilePlayback) primaryColor
                                else onPrimaryColor
                            )
                        )
                    )
                }
            }

            // ROW 2 ─ Title + Artist/Album
            Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                Image(
                    provider = ImageProvider(detailTitleBitmap),
                    contentDescription = detailTitle,
                    modifier = GlanceModifier.height(36.dp)
                )
                Text(
                    text = buildString {
                        append(playerInfo.artist.ifEmpty { "Unknown Artist" })
                        if (!playerInfo.album.isNullOrEmpty()) {
                            append(" .")
                            append(playerInfo.album)
                        }
                    },
                    style = TextStyle(
                        color = ColorProvider(onPrimaryColor.copy(alpha = 0.8f)),
                        fontSize = 14.sp,
                        fontFamily = FontFamily("google_sans_flex")
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ROW 3 ─ Three cookie-shaped album art tiles
            // ──────────────────────────────────────────────────────────
            // In Glance (RemoteViews), arbitrary path clipping isn't possible.
            // The cookie shape is achieved by stacking:
            //   1. ic_cookie_shape icon (tinted with frame color) — acts as the border/frame
            //   2. Album art image inset with padding — sits inside the frame
            // This faithfully represents the M3E "cookie" squircle shape token.
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left tile
                Box(
                    modifier = GlanceModifier.padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Cookie-shape frame (M3E squircle border — primaryContainer tinted)
                    Image(
                        provider = ImageProvider(R.drawable.ic_cookie_shape),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor.copy(alpha = 0.45f))),
                        modifier = GlanceModifier.size(82.dp)
                    )
                    // Album art inset
                    if (albumBitmap != null) {
                        Image(
                            provider = ImageProvider(albumBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.size(68.dp).cornerRadius(18.dp)
                        )
                    }
                }

                // Center tile (larger, highlighted with onSecondary as frame)
                Box(
                    modifier = GlanceModifier.padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_cookie_shape),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(onSecondaryColor)),
                        modifier = GlanceModifier.size(106.dp)
                    )
                    if (albumBitmap != null) {
                        Image(
                            provider = ImageProvider(albumBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.size(90.dp).cornerRadius(22.dp)
                        )
                    }
                }

                // Right tile
                Box(
                    modifier = GlanceModifier.padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_cookie_shape),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(onPrimaryColor.copy(alpha = 0.45f))),
                        modifier = GlanceModifier.size(82.dp)
                    )
                    if (albumBitmap != null) {
                        Image(
                            provider = ImageProvider(albumBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.size(68.dp).cornerRadius(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ROW 4 ─ Controls
            // Shapes and colours per M3E tokens:
            //   Shuffle  → ic_star_badge  (primaryContainer bg)         + shuffle icon (onPrimaryContainer)
            //   Repeat   → cornerRadius   (secondaryContainer bg)        + repeat icon (onSecondaryContainer)
            //   Volume   → ic_spiky_star  (errorContainer bg = red)      + volume icon (onErrorContainer)
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle — M3E star-badge shape, primaryContainer coloured
                val isShuffle = playerInfo.isShuffled
                Box(
                    modifier = GlanceModifier
                        .size(54.dp)
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_SHUFFLE))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_star_badge),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(if (isShuffle) onSecondaryColor else primaryContainer)),
                        modifier = GlanceModifier.size(54.dp)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_shuffle),
                        contentDescription = "Shuffle",
                        colorFilter = ColorFilter.tint(ColorProvider(if (isShuffle) primaryColor else onPrimaryContainer)),
                        modifier = GlanceModifier.size(22.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Repeat — M3E rounded rectangle, secondaryContainer coloured
                val isRepeat = playerInfo.repeatMode != "off"
                val repeatIcon = if (playerInfo.repeatMode == "one") R.drawable.ic_widget_repeat_one else R.drawable.ic_widget_repeat
                Box(
                    modifier = GlanceModifier
                        .size(54.dp)
                        .cornerRadius(16.dp)
                        .background(ColorProvider(if (isRepeat) onSecondaryContainer else secondaryContainer))
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_REPEAT))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(repeatIcon),
                        contentDescription = "Repeat",
                        colorFilter = ColorFilter.tint(ColorProvider(if (isRepeat) secondaryContainer else onSecondaryContainer)),
                        modifier = GlanceModifier.size(26.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Volume — M3E spiky-star shape, errorContainer (red) coloured
                val volIcon = when (playerInfo.volumeLevel) {
                    0    -> R.drawable.ic_widget_volume_off
                    1    -> R.drawable.ic_widget_volume_mid
                    else -> R.drawable.ic_widget_volume_high
                }
                Box(
                    modifier = GlanceModifier
                        .size(54.dp)
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(keyAction to ACT_VOLUME))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_spiky_star),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(errorContainer)),
                        modifier = GlanceModifier.size(54.dp)
                    )
                    Image(
                        provider = ImageProvider(volIcon),
                        contentDescription = "Volume",
                        colorFilter = ColorFilter.tint(ColorProvider(onErrorContainer)),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }

        // ── Tap Zones (unchanged) ─────────────────────────────────────
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

