package moe.memesta.vibeon.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic theme with dual-source color generation:
 * 1. Primary: Album art colors (when seedBitmap is provided)
 * 2. Fallback: System wallpaper colors (Material You)
 * 
 * @param seedBitmap Album art bitmap for color extraction
 * @param darkTheme Force dark theme (defaults to system setting)
 * @param content Composable content
 */
@Composable
fun DynamicTheme(
    seedBitmap: Bitmap? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // State for color scheme
    var colorScheme by remember { mutableStateOf<ColorScheme?>(null) }
    
    // Generate color scheme from album art or wallpaper
    LaunchedEffect(seedBitmap, darkTheme) {
        colorScheme = if (seedBitmap != null) {
            // Primary source: Album art colors
            withContext(Dispatchers.Default) {
                generateColorSchemeFromBitmap(seedBitmap, darkTheme)
            }
        } else {
            // Fallback: System wallpaper colors (Material You)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                // Pre-Android 12: Use static dark scheme
                defaultDarkColorScheme()
            }
        }
    }
    
    // Show content when color scheme is ready
    colorScheme?.let { scheme ->
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Generate ColorScheme from album art bitmap using Palette API
 */
private suspend fun generateColorSchemeFromBitmap(
    bitmap: Bitmap,
    darkTheme: Boolean
): ColorScheme {
    val palette = Palette.from(bitmap).generate()
    
    // Extract vibrant colors for primary, secondary, tertiary
    val vibrant = palette.vibrantSwatch?.rgb ?: 0xFF6750A4.toInt()
    val darkVibrant = palette.darkVibrantSwatch?.rgb ?: 0xFF381E72.toInt()
    val lightVibrant = palette.lightVibrantSwatch?.rgb ?: 0xFFD0BCFF.toInt()
    val muted = palette.mutedSwatch?.rgb ?: 0xFF625B71.toInt()
    
    val primaryColor = Color(vibrant)
    val secondaryColor = Color(lightVibrant)
    val tertiaryColor = Color(muted)
    
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White,
            primaryContainer = Color(darkVibrant),
            onPrimaryContainer = primaryColor.ensureLuminance(0.8f),
            
            secondary = secondaryColor,
            onSecondary = if (secondaryColor.luminance() > 0.5f) Color.Black else Color.White,
            secondaryContainer = secondaryColor.copy(alpha = 0.3f).compositeOver(VibeBackground),
            onSecondaryContainer = secondaryColor.ensureLuminance(0.7f),
            
            tertiary = tertiaryColor,
            onTertiary = if (tertiaryColor.luminance() > 0.5f) Color.Black else Color.White,
            tertiaryContainer = tertiaryColor.copy(alpha = 0.3f).compositeOver(VibeBackground),
            onTertiaryContainer = tertiaryColor.ensureLuminance(0.7f),
            
            background = VibeBackground,
            onBackground = Color(0xFFE6E1E5),
            surface = VibeSurface,
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = VibeSurfaceContainer,
            onSurfaceVariant = Color(0xFFCAC4D0),
            
            error = ErrorColor,
            onError = OnErrorColor,
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White,
            // Add light theme colors if needed
        )
    }
}

/**
 * Default dark color scheme (fallback for pre-Android 12)
 */
private fun defaultDarkColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        
        background = VibeBackground,
        onBackground = Color(0xFFE6E1E5),
        surface = VibeSurface,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = VibeSurfaceContainer,
        onSurfaceVariant = Color(0xFFCAC4D0),
        
        error = ErrorColor,
        onError = OnErrorColor,
    )
}

// Helper extension for Color compositing
private fun Color.compositeOver(background: Color): Color {
    return Color(
        red = red * alpha + background.red * (1 - alpha),
        green = green * alpha + background.green * (1 - alpha),
        blue = blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}

@Composable
fun rememberBitmapFromUrl(url: String?): Bitmap? {
    if (url == null) return null
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(url) {
        val loader = coil.ImageLoader(context)
        val request = coil.request.ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Palette needs software bitmap
            .build()
        val result = loader.execute(request)
        if (result is coil.request.SuccessResult) {
             bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        }
    }
    return bitmap
}
