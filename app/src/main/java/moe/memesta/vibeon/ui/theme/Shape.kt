package moe.memesta.vibeon.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.ui.components.SvgShape

/**
 * Material 3 Shapes with full roundness for Vibe-On!
 * - Small: Pill shapes (50% roundness) for buttons and chips
 * - Medium: Rounded cards (24dp) for track items and album cards (Squircle)
 * - Large: Extra rounded (32dp) for dialogs and bottom sheets
 */
val Shapes = Shapes(
    // Pill shapes for buttons, chips, and FABs
    small = RoundedCornerShape(percent = 50),
    
    // Rounded cards for track items, album grid items (Squircle)
    medium = RoundedCornerShape(24.dp),
    
    // Extra rounded for dialogs, bottom sheets, and large surfaces
    large = RoundedCornerShape(32.dp)
)

// Custom Shapes for Content
val SongCoverShape: Shape = CircleShape // Circle for songs

val PlaylistCoverShape: Shape = SvgShape(
    pathString = "M0 81.36C0 36.42 36.42 0 81.36 0H228.64C273.58 0 310 36.42 310 81.36C310 118.41 285.23 149.68 251.34 159.5C251.12 159.57 250.97 159.77 250.97 160C250.97 160.23 251.12 160.43 251.34 160.5C285.23 170.32 310 201.59 310 238.64C310 283.58 273.58 320 228.64 320H81.36C36.42 320 0 283.58 0 238.64C0 201.83 24.45 170.73 58 160.69C58.3 160.6 58.51 160.32 58.51 160C58.51 159.68 58.3 159.4 58 159.31C24.45 149.27 0 118.17 0 81.36Z",
    viewportWidth = 310f,
    viewportHeight = 320f
) // Stadium/Pill shape for playlists

val ArtistCoverShape: Shape = SvgShape(
    pathString = "M304 253.72C304 259.83 304 262.89 303.69 265.46C301.31 285.51 285.51 301.31 265.46 303.69C262.89 304 259.83 304 253.72 304H50.281C44.169 304 41.113 304 38.544 303.69C18.495 301.31 2.68799 285.51 0.304993 265.46C-7.33137e-06 262.89 0 259.83 0 253.72V152C0 68.05 68.053 0 152 0C235.95 0 304 68.05 304 152V253.72Z",
    viewportWidth = 304f,
    viewportHeight = 304f
) // Arch shape for artists

val DomeShape: Shape = SvgShape(
    pathString = "M330 160C330 248.366 258.366 320 170 320C81.6344 320 10 248.366 10 160C10 71.6344 81.6345 0 170 0C258.366 0 330 71.6345 330 160Z",
    viewportWidth = 330f,
    viewportHeight = 320f
) // Dome shape for carouselIndicators
