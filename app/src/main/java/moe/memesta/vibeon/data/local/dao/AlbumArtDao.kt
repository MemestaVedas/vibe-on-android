package moe.memesta.vibeon.data.local

import androidx.room.*

@Dao
interface AlbumArtDao {
    @Query("SELECT * FROM album_art WHERE album = :album AND artist = :artist LIMIT 1")
    suspend fun getAlbumArt(album: String, artist: String): AlbumArtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumArt(albumArt: AlbumArtEntity)

    @Query("DELETE FROM album_art")
    suspend fun clearAll()
}