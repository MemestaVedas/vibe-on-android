@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package moe.memesta.vibeon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.R

/**
 * Unified typography family across mobile and desktop surfaces.
 */

val GoogleSansFlexFamily = FontFamily(
    Font(resId = R.font.google_sans_flex)
)

val MPlusRoundedFont = GoogleSansFlexFamily

// Dedicated alias for the Now Playing expressive title system.
val MPlus1pRoundedFamily = GoogleSansFlexFamily

fun googleSansFlexSettings(
    weight: Int = 400,
    width: Float = 100f,
    opticalSize: Float = 14f,
    grade: Float = 0f,
    roundness: Float = 0f
): String = "'wght' ${weight.toFloat()}, 'wdth' $width, 'opsz' $opticalSize, 'GRAD' $grade, 'ROND' $roundness"

fun TextStyle.withGoogleSansAxes(settings: String): TextStyle =
    copy(fontFeatureSettings = settings)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle( // Used for track titles
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle( // Used for artist names
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MPlusRoundedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
