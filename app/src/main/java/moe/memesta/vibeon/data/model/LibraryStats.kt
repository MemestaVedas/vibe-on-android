package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
data class LibraryStats(
    val totalSongs: Int,
    val totalAlbums: Int,
    val totalArtists: Int,
    val totalDurationHours: Double
)
