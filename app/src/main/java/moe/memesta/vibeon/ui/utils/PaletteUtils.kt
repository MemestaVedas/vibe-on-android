package moe.memesta.vibeon.ui.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.*

/**
 * Data class to hold extracted theme colors.
 */
data class ThemeColors(
    val vibrant: Color = Color.Transparent,
    val muted: Color = Color.Transparent,
    val onVibrant: Color = Color.White,
    val onMuted: Color = Color.White
)

object PaletteUtils {
    
    /**
     * Extracts vibrant and muted colors using MCU from a Coil Drawable.
     */
    fun extractColors(drawable: Drawable?): ThemeColors {
        if (drawable == null || drawable !is BitmapDrawable) {
            return ThemeColors()
        }
        
        var bitmap = drawable.bitmap
        
        // Handle Hardware Bitmaps (Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
            bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        
        // Match logic in DynamicTheme.kt
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled != bitmap) scaled.recycle()

        val quantized = QuantizerCelebi.quantize(pixels, 128)
        val scored = Score.score(quantized)
        val sourceColor = if (scored.isNotEmpty()) scored[0] else 0xFF121113.toInt()

        val hct = Hct.fromInt(sourceColor)
        val scheme = SchemeTonalSpot(hct, true, 0.0)
        
        val vibrant = Color(scheme.primaryPalette.tone(80))
        val muted = Color(scheme.secondaryPalette.tone(80))
        
        val onVibrant = Color(scheme.primaryPalette.tone(20))
        val onMuted = Color(scheme.secondaryPalette.tone(20))
        
        return ThemeColors(
            vibrant = vibrant,
            muted = muted,
            onVibrant = onVibrant,
            onMuted = onMuted
        )
    }
}
