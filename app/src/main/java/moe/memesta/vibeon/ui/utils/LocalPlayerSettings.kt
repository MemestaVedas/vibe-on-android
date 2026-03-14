package moe.memesta.vibeon.ui.utils

import androidx.compose.runtime.compositionLocalOf
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.data.local.LibraryViewStyle

val LocalDisplayLanguage = compositionLocalOf { DisplayLanguage.ORIGINAL }
val LocalAlbumViewStyle = compositionLocalOf { LibraryViewStyle.MODERN }
val LocalArtistViewStyle = compositionLocalOf { LibraryViewStyle.MODERN }
