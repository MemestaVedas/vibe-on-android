package moe.memesta.vibeon.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Score
import com.google.android.material.color.utilities.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.image.AppImageLoader

/**
 * Dynamic theme using Material Color Utilities (MCU) SchemeTonalSpot —
 * the exact same algorithm as the PC app's useImageColors.ts.
 *
 * Both platforms:
 *   1. Extract source color from album art (dominant hue via QuantizerCelebi + Score)
 *   2. Build SchemeTonalSpot(hct, isDark=true, contrastLevel=0.0)
 *   3. Map tones to ColorScheme roles using identical tone values
 */
@Composable
fun DynamicTheme(
    seedBitmap: Bitmap? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Initialize with default scheme immediately to prevent black screen
    var colorScheme by remember { 
        mutableStateOf<ColorScheme>(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                defaultDarkColorScheme()
            }
        )
    }

    // Track if we've ever had a bitmap to distinguish initial state from refresh
    var hadBitmap by remember { mutableStateOf(false) }

    LaunchedEffect(seedBitmap, darkTheme) {
        if (seedBitmap != null) {
            // Generate new scheme from bitmap
            hadBitmap = true
            colorScheme = withContext(Dispatchers.Default) {
                generateSchemeFromBitmap(seedBitmap, darkTheme)
            }
        } else if (!hadBitmap) {
            // Only on first load without a bitmap, use fallback
            colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                defaultDarkColorScheme()
            }
        }
        // If seedBitmap is null but we had one before, keep the previous colorScheme
        // This prevents black screen during refresh when bitmap temporarily becomes null
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Generates a ColorScheme from a bitmap using MCU SchemeTonalSpot.
 * Tone values are identical to the PC app's useImageColors.ts:
 *   primary = primaryPalette.tone(80), surface = neutralPalette.tone(6), etc.
 */
private fun generateSchemeFromBitmap(bitmap: Bitmap, darkTheme: Boolean): ColorScheme {
    // Scale down for fast quantization (same as PC's sourceColorFromImage)
    val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    if (scaled != bitmap) scaled.recycle()

    // Quantize → score → pick dominant source color
    val quantized = QuantizerCelebi.quantize(pixels, 128)
    val scored = Score.score(quantized)
    val sourceColor = if (scored.isNotEmpty()) scored[0] else FALLBACK_SEED

    return buildScheme(sourceColor, darkTheme)
}

private fun buildScheme(sourceColor: Int, darkTheme: Boolean): ColorScheme {
    val hct = Hct.fromInt(sourceColor)
    val scheme = SchemeTonalSpot(hct, darkTheme, 0.0)

    // Mirror PC tone values exactly
    return if (darkTheme) {
        darkColorScheme(
            primary                = Color(scheme.primaryPalette.tone(80)),
            onPrimary              = Color(scheme.primaryPalette.tone(20)),
            primaryContainer       = Color(scheme.primaryPalette.tone(30)),
            onPrimaryContainer     = Color(scheme.primaryPalette.tone(90)),

            secondary              = Color(scheme.secondaryPalette.tone(80)),
            onSecondary            = Color(scheme.secondaryPalette.tone(20)),
            secondaryContainer     = Color(scheme.secondaryPalette.tone(30)),
            onSecondaryContainer   = Color(scheme.secondaryPalette.tone(90)),

            tertiary               = Color(scheme.tertiaryPalette.tone(80)),
            onTertiary             = Color(scheme.tertiaryPalette.tone(20)),
            tertiaryContainer      = Color(scheme.tertiaryPalette.tone(30)),
            onTertiaryContainer    = Color(scheme.tertiaryPalette.tone(90)),

            background             = Color(scheme.neutralPalette.tone(10)),
            onBackground           = Color(scheme.neutralPalette.tone(90)),
            surface                = Color(scheme.neutralPalette.tone(12)),
            onSurface              = Color(scheme.neutralPalette.tone(90)),

            surfaceVariant         = Color(scheme.neutralVariantPalette.tone(30)),
            onSurfaceVariant       = Color(scheme.neutralVariantPalette.tone(80)),

            surfaceContainerLowest = Color(scheme.neutralPalette.tone(8)),
            surfaceContainerLow    = Color(scheme.neutralPalette.tone(10)),
            surfaceContainer       = Color(scheme.neutralPalette.tone(12)),
            surfaceContainerHigh   = Color(scheme.neutralPalette.tone(14)),
            surfaceContainerHighest= Color(scheme.neutralPalette.tone(16)),

            outline                = Color(scheme.neutralVariantPalette.tone(60)),
            outlineVariant         = Color(scheme.neutralVariantPalette.tone(30)),

            error                  = ErrorColor,
            onError                = OnErrorColor,
        )
    } else {
        lightColorScheme(
            primary                = Color(scheme.primaryPalette.tone(40)),
            onPrimary              = Color(scheme.primaryPalette.tone(100)),
            primaryContainer       = Color(scheme.primaryPalette.tone(90)),
            onPrimaryContainer     = Color(scheme.primaryPalette.tone(10)),

            secondary              = Color(scheme.secondaryPalette.tone(40)),
            onSecondary            = Color(scheme.secondaryPalette.tone(100)),
            secondaryContainer     = Color(scheme.secondaryPalette.tone(90)),
            onSecondaryContainer   = Color(scheme.secondaryPalette.tone(10)),

            tertiary               = Color(scheme.tertiaryPalette.tone(40)),
            onTertiary             = Color(scheme.tertiaryPalette.tone(100)),
            tertiaryContainer      = Color(scheme.tertiaryPalette.tone(90)),
            onTertiaryContainer    = Color(scheme.tertiaryPalette.tone(10)),

            background             = Color(scheme.neutralPalette.tone(99)),
            onBackground           = Color(scheme.neutralPalette.tone(10)),
            surface                = Color(scheme.neutralPalette.tone(99)),
            onSurface              = Color(scheme.neutralPalette.tone(10)),

            surfaceVariant         = Color(scheme.neutralVariantPalette.tone(90)),
            onSurfaceVariant       = Color(scheme.neutralVariantPalette.tone(30)),

            outline                = Color(scheme.neutralVariantPalette.tone(50)),
            outlineVariant         = Color(scheme.neutralVariantPalette.tone(80)),

            error                  = ErrorColor,
            onError                = OnErrorColor,
        )
    }
}

private fun defaultDarkColorScheme(): ColorScheme {
    return buildScheme(FALLBACK_SEED, darkTheme = true)
}

private const val FALLBACK_SEED = 0xFF6366F1.toInt() // Same fallback as PC

@Composable
fun rememberBitmapFromUrl(url: String?): Bitmap? {
    if (url == null) return null
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(url) {
        val loader = AppImageLoader.get(context)
        val request = coil.request.ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Palette/MCU needs software bitmap
            .build()
        val result = withContext(Dispatchers.IO) { loader.execute(request) }
        if (result is coil.request.SuccessResult) {
            bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        }
    }
    return bitmap
}
