package moe.memesta.vibeon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.R

/**
 * Custom Font Families
 * - Syne: For display/heading text (bold, geometric)
 * - DM Sans: For body/UI text (clean, modern)
 * 
 * Note: Font files (.ttf) should be placed in res/font/ directory.
 * When font resources become available, uncomment the Font references below.
 * 
 * To add custom fonts:
 * 1. Download .ttf files for Syne and DM Sans
 * 2. Place them in res/font/ directory with names:
 *    - syne_regular.ttf, syne_medium.ttf, syne_semibold.ttf, syne_bold.ttf
 *    - dm_sans_regular.ttf, dm_sans_medium.ttf, dm_sans_semibold.ttf, dm_sans_bold.ttf
 * 3. Uncomment the Font() calls in SyneFont and DmSansFont below
 * 4. The typography will automatically use the custom fonts
 */

// Syne Font Family - for Headlines & Display text
// TODO: Uncomment Font references when .ttf files are added to res/font/
val SyneFont = FontFamily.Default
/* 
// Uncomment when font files are available:
val SyneFont = FontFamily(
    Font(R.font.syne_regular, FontWeight.Normal),
    Font(R.font.syne_medium, FontWeight.Medium),
    Font(R.font.syne_semibold, FontWeight.SemiBold),
    Font(R.font.syne_bold, FontWeight.Bold)
)
*/

// DM Sans Font Family - for Body & UI text
// TODO: Uncomment Font references when .ttf files are added to res/font/
val DmSansFont = FontFamily.Default
/*
// Uncomment when font files are available:
val DmSansFont = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)
*/

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SyneFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle( // Used for track titles
        fontFamily = DmSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle( // Used for artist names
        fontFamily = DmSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
