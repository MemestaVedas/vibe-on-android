package moe.memesta.vibeon.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Score
import com.google.android.material.color.utilities.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.R
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.utils.ContrastGuard

/**
 * Extracted palette from album art using MCU.
 * Matches the design spec: Primary, Secondary, Tertiary, OnPrimary colors.
 */
data class AlbumPalette(
    val primary: Color = Color(0xFFF5B7B4),     // Warm pink default
    val secondary: Color = Color(0xFFE4B7F2),   // Lavender default
    val tertiary: Color = Color(0xFFD4C0D7),    // Muted mauve default
    val onPrimary: Color = Color(0xFF452253),    // Dark purple default
    val surface: Color = Color(0xFF141414),      // Dark bg default
    val sourceColor: Int = 0
)

/**
 * Extract an AlbumPalette from a Bitmap using MCU SchemeTonalSpot.
 */
suspend fun extractAlbumPalette(bitmap: Bitmap): AlbumPalette = withContext(Dispatchers.Default) {
    val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    if (scaled != bitmap) scaled.recycle()

    val quantized = QuantizerCelebi.quantize(pixels, 128)
    val scored = Score.score(quantized)
    val sourceColor = if (scored.isNotEmpty()) scored[0] else 0xFF6366F1.toInt()

    val hct = Hct.fromInt(sourceColor)
    val scheme = SchemeTonalSpot(hct, true, 0.0)

    AlbumPalette(
        primary = Color(scheme.primaryPalette.tone(80)),
        secondary = Color(scheme.secondaryPalette.tone(70)),
        tertiary = Color(scheme.tertiaryPalette.tone(60)),
        onPrimary = Color(scheme.primaryPalette.tone(20)),
        surface = Color(scheme.neutralPalette.tone(8)),
        sourceColor = sourceColor
    )
}

/**
 * Animated radial gradient background that shifts from middle outward.
 * Extracts palette from album art and animates color transitions.
 *
 * Design spec: "The gradient shift must animate radially from the middle to the outside."
 */
@Composable
fun AnimatedRadialGradientBackground(
    albumArtBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Default palette (PairingScreen design language)
    val defaultPalette = remember {
        AlbumPalette()
    }

    var currentPalette by remember { mutableStateOf(defaultPalette) }

    // Extract palette when bitmap changes
    LaunchedEffect(albumArtBitmap) {
        currentPalette = if (albumArtBitmap != null) {
            extractAlbumPalette(albumArtBitmap)
        } else {
            defaultPalette
        }
    }

    // Animate each color for smooth transitions
    val animPrimary by animateColorAsState(
        targetValue = currentPalette.primary,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "grad_primary"
    )
    val animSecondary by animateColorAsState(
        targetValue = currentPalette.secondary,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "grad_secondary"
    )
    val animTertiary by animateColorAsState(
        targetValue = currentPalette.tertiary,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "grad_tertiary"
    )
    val animOnPrimary by animateColorAsState(
        targetValue = currentPalette.onPrimary,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "grad_onPrimary"
    )

    // Radial expansion animation for the gradient shift effect
    val infiniteTransition = rememberInfiniteTransition(label = "radial_shift")
    val radiusMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius_pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )

    Box(
        modifier = modifier.drawBehind {
            val center = Offset(size.width / 2f, size.height * 0.4f)
            val baseRadius = size.maxDimension * 0.8f
            val animatedRadius = baseRadius * radiusMultiplier

            // Draw the animated radial gradient
            rotate(rotationAngle, pivot = center) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to animPrimary,
                            0.3f to animSecondary,
                            0.6f to animTertiary,
                            1.0f to animOnPrimary
                        ),
                        center = center,
                        radius = animatedRadius
                    ),
                    size = size
                )
            }

            // Subtle noise-like overlay for depth
            drawRect(
                color = Color.Black.copy(alpha = 0.04f),
                size = size
            )
        },
        content = content
    )
}

/**
 * Logo-to-Album-Art morph using shared element crossfade.
 * Uses a smooth scale + crossfade for a premium feel.
 *
 * Design spec: "The Vibe-on logo must morph into the album art of the song currently playing."
 */
@Composable
fun LogoToAlbumArtMorph(
    albumArtUrl: String?,
    isMorphed: Boolean,
    modifier: Modifier = Modifier,
    logoSize: Int = 200,
    artSize: Int = 200
) {
    val morphProgress by animateFloatAsState(
        targetValue = if (isMorphed && albumArtUrl != null) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "morph_progress"
    )

    val scale by animateFloatAsState(
        targetValue = if (isMorphed) 1f else 1.1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 100f
        ),
        label = "morph_scale"
    )

    Box(
        modifier = modifier.size(artSize.dp),
        contentAlignment = Alignment.Center
    ) {
        // Logo layer (fades out)
        Image(
            painter = painterResource(id = R.drawable.ic_vibe_logo),
            contentDescription = "Vibe-On Logo",
            modifier = Modifier
                .size(logoSize.dp)
                .graphicsLayer {
                    alpha = (1f - morphProgress).coerceIn(0f, 1f)
                    scaleX = scale + (1f - morphProgress) * 0.15f
                    scaleY = scale + (1f - morphProgress) * 0.15f
                    // Subtle rotation during morph
                    rotationZ = (1f - morphProgress) * 15f
                }
        )

        // Album art layer (fades in) — clipped to the AlbumArtStarShape for brand consistency
        if (albumArtUrl != null) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(artSize.dp)
                    .clip(AlbumArtStarShape)
                    .graphicsLayer {
                        alpha = morphProgress.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Empty library placeholder — branded "no content" state
 * Used when PC library is empty or desktop app is idle.
 *
 * Design spec: "Define a branded placeholder for when the PC library is empty."
 */
@Composable
fun EmptyLibraryPlaceholder(
    modifier: Modifier = Modifier,
    title: String = "NO VIBES YET",
    subtitle: String = "Your PC library is empty or the desktop app is idle."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating logo with pulse
        Image(
            painter = painterResource(id = R.drawable.ic_vibe_logo),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    translationY = floatOffset
                    alpha = pulseAlpha
                }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = moe.memesta.vibeon.ui.pairing.NorlineFontFamily,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle
        androidx.compose.material3.Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )
    }
}
