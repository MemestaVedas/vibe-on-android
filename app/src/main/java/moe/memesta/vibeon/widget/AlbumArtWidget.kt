package moe.memesta.vibeon.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.height
import androidx.compose.ui.graphics.Color
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.sp
import android.content.ComponentName
import moe.memesta.vibeon.MainActivity

import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import moe.memesta.vibeon.PlaybackService
import moe.memesta.vibeon.R

/**
 * A clean, square album-art-only widget.
 *
 * Interactions (all invisible — no visible buttons):
 * - **Tap centre** → play / pause
 * - **Tap left edge** → previous track
 * - **Tap right edge** → next track
 */
class AlbumArtWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetPlaybackState>()
            GlanceTheme {
                AlbumArtContent(state = state, context = context)
            }
        }
    }

    @Composable
    private fun AlbumArtContent(state: WidgetPlaybackState, context: Context) {
        val size = LocalSize.current
        val minSide = min(size.width, size.height)
        val corner = 28.dp

        // Decode cached album-art bytes
        val imageProvider = state.albumArtBytes?.let { data ->
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, opts)

                val targetPx = (minSide.value * context.resources.displayMetrics.density).toInt()
                var sample = 1
                if (opts.outHeight > targetPx || opts.outWidth > targetPx) {
                    val hh = opts.outHeight / 2; val hw = opts.outWidth / 2
                    while (hh / sample >= targetPx && hw / sample >= targetPx) sample *= 2
                }
                opts.inSampleSize = sample
                opts.inJustDecodeBounds = false
                BitmapFactory.decodeByteArray(data, 0, data.size, opts)?.let { ImageProvider(it) }
            } catch (e: Exception) {
                Log.w("AlbumArtWidget", "Bitmap decode failed", e)
                null
            }
        }

        // Outer — centres the card
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Card container
            Box(
                modifier = GlanceModifier
                    .size(minSide)
                    .cornerRadius(corner),
                contentAlignment = Alignment.Center
            ) {
                // ── Layer 1: Album Art (full-bleed background)
                if (imageProvider != null) {
                    Image(
                        provider = imageProvider,
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(corner),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(corner)
                            .background(ColorProvider(Color(0xFF1C1B1F))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_music_note),
                            contentDescription = "No music",
                            modifier = GlanceModifier.size(minSide * 0.35f),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0x66FFFFFF)))
                        )
                    }
                }

                // ── Layer 2: Bottom gradient scrim (invisible overlay for legibility)
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Scrim covers bottom ~40% of card
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(minSide * 0.42f)
                            .background(ColorProvider(Color(0xCC000000)))
                    ) {}
                }

                // ── Layer 3: UI — top controls + bottom metadata + play button
                Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {

                    // -- TOP ROW: App label (left) + output toggle + like + shuffle (right)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App name
                        Text(
                            text = "Vibe-On",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Control pill on top-right: output toggle · like · shuffle
                        Row(
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0x80000000)))
                                .cornerRadius(100.dp)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Output toggle (phone = mobile, computer = PC)
                            Image(
                                provider = ImageProvider(
                                    if (state.isMobilePlayback) android.R.drawable.stat_sys_speakerphone
                                    else android.R.drawable.ic_media_play
                                ),
                                contentDescription = if (state.isMobilePlayback) "Playing on phone" else "Playing on PC",
                                modifier = GlanceModifier.size(22.dp)
                                    .clickable(actionRunCallback<WidgetActionCallback>(
                                        actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_TOGGLE_OUTPUT)
                                    )),
                                colorFilter = ColorFilter.tint(ColorProvider(
                                    if (state.isMobilePlayback) Color(0xFF80DFFF) else Color.White
                                ))
                            )

                            Spacer(modifier = GlanceModifier.size(6.dp))

                            // Shuffle
                            Image(
                                provider = ImageProvider(
                                    if (state.isShuffled) android.R.drawable.btn_star_big_on
                                    else android.R.drawable.btn_star_big_off
                                ),
                                contentDescription = "Shuffle",
                                modifier = GlanceModifier.size(22.dp)
                                    .clickable(actionRunCallback<WidgetActionCallback>(
                                        actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_SHUFFLE)
                                    )),
                                colorFilter = ColorFilter.tint(ColorProvider(
                                    if (state.isShuffled) Color(0xFFFFD700) else Color.White
                                ))
                            )

                            Spacer(modifier = GlanceModifier.size(6.dp))

                            // Like / Favorite
                            Image(
                                provider = ImageProvider(
                                    if (state.isLiked) android.R.drawable.star_on
                                    else android.R.drawable.star_off
                                ),
                                contentDescription = "Like",
                                modifier = GlanceModifier.size(22.dp)
                                    .clickable(actionRunCallback<WidgetActionCallback>(
                                        actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_FAVORITE)
                                    )),
                                colorFilter = ColorFilter.tint(ColorProvider(
                                    if (state.isLiked) Color(0xFFFF6B6B) else Color.White
                                ))
                            )
                        }
                    }

                    // Spacer pushes content to bottom
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // -- BOTTOM ROW: Title+Artist (left) + large Play/Pause circle (right)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Title + Artist
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = state.title,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = state.artist,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xCCFFFFFF)),
                                    fontSize = 13.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.size(12.dp))

                        // Large circular Play/Pause button
                        Box(
                            modifier = GlanceModifier
                                .size(56.dp)
                                .cornerRadius(100.dp)
                                .background(ColorProvider(Color(0xFFE8DEF8))) // Material You tonal container
                                .clickable(actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PLAY_PAUSE)
                                )),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (state.isPlaying) android.R.drawable.ic_media_pause
                                    else android.R.drawable.ic_media_play
                                ),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(28.dp),
                                colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF21005D)))
                            )
                        }
                    }
                }

                // ── Layer 4: Invisible tap zones (on top of everything, full coverage)
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // TOP zone → open app
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                            .clickable(actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                            ))
                    ) {}

                    // MIDDLE row → prev (left) / play-pause (centre) / next (right)
                    Row(modifier = GlanceModifier.fillMaxWidth().height(minSide * 0.35f)) {
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                                .clickable(actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PREVIOUS)
                                ))
                        ) {}
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                                .clickable(actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PLAY_PAUSE)
                                ))
                        ) {}
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                                .clickable(actionRunCallback<WidgetActionCallback>(
                                    actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_NEXT)
                                ))
                        ) {}
                    }

                    // BOTTOM zone → transparent (visible bottom row buttons handle clicks above)
                    Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {}
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Action callback — receives taps from the widget and starts the service
// ---------------------------------------------------------------------------

class WidgetActionCallback : ActionCallback {
    companion object {
        val actionKey = ActionParameters.Key<String>("widget_action")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[actionKey] ?: return
        val intent = Intent(context, PlaybackService::class.java).apply { this.action = action }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w("WidgetAction", "Cannot start service for $action", e)
        }
    }
}

object WidgetActions {
    const val ACTION_PLAY_PAUSE    = "moe.memesta.vibeon.WIDGET_PLAY_PAUSE"
    const val ACTION_NEXT          = "moe.memesta.vibeon.WIDGET_NEXT"
    const val ACTION_PREVIOUS      = "moe.memesta.vibeon.WIDGET_PREVIOUS"
    const val ACTION_SHUFFLE       = "moe.memesta.vibeon.WIDGET_SHUFFLE"
    const val ACTION_FAVORITE      = "moe.memesta.vibeon.WIDGET_FAVORITE"
    const val ACTION_TOGGLE_OUTPUT = "moe.memesta.vibeon.WIDGET_TOGGLE_OUTPUT"
}
