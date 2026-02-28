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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
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
        val corner = 24.dp

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

        val playPauseParams = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PLAY_PAUSE)
        val previousParams  = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PREVIOUS)
        val nextParams      = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_NEXT)

        // Outer — centres a perfect square
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Square container — tap for play/pause
            Box(
                modifier = GlanceModifier
                    .size(minSide)
                    .cornerRadius(corner)
                    .clickable(actionRunCallback<WidgetActionCallback>(playPauseParams)),
                contentAlignment = Alignment.Center
            ) {
                if (imageProvider != null) {
                    Image(
                        provider = imageProvider,
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(corner),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(corner)
                            .background(GlanceTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_music_note),
                            contentDescription = "No music",
                            modifier = GlanceModifier.size(minSide * 0.4f),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                }

                // Left 22 % → previous
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .size(width = minSide * 0.22f, height = minSide)
                            .clickable(actionRunCallback<WidgetActionCallback>(previousParams))
                    ) {}
                }

                // Right 22 % → next
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .size(width = minSide * 0.22f, height = minSide)
                            .clickable(actionRunCallback<WidgetActionCallback>(nextParams))
                    ) {}
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
    const val ACTION_PLAY_PAUSE = "moe.memesta.vibeon.WIDGET_PLAY_PAUSE"
    const val ACTION_NEXT       = "moe.memesta.vibeon.WIDGET_NEXT"
    const val ACTION_PREVIOUS   = "moe.memesta.vibeon.WIDGET_PREVIOUS"
}
