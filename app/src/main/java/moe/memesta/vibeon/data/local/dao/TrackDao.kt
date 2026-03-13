package moe.memesta.vibeon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY artist, album, trackNumber")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Query("SELECT DISTINCT album FROM tracks ORDER BY album")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM tracks ORDER BY artist")
    fun getAllArtists(): Flow<List<String>>
    
    @Query("SELECT * FROM tracks WHERE album = :albumName ORDER BY trackNumber")
    fun getTracksByAlbum(albumName: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artistName ORDER BY album, trackNumber")
    fun getTracksByArtist(artistName: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT * FROM tracks ORDER BY artist, album, trackNumber LIMIT :limit OFFSET :offset")
    suspend fun getTracksPage(limit: Int, offset: Int): List<TrackEntity>
}
