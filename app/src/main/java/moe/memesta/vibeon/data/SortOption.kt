package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
sealed class SortOption(val storageKey: String, val displayName: String) {
    // Album Sort Options
    object AlbumTitleAZ : SortOption("album_title_az", "Title (A-Z)")
    object AlbumTitleZA : SortOption("album_title_za", "Title (Z-A)")
    object AlbumArtist : SortOption("album_artist", "Artist")
    object AlbumSongCountAsc : SortOption("album_song_count_asc", "Fewest Songs")
    object AlbumSongCountDesc : SortOption("album_song_count_desc", "Most Songs")
    
    // Track Sort Options
    object TrackTitleAZ : SortOption("track_title_az", "Title (A-Z)")
    object TrackTitleZA : SortOption("track_title_za", "Title (Z-A)")
    object TrackDurationAsc : SortOption("track_duration_asc", "Duration (Low-High)")
    object TrackDurationDesc : SortOption("track_duration_desc", "Duration (High-Low)")

    companion object {
        val ALBUMS: List<SortOption> by lazy {
            listOf(
                AlbumTitleAZ,
                AlbumTitleZA,
                AlbumArtist,
                AlbumSongCountDesc,
                AlbumSongCountAsc
            )
        }
        
        val TRACKS: List<SortOption> by lazy {
            listOf(
                TrackTitleAZ,
                TrackTitleZA,
                TrackDurationAsc,
                TrackDurationDesc
            )
        }

        fun fromStorageKey(key: String): SortOption {
            return when (key) {
                "album_title_az" -> AlbumTitleAZ
                "album_title_za" -> AlbumTitleZA
                "album_artist" -> AlbumArtist
                "album_song_count_asc" -> AlbumSongCountAsc
                "album_song_count_desc" -> AlbumSongCountDesc
                "track_title_az" -> TrackTitleAZ
                "track_title_za" -> TrackTitleZA
                "track_duration_asc" -> TrackDurationAsc
                "track_duration_desc" -> TrackDurationDesc
                else -> AlbumTitleAZ
            }
        }
    }
}
