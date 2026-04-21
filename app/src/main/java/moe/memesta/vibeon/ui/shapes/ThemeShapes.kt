@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package moe.memesta.vibeon.ui.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Expressive Material 3 shape aliases for reusable surfaces.
 */
val Shapes = Shapes(
    extraSmall = ShapeCache.rounded10,
    small = ShapeCache.rounded16,
    medium = ShapeCache.rounded24,
    large = ShapeCache.rounded34,
    extraLarge = ShapeCache.rounded42
)

// --- Semantic Shapes for Content ---

val SongCoverShape = MaterialShapes.Circle // Circle for songs

val PlaylistCoverShape = MaterialShapes.Pill // Stadium/Pill shape for playlists

val ArtistCoverShape = MaterialShapes.Arch // Arch shape for artists

val DomeShape = MaterialShapes.Arch // Dome shape for areas

// ─── Futuristic Kinetic semantic aliases ─────────────────────────────────────

/** Full pill for the OrbitButton and FluxPill toggle chips. */
val PillButtonShape = MaterialShapes.Pill

/**
 * Asymmetric prism shape for PrismIconButton and secondary controls.
 * Opposite corners are sized differently to convey kinetic energy.
 */
val PrismButtonShape = MaterialShapes.Slanted

/** Slightly squircle card — main surface for album/track cards. */
val ElevatedCardShape = MaterialShapes.Cookie9Sided

/** Full-bleed bottom sheet top rounding. */
val SheetShape = MaterialShapes.Cookie12Sided

/** Compact chip/badge rounding. */
val ChipShape = MaterialShapes.Pill
