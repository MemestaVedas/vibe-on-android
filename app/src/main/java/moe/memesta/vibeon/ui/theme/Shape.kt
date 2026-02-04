package moe.memesta.vibeon.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shapes with full roundness for Vibe-On!
 * - Small: Pill shapes (50% roundness) for buttons and chips
 * - Medium: Rounded cards (24dp) for track items and album cards
 * - Large: Extra rounded (32dp) for dialogs and bottom sheets
 */
val Shapes = Shapes(
    // Pill shapes for buttons, chips, and FABs
    small = RoundedCornerShape(percent = 50),
    
    // Rounded cards for track items, album grid items
    medium = RoundedCornerShape(24.dp),
    
    // Extra rounded for dialogs, bottom sheets, and large surfaces
    large = RoundedCornerShape(32.dp)
)
