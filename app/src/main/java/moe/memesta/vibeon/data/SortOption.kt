package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable

@Immutable
sealed class SortOption(val storageKey: String, val displayName: String) {
    // Album Sort Options
    object AlbumDefault : SortOption("album_default", "Default")
    object AlbumTitleAZ : SortOption("album_title_az", "Title (A-Z)")
    object AlbumTitleZA : SortOption("album_title_za", "Title (Z-A)")
    object AlbumArtist : SortOption("album_artist", "Artist")
    object AlbumSongCountAsc : SortOption("album_song_count_asc", "Fewest Songs")
    object AlbumSongCountDesc : SortOption("album_song_count_desc", "Most Songs")
    
    // Track Sort Options
    object TrackDefault : SortOption("track_default", "Default")
    object TrackTitleAZ : SortOption("track_title_az", "Title (A-Z)")
    object TrackTitleZA : SortOption("track_title_za", "Title (Z-A)")
    object TrackDurationAsc : SortOption("track_duration_asc", "Duration (Low-High)")
    object TrackDurationDesc : SortOption("track_duration_desc", "Duration (High-Low)")
    
    // Playlist Sort Options
    object PlaylistDefault : SortOption("playlist_default", "Default")
    object PlaylistNameAZ : SortOption("playlist_name_az", "Name (A-Z)")
    object PlaylistNameZA : SortOption("playlist_name_za", "Name (Z-A)")
    object PlaylistTrackCountAsc : SortOption("playlist_track_count_asc", "Fewest Tracks")
    object PlaylistTrackCountDesc : SortOption("playlist_track_count_desc", "Most Tracks")

    companion object {
        val ALBUMS: List<SortOption> by lazy {
            listOf(
                AlbumDefault,
                AlbumTitleAZ,
                AlbumTitleZA,
                AlbumArtist,
                AlbumSongCountDesc,
                AlbumSongCountAsc
            )
        }
        
        val TRACKS: List<SortOption> by lazy {
            listOf(
                TrackDefault,
                TrackTitleAZ,
                TrackTitleZA,
                TrackDurationAsc,
                TrackDurationDesc
            )
        }
        
        val PLAYLISTS: List<SortOption> by lazy {
            listOf(
                PlaylistDefault,
                PlaylistNameAZ,
                PlaylistNameZA,
                PlaylistTrackCountDesc,
                PlaylistTrackCountAsc
            )
        }

        fun fromStorageKey(key: String): SortOption {
            return when (key) {
                "album_default" -> AlbumDefault
                "album_title_az" -> AlbumTitleAZ
                "album_title_za" -> AlbumTitleZA
                "album_artist" -> AlbumArtist
                "album_song_count_asc" -> AlbumSongCountAsc
                "album_song_count_desc" -> AlbumSongCountDesc
                "track_default" -> TrackDefault
                "track_title_az" -> TrackTitleAZ
                "track_title_za" -> TrackTitleZA
                "track_duration_asc" -> TrackDurationAsc
                "track_duration_desc" -> TrackDurationDesc
                "playlist_default" -> PlaylistDefault
                "playlist_name_az" -> PlaylistNameAZ
                "playlist_name_za" -> PlaylistNameZA
                "playlist_track_count_asc" -> PlaylistTrackCountAsc
                "playlist_track_count_desc" -> PlaylistTrackCountDesc
                else -> AlbumDefault
            }
        }
    }
}
