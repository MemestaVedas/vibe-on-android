package moe.memesta.vibeon.ui.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

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
     * Extracts vibrant and muted colors from a Coil Drawable.
     */
    fun extractColors(drawable: Drawable?): ThemeColors {
        if (drawable == null || drawable !is BitmapDrawable) {
            return ThemeColors()
        }
        
        var bitmap = drawable.bitmap
        
        // Handle Hardware Bitmaps (Android 8+)
        // Palette cannot access pixels of hardware bitmaps directly
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
            bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        
        val palette = Palette.from(bitmap).generate()
        
        val vibrant = palette.vibrantSwatch?.let { Color(it.rgb) } ?: palette.darkVibrantSwatch?.let { Color(it.rgb) } ?: Color.Transparent
        val muted = palette.mutedSwatch?.let { Color(it.rgb) } ?: palette.darkMutedSwatch?.let { Color(it.rgb) } ?: Color.Transparent
        
        val onVibrant = palette.vibrantSwatch?.let { Color(it.titleTextColor) } ?: Color.White
        val onMuted = palette.mutedSwatch?.let { Color(it.titleTextColor) } ?: Color.White
        
        return ThemeColors(
            vibrant = vibrant,
            muted = muted,
            onVibrant = onVibrant,
            onMuted = onMuted
        )
    }
}
