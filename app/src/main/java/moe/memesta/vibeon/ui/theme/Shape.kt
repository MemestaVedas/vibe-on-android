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

val PlaylistCoverShape: Shape = RoundedCornerShape(24.dp) // Stadium/Pill shape for playlists

val ArtistCoverShape: Shape = CircleShape // Circle shape for artists

val DomeShape: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp) // Dome shape for areas
