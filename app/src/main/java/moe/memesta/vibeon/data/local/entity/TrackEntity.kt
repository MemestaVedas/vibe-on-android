package moe.memesta.vibeon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.security.MessageDigest

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["album", "artist"]),
        Index(value = ["canonicalId"])  // Index for deduplication queries
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,  // Unique per source (PC path or mobile cache path)
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val albumArtUrl: String?,
    val albumMainColor: Int? = null,
    val year: Int?,
    val genre: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val titleRomaji: String? = null,
    val titleEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null,
    val albumRomaji: String? = null,
    val albumEn: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Deduplication fields
    val source: String = "pc",  // "pc" or "mobile"
    val canonicalId: String = generateCanonicalId(title, artist, album),  // Hash for dedup
    val localPath: String? = null  // Path on device if downloaded/cached
) {
    companion object {
        /**
         * Generate a canonical ID from track metadata.
         * Same track (same title+artist+album) gets same ID regardless of source.
         */
        fun generateCanonicalId(title: String, artist: String, album: String): String {
            val normalized = "$title|$artist|$album".lowercase().trim()
            val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
        }
    }
}

