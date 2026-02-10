package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumInfo(
    val name: String,
    val artist: String,
    val coverUrl: String?
)

@Immutable
data class ArtistItemData(
    val name: String,
    val followerCount: String,
    val photoUrl: String?
)

@Immutable
data class LibraryStats(
    val totalSongs: Int,
    val totalAlbums: Int,
    val totalArtists: Int,
    val totalDurationHours: Double
)
