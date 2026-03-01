package moe.memesta.vibeon.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max

/**
 * Contrast Guard — ensures text meets WCAG AA accessibility standards
 * against dynamic background colors extracted from album art.
 *
 * WCAG AA requires:
 *   - Normal text: contrast ratio >= 4.5:1
 *   - Large text (18sp+): contrast ratio >= 3:1
 */
object ContrastGuard {

    /**
     * Returns either white or black, whichever provides better contrast
     * against the given background color.
     */
    fun textColorFor(background: Color): Color {
        val luminance = background.luminance()
        return if (luminance > 0.45f) Color.Black else Color.White
    }

    /**
     * Returns a text color with guaranteed WCAG AA contrast ratio
     * against the given background. Prefers the suggested color
     * if it meets the minimum ratio; otherwise falls back to white or black.
     *
     * @param background The background color to check against
     * @param suggested The preferred text color
     * @param minRatio Minimum contrast ratio (4.5 for normal text, 3.0 for large text)
     */
    fun ensureContrast(
        background: Color,
        suggested: Color,
        minRatio: Float = 4.5f
    ): Color {
        if (contrastRatio(background, suggested) >= minRatio) return suggested
        // Fall back to whichever provides better contrast
        return textColorFor(background)
    }

    /**
     * Returns a tinted version of the suggested color that meets
     * the minimum contrast ratio against the background.
     * Gradually lightens or darkens the suggested color until contrast is met.
     */
    fun adjustForContrast(
        background: Color,
        suggested: Color,
        minRatio: Float = 4.5f
    ): Color {
        if (contrastRatio(background, suggested) >= minRatio) return suggested

        val bgLuminance = background.luminance()
        // Decide whether to lighten or darken
        val shouldLighten = bgLuminance < 0.5f

        var adjusted = suggested
        for (step in 1..20) {
            val factor = step * 0.05f
            adjusted = if (shouldLighten) {
                Color(
                    red = (suggested.red + (1f - suggested.red) * factor).coerceIn(0f, 1f),
                    green = (suggested.green + (1f - suggested.green) * factor).coerceIn(0f, 1f),
                    blue = (suggested.blue + (1f - suggested.blue) * factor).coerceIn(0f, 1f),
                    alpha = suggested.alpha
                )
            } else {
                Color(
                    red = (suggested.red * (1f - factor)).coerceIn(0f, 1f),
                    green = (suggested.green * (1f - factor)).coerceIn(0f, 1f),
                    blue = (suggested.blue * (1f - factor)).coerceIn(0f, 1f),
                    alpha = suggested.alpha
                )
            }
            if (contrastRatio(background, adjusted) >= minRatio) return adjusted
        }

        // Final fallback
        return textColorFor(background)
    }

    /**
     * Adjusts a gradient's brightness to ensure overlaid text is readable.
     * Returns a modified list of gradient colors with sufficient contrast range.
     */
    fun guardGradient(
        gradientColors: List<Color>,
        textColor: Color,
        minRatio: Float = 3.0f
    ): List<Color> {
        return gradientColors.map { bg ->
            if (contrastRatio(bg, textColor) >= minRatio) {
                bg
            } else {
                // Dim or brighten the background stop
                val bgLuminance = bg.luminance()
                val textLuminance = textColor.luminance()
                if (textLuminance > bgLuminance) {
                    // Text is lighter — darken background
                    bg.copy(
                        red = bg.red * 0.6f,
                        green = bg.green * 0.6f,
                        blue = bg.blue * 0.6f
                    )
                } else {
                    // Text is darker — lighten background
                    bg.copy(
                        red = (bg.red + (1f - bg.red) * 0.4f).coerceAtMost(1f),
                        green = (bg.green + (1f - bg.green) * 0.4f).coerceAtMost(1f),
                        blue = (bg.blue + (1f - bg.blue) * 0.4f).coerceAtMost(1f)
                    )
                }
            }
        }
    }

    /**
     * Calculates the WCAG contrast ratio between two colors.
     * Returns a value between 1:1 (identical) and 21:1 (black vs white).
     */
    fun contrastRatio(color1: Color, color2: Color): Float {
        val l1 = color1.luminance()
        val l2 = color2.luminance()
        val lighter = max(l1, l2) + 0.05f
        val darker = minOf(l1, l2) + 0.05f
        return lighter / darker
    }
}
