package moe.memesta.vibeon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["album", "artist"])]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val albumArtUrl: String?,
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
    val lastUpdated: Long = System.currentTimeMillis()
)
