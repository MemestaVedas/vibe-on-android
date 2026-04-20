package moe.memesta.vibeon.ui.navigation

import java.net.URLEncoder
import kotlin.text.Charsets
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute {
    const val path: String = "main"
}

@Serializable
data object DiscoveryRoute {
    const val path: String = "discovery"
}

@Serializable
data object PairingRoute {
    const val path: String = "pairing"
}

@Serializable
data object NowPlayingRoute {
    const val path: String = "now_playing"
}

@Serializable
data object LibraryRoute {
    const val path: String = "library"
}

@Serializable
data object OfflineSongsRoute {
    const val path: String = "offline_songs"
}

@Serializable
data object PlaylistsRoute {
    const val path: String = "playlists"
}

@Serializable
data object SearchRoute {
    const val path: String = "search"
}

@Serializable
data class AlbumRoute(val albumName: String) {
    fun toPath(): String = "album/${albumName.urlEncode()}"
}

@Serializable
data class ArtistRoute(val artistName: String) {
    fun toPath(): String = "artist/${artistName.urlEncode()}"
}

@Serializable
data class PlaylistRoute(val playlistId: String) {
    fun toPath(): String = "playlist/${playlistId.urlEncode()}"
}

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
