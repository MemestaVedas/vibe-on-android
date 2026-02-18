package moe.memesta.vibeon.ui.utils

import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.QueueItem
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.data.local.TrackEntity

fun TrackInfo.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> titleRomaji ?: title
        DisplayLanguage.ENGLISH -> titleEn ?: title
        else -> title
    }
}

fun TrackInfo.getDisplayArtist(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> artistRomaji ?: artist
        DisplayLanguage.ENGLISH -> artistEn ?: artist
        else -> artist
    }
}

fun TrackInfo.getDisplayAlbum(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> albumRomaji ?: album
        DisplayLanguage.ENGLISH -> albumEn ?: album
        else -> album
    }
}

// MediaSessionData
fun MediaSessionData.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> titleRomaji ?: title
        DisplayLanguage.ENGLISH -> titleEn ?: title
        else -> title
    }
}

fun MediaSessionData.getDisplayArtist(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> artistRomaji ?: artist
        DisplayLanguage.ENGLISH -> artistEn ?: artist
        else -> artist
    }
}

fun MediaSessionData.getDisplayAlbum(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> albumRomaji ?: album
        DisplayLanguage.ENGLISH -> albumEn ?: album
        else -> album
    }
}

// QueueItem
fun QueueItem.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> titleRomaji ?: title
        DisplayLanguage.ENGLISH -> titleEn ?: title
        else -> title
    }
}

fun QueueItem.getDisplayArtist(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> artistRomaji ?: artist
        DisplayLanguage.ENGLISH -> artistEn ?: artist
        else -> artist
    }
}

fun QueueItem.getDisplayAlbum(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> albumRomaji ?: album
        DisplayLanguage.ENGLISH -> albumEn ?: album
        else -> album
    }
}

// TrackEntity
fun TrackEntity.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> titleRomaji ?: title
        DisplayLanguage.ENGLISH -> titleEn ?: title
        else -> title
    }
}

fun TrackEntity.getDisplayArtist(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> artistRomaji ?: artist
        DisplayLanguage.ENGLISH -> artistEn ?: artist
        else -> artist
    }
}

fun TrackEntity.getDisplayAlbum(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> albumRomaji ?: album
        DisplayLanguage.ENGLISH -> albumEn ?: album
        else -> album
    }
}

// AlbumInfo Helpers
fun AlbumInfo.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> nameRomaji ?: name
        DisplayLanguage.ENGLISH -> nameEn ?: name
        else -> name
    }
}

fun AlbumInfo.getDisplayArtist(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> artistRomaji ?: artist
        DisplayLanguage.ENGLISH -> artistEn ?: artist
        else -> artist
    }
}

// ArtistItemData Helpers
fun ArtistItemData.getDisplayName(language: DisplayLanguage): String {
    return when (language) {
        DisplayLanguage.ROMAJI -> nameRomaji ?: name
        DisplayLanguage.ENGLISH -> nameEn ?: name
        else -> name
    }
}
