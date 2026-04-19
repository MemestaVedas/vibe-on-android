package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
data class ArtistItemData(
    val name: String,
    val followerCount: String,
    val photoUrl: String?,
    val mainColor: Int? = null,
    val nameRomaji: String? = null,
    val nameEn: String? = null
)
