package moe.memesta.vibeon.ui.utils

import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot

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

    fun colorsFromMainColor(mainColor: Int?): ThemeColors {
        if (mainColor == null) return ThemeColors()

        val hct = Hct.fromInt(mainColor)
        val scheme = SchemeTonalSpot(hct, true, 0.0)
        return ThemeColors(
            vibrant = Color(scheme.primaryPalette.tone(80)),
            muted = Color(scheme.secondaryPalette.tone(80)),
            onVibrant = Color(scheme.primaryPalette.tone(20)),
            onMuted = Color(scheme.secondaryPalette.tone(20))
        )
    }
}
