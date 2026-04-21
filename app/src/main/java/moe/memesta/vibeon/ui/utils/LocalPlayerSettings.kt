package moe.memesta.vibeon.ui.utils

import androidx.compose.runtime.compositionLocalOf
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.data.local.LibraryViewStyle
import moe.memesta.vibeon.data.local.NowPlayingFontMode

val LocalDisplayLanguage = compositionLocalOf { DisplayLanguage.ORIGINAL }
val LocalAlbumViewStyle = compositionLocalOf { LibraryViewStyle.MODERN }
val LocalArtistViewStyle = compositionLocalOf { LibraryViewStyle.MODERN }
val LocalNowPlayingFontMode = compositionLocalOf { NowPlayingFontMode.AUTOMATIC }
