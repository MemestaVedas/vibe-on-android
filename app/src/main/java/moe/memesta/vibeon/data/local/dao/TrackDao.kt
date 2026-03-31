package moe.memesta.vibeon.data.local

import androidx.room.*
import androidx.paging.PagingSource
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

    @Query(
        """
        SELECT t.*
        FROM tracks t
        WHERE t.id = (
            SELECT t2.id
            FROM tracks t2
            WHERE t2.canonicalId = t.canonicalId
            ORDER BY
                CASE WHEN t2.source = 'pc' THEN 0 ELSE 1 END,
                t2.lastUpdated DESC,
                t2.id ASC
            LIMIT 1
        )
        AND (
            :query = ''
            OR t.title LIKE '%' || :query || '%'
            OR t.artist LIKE '%' || :query || '%'
            OR t.album LIKE '%' || :query || '%'
            OR COALESCE(t.titleRomaji, '') LIKE '%' || :query || '%'
            OR COALESCE(t.artistRomaji, '') LIKE '%' || :query || '%'
            OR COALESCE(t.albumRomaji, '') LIKE '%' || :query || '%'
            OR COALESCE(t.titleEn, '') LIKE '%' || :query || '%'
            OR COALESCE(t.artistEn, '') LIKE '%' || :query || '%'
            OR COALESCE(t.albumEn, '') LIKE '%' || :query || '%'
        )
        ORDER BY
            CASE WHEN :sortKey = 'track_title_az' THEN lower(t.title) END ASC,
            CASE WHEN :sortKey = 'track_title_za' THEN lower(t.title) END DESC,
            CASE WHEN :sortKey = 'track_duration_asc' THEN t.duration END ASC,
            CASE WHEN :sortKey = 'track_duration_desc' THEN t.duration END DESC,
            CASE WHEN :sortKey = 'track_default' THEN lower(t.artist) END ASC,
            CASE WHEN :sortKey = 'track_default' THEN lower(t.album) END ASC,
            CASE WHEN :sortKey = 'track_default' THEN COALESCE(t.trackNumber, 0) END ASC,
            lower(t.title) ASC
    """
    )
    fun getTracksDedupedPaging(
        query: String,
        sortKey: String
    ): PagingSource<Int, TrackEntity>

    // ═══════════════════════════════════════════════════════════════
    // Deduplication queries
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get all tracks grouped by canonicalId (dedup view).
     * Returns one entry per unique track (by title+artist+album).
     */
    @Query("""
        SELECT t.*
        FROM tracks t
        WHERE t.id = (
            SELECT t2.id
            FROM tracks t2
            WHERE t2.canonicalId = t.canonicalId
            ORDER BY
                CASE WHEN t2.source = 'pc' THEN 0 ELSE 1 END,
                t2.lastUpdated DESC,
                t2.id ASC
            LIMIT 1
        )
        ORDER BY t.artist, t.album, t.trackNumber
    """)
    fun getTracksDeduped(): Flow<List<TrackEntity>>
    
    /**
     * Get all sources for a track (by canonicalId).
     * Returns all versions: PC + mobile downloads, etc.
     */
    @Query("SELECT * FROM tracks WHERE canonicalId = :canonicalId ORDER BY source DESC, lastUpdated DESC")
    suspend fun getTrackSources(canonicalId: String): List<TrackEntity>
    
    /**
     * Get duplicates (same canonicalId from different sources).
     */
    @Query("""
        SELECT canonicalId, COUNT(*) as count FROM tracks 
        GROUP BY canonicalId 
        HAVING count > 1
        ORDER BY count DESC
    """)
    suspend fun getDuplicateGroupIds(): List<DuplicateGroup>
    
    /**
     * Get all tracks with local paths (downloaded/cached).
     */
    @Query("SELECT * FROM tracks WHERE localPath IS NOT NULL ORDER BY artist, album, trackNumber")
    fun getOfflineTracks(): Flow<List<TrackEntity>>
    
    /**
     * Get tracks by source (pc or mobile).
     */
    @Query("SELECT * FROM tracks WHERE source = :source ORDER BY artist, album, trackNumber")
    fun getTracksBySource(source: String): Flow<List<TrackEntity>>
    
    /**
     * Merge tracks during sync: update canonical IDs and prefer PC metadata.
     */
    suspend fun mergeTracksByMetadata(newTracks: List<TrackEntity>) {
        newTracks.forEach { newTrack ->
            // Find if track with same metadata exists
            val existingTracks = getTrackSources(newTrack.canonicalId)
            if (existingTracks.isNotEmpty()) {
                // Prefer PC version for metadata
                val preferredTrack = existingTracks.find { it.source == "pc" } ?: existingTracks.first()
                // Keep localPath if mobile has it
                val mergedTrack = preferredTrack.copy(
                    id = preferredTrack.id,  // Keep original ID
                    localPath = newTrack.localPath ?: preferredTrack.localPath,
                    canonicalId = preferredTrack.canonicalId
                )
                insertTrack(mergedTrack)
            } else {
                insertTrack(newTrack)
            }
        }
    }
}

data class DuplicateGroup(
    @androidx.room.ColumnInfo(name = "canonicalId") val canonicalId: String,
    @androidx.room.ColumnInfo(name = "count") val count: Int
)
