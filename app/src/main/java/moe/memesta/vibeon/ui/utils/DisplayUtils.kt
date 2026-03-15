package moe.memesta.vibeon.ui.utils

import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.QueueItem
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.data.local.TrackEntity
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator

private fun pickText(vararg candidates: String?): String {
    return candidates.firstOrNull { !it.isNullOrBlank() } ?: ""
}

private fun selectByLanguage(
    language: DisplayLanguage,
    original: String?,
    romaji: String?,
    english: String?
): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> pickText(romaji, english, original)
        DisplayLanguage.ENGLISH -> pickText(english, romaji, original)
        else -> pickText(original, english, romaji)
    }
}

fun TrackInfo.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, title, titleRomaji, titleEn)
}

fun TrackInfo.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun TrackInfo.getDisplayAlbum(language: DisplayLanguage): String {
    return selectByLanguage(language, album, albumRomaji, albumEn)
}

// MediaSessionData
fun MediaSessionData.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, title, titleRomaji, titleEn)
}

fun MediaSessionData.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun MediaSessionData.getDisplayAlbum(language: DisplayLanguage): String {
    return selectByLanguage(language, album, albumRomaji, albumEn)
}

// QueueItem
fun QueueItem.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, title, titleRomaji, titleEn)
}

fun QueueItem.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun QueueItem.getDisplayAlbum(language: DisplayLanguage): String {
    return selectByLanguage(language, album, albumRomaji, albumEn)
}

// TrackEntity
fun TrackEntity.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, title, titleRomaji, titleEn)
}

fun TrackEntity.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun TrackEntity.getDisplayAlbum(language: DisplayLanguage): String {
    return selectByLanguage(language, album, albumRomaji, albumEn)
}

// AlbumInfo Helpers
fun AlbumInfo.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, name, nameRomaji, nameEn)
}

fun AlbumInfo.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

// ArtistItemData Helpers
fun ArtistItemData.getDisplayName(language: DisplayLanguage): String {
    return selectByLanguage(language, name, nameRomaji, nameEn)
}

fun PlaybackStatsCalculator.SongPlaybackSummary.getDisplayTitle(language: DisplayLanguage): String {
    return selectByLanguage(language, title, titleRomaji, titleEn)
}

fun PlaybackStatsCalculator.SongPlaybackSummary.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun PlaybackStatsCalculator.ArtistPlaybackSummary.getDisplayArtist(language: DisplayLanguage): String {
    return selectByLanguage(language, artist, artistRomaji, artistEn)
}

fun PlaybackStatsCalculator.AlbumPlaybackSummary.getDisplayAlbum(language: DisplayLanguage): String {
    return selectByLanguage(language, album, albumRomaji, albumEn)
}

// Album Normalization
data class ParsedAlbum(
    val baseName: String,
    val discInfo: String?,
    val discNumber: Int?
)

fun parseAlbum(albumName: String, discNum: Int?): ParsedAlbum {
    // Regex matching the web version: /^(.*?)(?:\\|\/|\s-\s|\s*[\(\[]\s*|\s+-\s+)(?:Disc|CD)\s*(\d+)(.*)$/i
    val regex = Regex("^(.*?)(?:\\\\|/|\\s-\\s|\\s*[\\(\\[]\\s*|\\s+-\\s+)(?:Disc|CD)\\s*(\\d+)(.*)$", RegexOption.IGNORE_CASE)
    val match = regex.find(albumName)
    
    return if (match != null) {
        val baseName = match.groupValues[1].trim()
        val num = match.groupValues[2].toIntOrNull() ?: discNum
        ParsedAlbum(
            baseName = baseName,
            discInfo = "Disc $num",
            discNumber = num
        )
    } else {
        ParsedAlbum(
            baseName = albumName.trim(),
            discInfo = discNum?.let { "Disc $it" },
            discNumber = discNum
        )
    }
}
