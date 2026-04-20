package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random
import moe.memesta.vibeon.MainActivity
import moe.memesta.vibeon.R

private val keyAction = ActionParameters.Key<String>("action")
private const val ACT_PLAY_PAUSE = "play_pause"
private const val ACT_PREV       = "prev"
private const val ACT_NEXT       = "next"
private const val ACT_TOGGLE_OUT = "toggle_output"

/** LRU cache for decoded album art bitmaps to avoid repeated decoding. */
private object AlbumArtBitmapCache {
    private const val CACHE_SIZE_BYTES = 2 * 1024 * 1024 // 2 MiB
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getBitmap(key: String): Bitmap? = lruCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            lruCache.put(key, bitmap)
        }
    }
}

private object WidgetNoiseBitmapCache {
    private const val CACHE_SIZE_BYTES = 512 * 1024
    private val lruCache = object : LruCache<Int, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getBitmap(alpha: Int): Bitmap? = lruCache.get(alpha)

    fun putBitmap(alpha: Int, bitmap: Bitmap) {
        if (getBitmap(alpha) == null) {
            lruCache.put(alpha, bitmap)
        }
    }
}

private fun generateNoiseBitmap(width: Int = 256, height: Int = 256, alpha: Int = 35): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val random = Random(42)

    for (index in pixels.indices) {
        val gray = random.nextInt(256)
        pixels[index] = android.graphics.Color.argb(alpha, gray, gray, gray)
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

class LikedWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val currentState = currentState<WidgetPlaybackState>()
            LikedWidgetContent(currentState)
        }
    }
}

@Composable
private fun LikedWidgetContent(state: WidgetPlaybackState) {
    val albumBitmap = state.albumArtBitmapData?.let { data ->
        try {
            val cacheKey = data.contentHashCode().toString()
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
            }
            bitmap
        } catch (e: Exception) {
            Log.w("LikedWidget", "Failed to decode cached album art", e)
            null
        }
    }
    val colorPrimary              = Color(state.colorPrimary)
    val colorOnPrimary            = Color(state.colorOnPrimary)
    val colorSecondaryContainer   = Color(state.colorSecondaryContainer)
    val colorOnSecondaryContainer = Color(state.colorOnSecondaryContainer)
    val colorErrorContainer       = Color(state.colorErrorContainer)
    val colorOnErrorContainer     = Color(state.colorOnErrorContainer)
    val colorPrimaryContainer     = Color(state.colorPrimaryContainer)
    val colorOnPrimaryContainer   = Color(state.colorOnPrimaryContainer)
    val widgetNoiseBitmap = WidgetNoiseBitmapCache.getBitmap(35) ?: generateNoiseBitmap(alpha = 35).also {
        WidgetNoiseBitmapCache.putBitmap(35, it)
    }
    val toggleBackgroundColor = if (state.isMobilePlayback) colorPrimary else colorOnPrimary
    val toggleIconColor = if (state.isMobilePlayback) colorOnPrimary else colorPrimary

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(40.dp),
        contentAlignment = Alignment.TopStart
    ) {
        // ── Layer 1: Album art full-bleed background ───────────────────
        if (albumBitmap != null) {
            Image(
                provider = ImageProvider(albumBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            // Fallback: solid primary color when no art is available
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(colorPrimary.copy(alpha = 0.40f)))
            ) {}
        }

        // ── Layer 2: Primary dark scrim over full widget ───────────────
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(colorPrimaryContainer.copy(alpha = 0.84f)))
        ) {}

        Image(
            provider = ImageProvider(widgetNoiseBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize()
        )

        // ── Layer 3: UI ────────────────────────────────────────────────
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ROW 1: Logo | [Tap Zone 3 - empty] | Output Toggle
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vibe-on monochrome logo — onPrimary tint, NO background
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(10.dp),
                    contentAlignment = Alignment.TopStart
                ) {}

                // Tap Zone 3 — open app, intentionally EMPTY (no icon)
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<MainActivity>())
                ) {}

                // Output toggle — secondaryContainer circle, onSecondaryContainer icon
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<LikedWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_TOGGLE_OUT)
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(42.dp)
                            .cornerRadius(21.dp)
                            .background(ColorProvider(toggleBackgroundColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = if (state.isMobilePlayback)
                                ImageProvider(R.drawable.ic_widget_phone)
                            else
                                ImageProvider(R.drawable.ic_widget_computer),
                            contentDescription = if (state.isMobilePlayback) "Mobile" else "PC",
                            colorFilter = ColorFilter.tint(ColorProvider(toggleIconColor)),
                            modifier = GlanceModifier.size(19.dp)
                        )
                    }
                }
            }

            // ROW 2: [Tap Zone 1 - Prev] | [Tap Zone 5 - Play/Pause] | [Tap Zone 2 - Next]
            // All three zones are intentionally EMPTY — no icons, just tap areas
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<LikedWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PREV)
                            )
                        )
                ) {}
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<LikedWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PLAY_PAUSE)
                            )
                        )
                ) {}
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            actionRunCallback<LikedWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_NEXT)
                            )
                        )
                ) {}
            }

            // ROW 3: [Tap Zone 4 - Title + Artist] | [Heart icon]
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ColorProvider(colorPrimaryContainer.copy(alpha = 0.94f))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tap Zone 4 — Title (bold) + Artist (regular), opens app
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(colorOnPrimaryContainer),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = state.artist,
                        style = TextStyle(
                            color = ColorProvider(colorOnPrimaryContainer.copy(alpha = 0.80f)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class LikedWidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[keyAction] ?: return
        val client = (context.applicationContext as? moe.memesta.vibeon.VibeonApp)
            ?.container?.webSocketClient ?: return

        when (action) {
            ACT_PLAY_PAUSE -> {
                val isPlaying = moe.memesta.vibeon.MediaNotificationManager.wsClient
                    ?.isPlaying?.value ?: false
                if (isPlaying) client.sendPause() else client.sendPlay()
            }
            ACT_PREV       -> client.sendPrevious()
            ACT_NEXT       -> client.sendNext()
            ACT_TOGGLE_OUT -> {
                val isMobile = moe.memesta.vibeon.MediaNotificationManager.isMobilePlayback
                if (isMobile) client.sendStopMobilePlayback()
                else client.sendStartMobilePlayback()
            }
        }
    }
}
