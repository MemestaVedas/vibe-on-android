package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumInfo(
    val name: String,
    val artist: String,
    val coverUrl: String?,
    val albumMainColor: Int? = null,
    val songCount: Int = 0,
    val nameRomaji: String? = null,
    val nameEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null
)
