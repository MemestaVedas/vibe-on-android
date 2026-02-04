package moe.memesta.vibeon.data

data class AlbumInfo(
    val name: String,
    val artist: String,
    val coverUrl: String?
)

data class ArtistItemData(
    val name: String,
    val followerCount: String,
    val photoUrl: String?
)
