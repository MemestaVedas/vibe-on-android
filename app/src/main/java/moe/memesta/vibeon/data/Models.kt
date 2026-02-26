package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumInfo(
    val name: String,
    val artist: String,
    val coverUrl: String?,
    val songCount: Int = 0,
    val nameRomaji: String? = null,
    val nameEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null
)

@Immutable
data class ArtistItemData(
    val name: String,
    val followerCount: String,
    val photoUrl: String?,
    val nameRomaji: String? = null,
    val nameEn: String? = null
)

@Immutable
data class LibraryStats(
    val totalSongs: Int,
    val totalAlbums: Int,
    val totalArtists: Int,
    val totalDurationHours: Double
)
