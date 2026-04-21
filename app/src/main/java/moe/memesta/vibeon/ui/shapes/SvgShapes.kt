@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package moe.memesta.vibeon.ui.shapes

import androidx.compose.material3.MaterialShapes

/**
 * Legacy semantic aliases rewritten to use Material3 Expressive shapes directly.
 *
 * Keep the existing call sites stable while swapping the underlying geometry
 * to the new shape system.
 */
val AlbumSquircleShape = MaterialShapes.Cookie9Sided

val AlbumsShape = MaterialShapes.Cookie12Sided

val PlaylistsShape = MaterialShapes.Pill

val ArtistsShape = MaterialShapes.Circle

val SettingsShape = MaterialShapes.VerySunny

val AlbumArtStarShape = MaterialShapes.SoftBurst

val ArrowBlobShape = MaterialShapes.Arrow