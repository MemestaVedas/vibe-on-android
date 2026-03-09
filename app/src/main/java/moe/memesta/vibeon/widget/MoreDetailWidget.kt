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
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
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
private const val ACT_OPEN_APP   = "open_app"

/** LRU cache for decoded album art bitmaps to avoid repeated decoding. */
private object MoreDetailAlbumArtCache {
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

class MoreDetailWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetStateDefinition
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val currentState = currentState<WidgetPlaybackState>()
            MoreDetailWidgetContent(currentState)
        }
    }
}

@Composable
private fun MoreDetailWidgetContent(state: WidgetPlaybackState) {
    val albumBitmap = state.albumArtBitmapData?.let { data ->
        try {
            val cacheKey = data.contentHashCode().toString()
            var bitmap = MoreDetailAlbumArtCache.getBitmap(cacheKey)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.let { MoreDetailAlbumArtCache.putBitmap(cacheKey, it) }
            }
            bitmap
        } catch (_: Exception) { null }
    }

    // Material 3 colors from palette
    val colorPrimary              = Color(state.colorPrimary)
    val colorOnPrimary            = Color(state.colorOnPrimary)
    val colorSecondary            = Color(state.colorSecondary)
    val colorOnSecondary          = Color(state.colorOnSecondary)
    val colorTertiary             = Color(0xFF7D5260) // Default tertiary
    val colorOnTertiary           = Color(0xFFFFFFFF) // Default onTertiary
    val colorSecondaryContainer   = Color(state.colorSecondaryContainer)
    val colorOnSecondaryContainer = Color(state.colorOnSecondaryContainer)
    val colorErrorContainer       = Color(state.colorErrorContainer)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(64.dp), // 64dp radius as per mockup
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

        // ── Layer 2: Light primary tint scrim over full widget ─────────
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(colorPrimary.copy(alpha = 0.25f)))
        ) {}

        // ── Layer 3: UI Content ──────────────────────────────────────
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Top Row: Logo and Phone/PC Toggle
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Vibe-on monochrome logo — onPrimary tint, NO background
                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.finalmono),
                        contentDescription = "Vibe-on",
                        colorFilter = ColorFilter.tint(ColorProvider(colorOnPrimary)),
                        modifier = GlanceModifier.size(36.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Output toggle — secondaryContainer circle, onSecondaryContainer icon
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .cornerRadius(24.dp)
                        .background(ColorProvider(colorSecondaryContainer))
                        .clickable(
                            actionRunCallback<MoreDetailWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_TOGGLE_OUT)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = if (state.isMobilePlayback)
                            ImageProvider(R.drawable.ic_widget_phone)
                        else
                            ImageProvider(R.drawable.ic_widget_computer),
                        contentDescription = if (state.isMobilePlayback) "Mobile" else "PC",
                        colorFilter = ColorFilter.tint(ColorProvider(colorOnSecondaryContainer)),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(24.dp))

            // Album Art Section - Large centered album art with play/pause tap zone
            Box(
                modifier = GlanceModifier
                    .size(220.dp)
                    .cornerRadius(24.dp)
                    .clickable(
                        actionRunCallback<MoreDetailWidgetActionCallback>(
                            actionParametersOf(keyAction to ACT_PLAY_PAUSE)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (albumBitmap != null) {
                    Image(
                        provider = ImageProvider(albumBitmap),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(colorPrimary.copy(alpha = 0.6f)))
                    ) {}
                }
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            // Track Info Section - Title, Artist, Album
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title - M PLUS Rounded 1c Bold
                Text(
                    text = state.title,
                    style = TextStyle(
                        color = ColorProvider(colorOnPrimary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Artist - M PLUS Rounded 1c Regular, secondary color
                Text(
                    text = state.artist,
                    style = TextStyle(
                        color = ColorProvider(colorSecondary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Album name - M PLUS Rounded 1c Regular, lighter than secondary
                if (state.album.isNotEmpty()) {
                    Text(
                        text = state.album,
                        style = TextStyle(
                            color = ColorProvider(colorSecondary.copy(alpha = 0.7f)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Bottom Row: Controls
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Previous button
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .clickable(
                            actionRunCallback<MoreDetailWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PREV)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_eight_sided_star),
                        contentDescription = "Previous",
                        colorFilter = ColorFilter.tint(ColorProvider(colorOnPrimary)),
                        modifier = GlanceModifier.size(32.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(24.dp))

                // Play/Pause button (larger)
                Box(
                    modifier = GlanceModifier
                        .size(64.dp)
                        .cornerRadius(32.dp)
                        .background(ColorProvider(colorSecondaryContainer))
                        .clickable(
                            actionRunCallback<MoreDetailWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_PLAY_PAUSE)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_spiky_star),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(ColorProvider(colorOnSecondaryContainer)),
                        modifier = GlanceModifier.size(40.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(24.dp))

                // Next button
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .clickable(
                            actionRunCallback<MoreDetailWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_NEXT)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_eight_sided_star),
                        contentDescription = "Next",
                        colorFilter = ColorFilter.tint(ColorProvider(colorOnPrimary)),
                        modifier = GlanceModifier.size(32.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Like / heart icon
                // outline → onSecondary, filled → errorContainer
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .clickable(
                            actionRunCallback<MoreDetailWidgetActionCallback>(
                                actionParametersOf(keyAction to ACT_LIKE)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = if (state.isLiked)
                            ImageProvider(R.drawable.ic_widget_heart_filled)
                        else
                            ImageProvider(R.drawable.ic_widget_heart_outline),
                        contentDescription = if (state.isLiked) "Unlike" else "Like",
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                if (state.isLiked) colorErrorContainer else colorOnSecondary
                            )
                        ),
                        modifier = GlanceModifier.size(32.dp)
                    )
                }
            }
        }
    }
}

class MoreDetailWidgetActionCallback : ActionCallback {
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
            ACT_LIKE -> {
                val path = moe.memesta.vibeon.MediaNotificationManager.currentTrackPath ?: return
                client.sendToggleFavorite(path)
            }
            ACT_OPEN_APP -> {
                // Already handled by actionStartActivity in UI
            }
        }
    }
}
