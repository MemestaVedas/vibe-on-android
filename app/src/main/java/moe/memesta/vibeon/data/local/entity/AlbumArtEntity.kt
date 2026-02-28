package moe.memesta.vibeon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "album_art",
    indices = [Index(value = ["album", "artist"], unique = true)]
)
data class AlbumArtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val album: String,
    val artist: String,
    val artUrl: String?,
    val cachedPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
