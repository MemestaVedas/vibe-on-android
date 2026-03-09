package moe.memesta.vibeon.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import moe.memesta.vibeon.MainActivity
import moe.memesta.vibeon.PlaybackService
import moe.memesta.vibeon.R

// Fixed UI constants — do NOT scale with widget size (2×2 design)
// Figma reference: 400×409 canvas, wave at y=285, bottom section height=124px
// At 2×2 widget (~175dp), Figma scale factor ≈ 175/400 = 0.4375
private val ICON_SIZE  = 44.dp
private val ICON_INNER = 26.dp
private val CORNER     = 40.dp
private val STRIP_W    = 56.dp
private val WAVE_H     = 32.dp   // wave bitmap height (purely visual divider)
private val SCRIM_H    = 64.dp   // solid info strip below wave
private val BOTTOM_H   = WAVE_H + SCRIM_H
private val PAD        = 12.dp

// More-options panel constants
private val MOP_PAD         = 10.dp  // padding inside more-options panel
private val CTRL_BTN_SIZE   = 44.dp  // control button (shuffle / repeat / volume / heart)
private val CTRL_BTN_CORNER = 16.dp  // squircle-ish corner radius for control buttons
private val CTRL_ICON_SIZE  = 24.dp  // icon inside control button
private val PL_CORNER       = 18.dp  // playlist tile corner radius

/**
 * Draws the Figma wave shape (viewBox 400×120) into a Bitmap at any resolution.
 * The area ABOVE the wave curve is transparent; the area BELOW is filled with [color].
 * Using a drawn Bitmap avoids Glance's unreliable ColorFilter.tint on vector drawables.
 */
private fun makeWaveBitmap(color: Color, widthPx: Int = 200, heightPx: Int = 45): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red   * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue  * 255).toInt()
        )
        style = android.graphics.Paint.Style.FILL
    }
    // Exact Figma path scaled from 400×120 viewBox:
    // M0,48 C60,8 120,80 200,44 C280,8 340,72 400,40 L400,120 L0,120 Z
    val sx = widthPx  / 400f
    val sy = heightPx / 120f
    val path = android.graphics.Path().apply {
        moveTo(  0f * sx,  48f * sy)
        cubicTo( 60f * sx,  8f * sy,  120f * sx,  80f * sy,  200f * sx,  44f * sy)
        cubicTo(280f * sx,  8f * sy,  340f * sx,  72f * sy,  400f * sx,  40f * sy)
        lineTo(400f * sx, 120f * sy)
        lineTo(  0f * sx, 120f * sy)
        close()
    }
    canvas.drawPath(path, paint)
    return bmp
}

/**
 * Variant 1 – Gradient + wave widget.
 * The bottom section fades from transparent to palette-primary with a wavy top edge.
 *
 * Tap zones:
 *  Left strip  → Previous | Right strip → Next
 *  Centre-top 35%         → Open app
 *  Centre-middle          → Play / Pause
 *  Centre-bottom          → Open app
 * Visible corner buttons (fixed size):
 *  Top-left  → app logo (no background)  → Open app
 *  Top-right → source-toggle (secondary circle, onSecondary icon)
 *  Bottom-right → heart (secondary / error)
 */
class AlbumArtWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetPlaybackState>()
            GlanceTheme { WidgetContent(state, context, gradientScrim = true) }
        }
    }
}

/**
 * Variant 2 – Solid + wave widget.
 * The bottom section is 100% palette-primary with a wavy top edge.
 */
class AlbumArtWidgetSolid : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<WidgetPlaybackState>()
            GlanceTheme { WidgetContent(state, context, gradientScrim = false) }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared composable — dispatches to normal or more-options view
// ---------------------------------------------------------------------------

@Composable
internal fun WidgetContent(state: WidgetPlaybackState, context: Context, gradientScrim: Boolean) {
    if (state.showingMoreOptions) {
        MoreOptionsContent(state, context)
    } else {
        NormalWidgetContent(state, context, gradientScrim)
    }
}

/**
 * Variant 1 – Gradient + wave widget (normal playback view).
 */
@Composable
internal fun NormalWidgetContent(state: WidgetPlaybackState, context: Context, gradientScrim: Boolean) {
    val size    = LocalSize.current
    val minSide = min(size.width, size.height)

    val primary     = Color(state.colorPrimary)
    val onPrimary   = Color(state.colorOnPrimary)
    val secondary   = Color(state.colorSecondary)
    val onSecondary = Color(state.colorOnSecondary)
    val errorColor  = Color(state.colorError)

    // Load album-art from the file written by WidgetUpdater.
    // BitmapFactory.decodeFile runs in the app process (inside provideContent) so it can
    // access filesDir. The resulting bitmap is serialised into RemoteViews in-process,
    // bypassing the cross-process IPC bundle size limit entirely.
    val imageProvider = state.albumArtPath?.let { path ->
        try {
            BitmapFactory.decodeFile(path)?.let { ImageProvider(it) }
        } catch (e: Exception) {
            Log.w("AlbumArtWidget", "Album art load failed", e); null
        }
    }

    val openApp = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = GlanceModifier
                .size(minSide)
                .cornerRadius(CORNER)
                .background(ColorProvider(primary)),
            contentAlignment = Alignment.Center
        ) {

            // Layer 1: Album art — SCALES with widget size
            if (imageProvider != null) {
                Image(
                    provider          = imageProvider,
                    contentDescription = "Album Art",
                    modifier          = GlanceModifier.fillMaxSize().cornerRadius(CORNER),
                    contentScale      = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(CORNER)
                        .background(ColorProvider(Color(0xFF1C1B1F))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider          = ImageProvider(R.drawable.ic_widget_music_note),
                        contentDescription = "No music",
                        modifier          = GlanceModifier.size(64.dp),
                        contentScale      = ContentScale.Fit,
                        colorFilter       = ColorFilter.tint(ColorProvider(Color(0x66FFFFFF)))
                    )
                }
            }

            // Layer 2: Gradient scrim (gradient variant only)
            // 7 steps from transparent → primary, extending slightly above the wave
            if (gradientScrim) {
                val gradH = BOTTOM_H + 20.dp  // extra 20dp bleed into artwork
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    val alphas = listOf(0x00, 0x18, 0x38, 0x60, 0x90, 0xC0, 0xEC)
                    for (a in alphas) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(gradH / 7)
                                .background(ColorProvider(primary.copy(alpha = a / 255f)))
                        ) {}
                    }
                }
            }

            // Layer 3: Wave bitmap + solid info strip
            // Wave is drawn as a Bitmap at runtime using Canvas, avoiding Glance's
            // unreliable ColorFilter.tint behaviour on vector drawables.
            val waveColor = if (gradientScrim) primary.copy(alpha = 0.97f) else primary
            val waveBitmap = makeWaveBitmap(waveColor)
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Image(
                    provider          = ImageProvider(waveBitmap),
                    contentDescription = null,
                    modifier          = GlanceModifier.fillMaxWidth().height(WAVE_H),
                    contentScale      = ContentScale.FillBounds
                )
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(SCRIM_H)
                        .background(ColorProvider(primary))
                ) {}
            }

            // Layer 4: Text — title + artist (fixed sizes, do NOT scale)
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = PAD, vertical = PAD)
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    modifier           = GlanceModifier.fillMaxWidth().height(SCRIM_H - PAD),
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight().padding(end = 8.dp)) {
                        Text(
                            text  = state.title,
                            style = TextStyle(
                                color      = ColorProvider(onPrimary),
                                fontSize   = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text  = state.artist,
                            style = TextStyle(
                                color    = ColorProvider(onPrimary),
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }
                    // Reserve space so text doesn't slide under the heart button
                    Spacer(modifier = GlanceModifier.size(ICON_SIZE))
                }
            }

            // Layer 5: Invisible tap zones
            Row(modifier = GlanceModifier.fillMaxSize()) {
                Box(
                    modifier = GlanceModifier.width(STRIP_W).fillMaxHeight()
                        .clickable(actionRunCallback<WidgetActionCallback>(
                            actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PREVIOUS)
                        ))
                ) {}
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    val topH = minSide * 0.35f
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().height(topH)
                            .clickable(actionStartActivity(openApp))
                    ) {}
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_PLAY_PAUSE)
                            ))
                    ) {}
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().height(SCRIM_H * 0.5f)
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_MORE_OPTIONS)
                            ))
                    ) {}
                }
                Box(
                    modifier = GlanceModifier.width(STRIP_W).fillMaxHeight()
                        .clickable(actionRunCallback<WidgetActionCallback>(
                            actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_NEXT)
                        ))
                ) {}
            }

            // Layer 6a: Top-left — app logo, NO background circle
            Box(
                modifier           = GlanceModifier.fillMaxSize().padding(PAD),
                contentAlignment   = Alignment.TopStart
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Image(
                        provider          = ImageProvider(R.drawable.finalmono),
                        contentDescription = "Open Vibe-On",
                        modifier          = GlanceModifier
                            .size(ICON_SIZE)
                            .clickable(actionStartActivity(openApp)),
                        colorFilter       = ColorFilter.tint(ColorProvider(onPrimary))
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Top-right — source toggle: secondary bg, onSecondary icon
                    Box(
                        modifier = GlanceModifier
                            .size(ICON_SIZE)
                            .cornerRadius(100.dp)
                            .background(ColorProvider(secondary))
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_TOGGLE_OUTPUT)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (state.isMobilePlayback) R.drawable.ic_widget_phone
                                else R.drawable.ic_widget_computer
                            ),
                            contentDescription = if (state.isMobilePlayback) "Playing on phone" else "Playing on PC",
                            modifier    = GlanceModifier.size(ICON_INNER),
                            colorFilter = ColorFilter.tint(ColorProvider(onSecondary))
                        )
                    }
                }
            }

            // Layer 6b: Bottom-right — heart / like button
            Box(
                modifier           = GlanceModifier.fillMaxSize().padding(PAD),
                contentAlignment   = Alignment.BottomEnd
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(ICON_SIZE)
                        .cornerRadius(100.dp)
                        .background(ColorProvider(
                            if (state.isLiked) errorColor.copy(alpha = 0.25f) else secondary
                        ))
                        .clickable(actionRunCallback<WidgetActionCallback>(
                            actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_FAVORITE)
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (state.isLiked) R.drawable.ic_widget_heart_filled
                            else R.drawable.ic_widget_heart_outline
                        ),
                        contentDescription = if (state.isLiked) "Unlike" else "Like",
                        modifier    = GlanceModifier.size(ICON_INNER),
                        colorFilter = ColorFilter.tint(ColorProvider(
                            if (state.isLiked) errorColor else onSecondary
                        ))
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// More-options overlay composable
// ---------------------------------------------------------------------------

/**
 * Replaces the normal widget content when the user taps the bottom "more options" zone.
 * Layout (Figma Frame 17/18/19):
 *   Row 1: App icon  |  Title + Artist (tap to close)  |  Source toggle
 *   Row 2: Playlist tiles (up to 4, cookie-shaped — tap = add current track)
 *   Row 3: Shuffle | Repeat | Volume (tri-cycle) | Heart
 */
@Composable
internal fun MoreOptionsContent(state: WidgetPlaybackState, context: Context) {
    val size    = LocalSize.current
    val minSide = min(size.width, size.height)

    val primary             = Color(state.colorPrimary)
    val onPrimary           = Color(state.colorOnPrimary)
    val secondary           = Color(state.colorSecondary)
    val onSecondary         = Color(state.colorOnSecondary)
    val errorColor          = Color(state.colorError)
    val primaryContainer    = Color(state.colorPrimaryContainer)
    val onPrimaryContainer  = Color(state.colorOnPrimaryContainer)
    val secondContainer     = Color(state.colorSecondaryContainer)
    val onSecondContainer   = Color(state.colorOnSecondaryContainer)
    val errorContainer      = Color(state.colorErrorContainer)
    val onErrorContainer    = Color(state.colorOnErrorContainer)

    val openApp = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    // Load album art for background
    val imageProvider = state.albumArtPath?.let { path ->
        try { BitmapFactory.decodeFile(path)?.let { ImageProvider(it) } } catch (e: Exception) { null }
    }

    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = GlanceModifier
                .size(minSide)
                .cornerRadius(CORNER)
                .background(ColorProvider(primary)),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: blurred album art background
            if (imageProvider != null) {
                Image(
                    provider           = imageProvider,
                    contentDescription = null,
                    modifier           = GlanceModifier.fillMaxSize().cornerRadius(CORNER),
                    contentScale       = ContentScale.Crop
                )
            }

            // Layer 2: semi-transparent primary scrim
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(primary.copy(alpha = 0.65f)))
            ) {}

            // Layer 3: panel content
            Column(
                modifier           = GlanceModifier.fillMaxSize().padding(MOP_PAD),
                verticalAlignment  = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                // ── Row 1: App icon │ Title + Artist │ Source toggle ──────
                Row(
                    modifier          = GlanceModifier.fillMaxWidth().height(ICON_SIZE),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon (open app)
                    Image(
                        provider           = ImageProvider(R.drawable.finalmono),
                        contentDescription = "Open Vibe-On",
                        modifier           = GlanceModifier
                            .size(ICON_SIZE)
                            .clickable(actionStartActivity(openApp)),
                        colorFilter        = ColorFilter.tint(ColorProvider(onPrimary))
                    )

                    // Title + artist — tap to close the panel
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp)
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_CLOSE_MORE_OPTIONS)
                            )),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = state.title,
                            style    = TextStyle(
                                color      = ColorProvider(onPrimary),
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text     = state.artist,
                            style    = TextStyle(
                                color    = ColorProvider(onPrimary.copy(alpha = 0.75f)),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }

                    // Source toggle
                    Box(
                        modifier = GlanceModifier
                            .size(ICON_SIZE)
                            .cornerRadius(100.dp)
                            .background(ColorProvider(secondary))
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_TOGGLE_OUTPUT)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (state.isMobilePlayback) R.drawable.ic_widget_phone
                                else R.drawable.ic_widget_computer
                            ),
                            contentDescription = null,
                            modifier    = GlanceModifier.size(ICON_INNER),
                            colorFilter = ColorFilter.tint(ColorProvider(onSecondary))
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ── Row 2: Playlist tiles ─────────────────────────────────
                // Pre-extract exactly 4 slots so the Row always produces a
                // constant child count (4 PlaylistTile + 3 Spacer). Glance /
                // RemoteViews require a stable template structure across updates.
                val pl0 = state.widgetPlaylists.getOrNull(0)
                val pl1 = state.widgetPlaylists.getOrNull(1)
                val pl2 = state.widgetPlaylists.getOrNull(2)
                val pl3 = state.widgetPlaylists.getOrNull(3)
                Row(
                    modifier          = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistTile(pl0, secondary, onPrimary)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    PlaylistTile(pl1, secondary, onPrimary)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    PlaylistTile(pl2, secondary, onPrimary)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    PlaylistTile(pl3, secondary, onPrimary)
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ── Row 3: Control buttons ─────────────────────────────────
                Row(
                    modifier          = GlanceModifier.fillMaxWidth().height(CTRL_BTN_SIZE),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle — checked: primaryContainer / onPrimaryContainer
                    val shuffleBg   = if (state.isShuffled) primaryContainer   else primary
                    val shuffleFg   = if (state.isShuffled) onPrimaryContainer else onPrimary
                    ControlButton(
                        iconRes     = R.drawable.ic_widget_shuffle,
                        bgColor     = shuffleBg,
                        iconColor   = shuffleFg,
                        clickParams = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_SHUFFLE),
                        modifier    = GlanceModifier.defaultWeight().fillMaxHeight()
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Repeat — checked: secondaryContainer / onSecondaryContainer
                    val repeatOn  = state.repeatMode != "off"
                    val repeatBg  = if (repeatOn) secondContainer   else primary
                    val repeatFg  = if (repeatOn) onSecondContainer else onPrimary
                    val repeatImg = if (state.repeatMode == "one") R.drawable.ic_widget_repeat_one
                                   else R.drawable.ic_widget_repeat
                    ControlButton(
                        iconRes     = repeatImg,
                        bgColor     = repeatBg,
                        iconColor   = repeatFg,
                        clickParams = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_REPEAT),
                        modifier    = GlanceModifier.defaultWeight().fillMaxHeight()
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Volume — tri-mode: 2=100%(primary) / 1=50%(primary) / 0=mute(errorContainer)
                    val (volImg, volBg, volFg) = when (state.volumeLevel) {
                        0    -> Triple(R.drawable.ic_widget_volume_off,  errorContainer, onErrorContainer)
                        1    -> Triple(R.drawable.ic_widget_volume_mid,  primary,        onPrimary)
                        else -> Triple(R.drawable.ic_widget_volume_high, primary,        onPrimary)
                    }
                    ControlButton(
                        iconRes     = volImg,
                        bgColor     = volBg,
                        iconColor   = volFg,
                        clickParams = actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_VOLUME_CYCLE),
                        modifier    = GlanceModifier.defaultWeight().fillMaxHeight()
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Heart — circle, errorContainer when liked
                    Box(
                        modifier = GlanceModifier
                            .width(CTRL_BTN_SIZE)
                            .fillMaxHeight()
                            .cornerRadius(100.dp)
                            .background(ColorProvider(
                                if (state.isLiked) errorColor.copy(alpha = 0.25f) else secondary
                            ))
                            .clickable(actionRunCallback<WidgetActionCallback>(
                                actionParametersOf(WidgetActionCallback.actionKey to WidgetActions.ACTION_FAVORITE)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (state.isLiked) R.drawable.ic_widget_heart_filled
                                else R.drawable.ic_widget_heart_outline
                            ),
                            contentDescription = null,
                            modifier    = GlanceModifier.size(CTRL_ICON_SIZE),
                            colorFilter = ColorFilter.tint(ColorProvider(
                                if (state.isLiked) errorColor else onSecondary
                            ))
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cookie-shaped playlist tile used in the more-options panel
// ---------------------------------------------------------------------------

/**
 * Renders one playlist slot. When [playlist] is null the tile shows as a
 * dimmer empty placeholder. Always produces exactly one Box + one Text child
 * so the Glance/RemoteViews template structure is constant across updates.
 */
@Composable
private fun PlaylistTile(
    playlist: PlaylistWidgetInfo?,
    bgColor:  Color,
    textColor: Color
) {
    val alpha = if (playlist != null) 0.55f else 0.2f
    val baseModifier = GlanceModifier
        .defaultWeight()
        .fillMaxHeight()
        .cornerRadius(PL_CORNER)
        .background(ColorProvider(bgColor.copy(alpha = alpha)))
    val tileModifier = if (playlist != null) {
        baseModifier.clickable(actionRunCallback<WidgetActionCallback>(
            actionParametersOf(
                WidgetActionCallback.actionKey to WidgetActions.ACTION_ADD_TO_PLAYLIST,
                WidgetActionCallback.playlistIdKey to playlist.id
            )
        ))
    } else {
        baseModifier
    }
    Box(modifier = tileModifier, contentAlignment = Alignment.Center) {
        Text(
            text  = playlist?.name ?: "",
            style = TextStyle(
                color      = ColorProvider(textColor),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 3
        )
    }
}

// ---------------------------------------------------------------------------
// Reusable squircle-shaped control button used in the more-options panel
// ---------------------------------------------------------------------------

@Composable
private fun ControlButton(
    iconRes:     Int,
    bgColor:     Color,
    iconColor:   Color,
    clickParams: ActionParameters,
    modifier:    GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .cornerRadius(CTRL_BTN_CORNER)
            .background(ColorProvider(bgColor))
            .clickable(actionRunCallback<WidgetActionCallback>(clickParams)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider           = ImageProvider(iconRes),
            contentDescription = null,
            modifier           = GlanceModifier.size(CTRL_ICON_SIZE),
            colorFilter        = ColorFilter.tint(ColorProvider(iconColor))
        )
    }
}

// ---------------------------------------------------------------------------
// Action callback
// ---------------------------------------------------------------------------

class WidgetActionCallback : ActionCallback {
    companion object {
        val actionKey     = ActionParameters.Key<String>("widget_action")
        val playlistIdKey = ActionParameters.Key<String>("playlist_id")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[actionKey] ?: return
        val wsClient = moe.memesta.vibeon.MediaNotificationManager.wsClient
        try {
            when (action) {
                WidgetActions.ACTION_PLAY_PAUSE -> {
                    val isPlaying = moe.memesta.vibeon.MediaNotificationManager.wsClient?.isPlaying?.value == true
                    if (isPlaying) wsClient?.sendPause() else wsClient?.sendPlay()
                }
                WidgetActions.ACTION_NEXT      -> wsClient?.sendNext()
                WidgetActions.ACTION_PREVIOUS  -> wsClient?.sendPrevious()
                WidgetActions.ACTION_SHUFFLE   -> wsClient?.sendToggleShuffle()
                WidgetActions.ACTION_REPEAT    -> wsClient?.sendToggleRepeat()
                WidgetActions.ACTION_VOLUME_CYCLE -> {
                    // Cycle: 100 % → 50 % → mute → 100 %
                    val currentVol = moe.memesta.vibeon.MediaNotificationManager.volume
                    val nextVolume = when {
                        currentVol > 0.75 -> 0.5
                        currentVol > 0.1  -> 0.0
                        else              -> 1.0
                    }
                    wsClient?.sendSetVolume(nextVolume)
                }
                WidgetActions.ACTION_FAVORITE  -> {
                    val trackPath = moe.memesta.vibeon.MediaNotificationManager.currentTrackPath
                    if (!trackPath.isNullOrEmpty()) wsClient?.sendToggleFavorite(trackPath)
                }
                WidgetActions.ACTION_TOGGLE_OUTPUT -> {
                    val isMobile = moe.memesta.vibeon.MediaNotificationManager.isMobilePlayback
                    if (isMobile) wsClient?.sendStopMobilePlayback() else wsClient?.sendStartMobilePlayback()
                }
                WidgetActions.ACTION_MORE_OPTIONS -> {
                    WidgetUpdater.onMoreOptionsChanged(true)
                }
                WidgetActions.ACTION_CLOSE_MORE_OPTIONS -> {
                    WidgetUpdater.onMoreOptionsChanged(false)
                }
                WidgetActions.ACTION_ADD_TO_PLAYLIST -> {
                    val playlistId = parameters[playlistIdKey]
                    val trackPath  = moe.memesta.vibeon.MediaNotificationManager.currentTrackPath
                    if (!playlistId.isNullOrEmpty() && !trackPath.isNullOrEmpty()) {
                        wsClient?.sendAddToPlaylist(playlistId, trackPath)
                    }
                }
                else -> {
                    val intent = Intent(context, PlaybackService::class.java).apply { this.action = action }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("WidgetAction", "Cannot execute action for $action", e)
        }
    }
}

object WidgetActions {
    const val ACTION_PLAY_PAUSE         = "moe.memesta.vibeon.WIDGET_PLAY_PAUSE"
    const val ACTION_NEXT               = "moe.memesta.vibeon.WIDGET_NEXT"
    const val ACTION_PREVIOUS           = "moe.memesta.vibeon.WIDGET_PREVIOUS"
    const val ACTION_SHUFFLE            = "moe.memesta.vibeon.WIDGET_SHUFFLE"
    const val ACTION_REPEAT             = "moe.memesta.vibeon.WIDGET_REPEAT"
    const val ACTION_VOLUME_CYCLE       = "moe.memesta.vibeon.WIDGET_VOLUME_CYCLE"
    const val ACTION_FAVORITE           = "moe.memesta.vibeon.WIDGET_FAVORITE"
    const val ACTION_TOGGLE_OUTPUT      = "moe.memesta.vibeon.WIDGET_TOGGLE_OUTPUT"
    const val ACTION_MORE_OPTIONS       = "moe.memesta.vibeon.WIDGET_MORE_OPTIONS"
    const val ACTION_CLOSE_MORE_OPTIONS = "moe.memesta.vibeon.WIDGET_CLOSE_MORE_OPTIONS"
    const val ACTION_ADD_TO_PLAYLIST    = "moe.memesta.vibeon.WIDGET_ADD_TO_PLAYLIST"
}
