package moe.memesta.vibeon.ui.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Expressive Material 3 shape scale.
 *
 * This keeps a soft visual language but gives each size tier a distinct
 * personality so surfaces don't collapse into one radius everywhere.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(42.dp)
)

// --- Semantic Shapes for Content ---

val SongCoverShape: Shape = CircleShape // Circle for songs

val PlaylistCoverShape: Shape = RoundedCornerShape(24.dp) // Stadium/Pill shape for playlists

val ArtistCoverShape: Shape = CircleShape // Circle shape for artists

val DomeShape: Shape = RoundedCornerShape(
    topStart = 32.dp, 
    topEnd = 32.dp, 
    bottomStart = 0.dp, 
    bottomEnd = 0.dp
) // Dome shape for areas

// ─── Futuristic Kinetic semantic aliases ─────────────────────────────────────

/** Full pill for the OrbitButton and FluxPill toggle chips. */
val PillButtonShape: Shape = RoundedCornerShape(percent = 50)

/**
 * Asymmetric prism shape for PrismIconButton and secondary controls.
 * Opposite corners are sized differently to convey kinetic energy.
 */
val PrismButtonShape: Shape = RoundedCornerShape(
    topStart = 10.dp, topEnd = 18.dp,
    bottomStart = 18.dp, bottomEnd = 10.dp
)

/** Slightly squircle card — main surface for album/track cards. */
val ElevatedCardShape: Shape = RoundedCornerShape(20.dp)

/** Full-bleed bottom sheet top rounding. */
val SheetShape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Compact chip/badge rounding. */
val ChipShape: Shape = RoundedCornerShape(12.dp)
