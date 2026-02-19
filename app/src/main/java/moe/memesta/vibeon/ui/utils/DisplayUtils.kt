package moe.memesta.vibeon.ui.utils

import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.QueueItem
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.data.local.TrackEntity

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
