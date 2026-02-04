package moe.memesta.vibeon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.pow

// Vibe-On Design System Colors (Static)
val VibeBackground = Color(0xFF121113) // Night Owl Deep Charcoal - always consistent
val VibeSurface = Color(0xFF1E1C20)    // Tonal Surface
val VibeSurfaceContainer = Color(0xFF252329) // Slightly lighter surface

// Dynamic colors removed - will be generated from album art or wallpaper
// VibePrimary, VibeSecondary, etc. are now generated dynamically

// Utility: Generate tonal palette from seed color
fun generateTonalPalette(seedColor: Color): Map<Int, Color> {
    val hsl = seedColor.toHsl()
    return (0..100 step 10).associateWith { tone ->
        Color.hsl(hsl[0], hsl[1], tone / 100f, hsl[3])
    }
}

// Utility: Convert Color to HSL
fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    var h = when {
        delta == 0f -> 0f
        max == r -> 60 * (((g - b) / delta) % 6)
        max == g -> 60 * (((b - r) / delta) + 2)
        else -> 60 * (((r - g) / delta) + 4)
    }
    if (h < 0) h += 360
    
    val l = (max + min) / 2
    val s = if (delta == 0f) 0f else delta / (1 - kotlin.math.abs(2 * l - 1))
    
    return floatArrayOf(h, s, l, alpha)
}

// Utility: Ensure minimum luminance for text contrast
fun Color.ensureLuminance(minLuminance: Float): Color {
    return if (luminance() < minLuminance) {
        copy(
            red = (red * 1.2f).coerceAtMost(1f),
            green = (green * 1.2f).coerceAtMost(1f),
            blue = (blue * 1.2f).coerceAtMost(1f)
        )
    } else this
}

// Fallback colors for error states only
val ErrorColor = Color(0xFFB3261E)
val OnErrorColor = Color(0xFFFFFFFF)

data class NowPlayingColors(
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val containerColor: Color
)

