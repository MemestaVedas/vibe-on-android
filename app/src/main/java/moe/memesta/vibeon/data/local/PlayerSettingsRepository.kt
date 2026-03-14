package moe.memesta.vibeon.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DisplayLanguage(val value: String) {
    ORIGINAL("original"),
    ROMAJI("romaji"),
    ENGLISH("english");

    companion object {
        fun fromString(value: String): DisplayLanguage {
            return entries.find { it.value == value } ?: ORIGINAL
        }
    }
}

enum class LibraryViewStyle(val value: String) {
    LEGACY("legacy"),
    MODERN("modern");

    companion object {
        fun fromString(value: String): LibraryViewStyle {
            return entries.find { it.value == value } ?: MODERN
        }
    }
}

class PlayerSettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_settings",
        Context.MODE_PRIVATE
    )

    private val _displayLanguage = MutableStateFlow(
        DisplayLanguage.fromString(prefs.getString(KEY_DISPLAY_LANGUAGE, DisplayLanguage.ORIGINAL.value)!!)
    )
    val displayLanguage: StateFlow<DisplayLanguage> = _displayLanguage.asStateFlow()

    private val _albumViewStyle = MutableStateFlow(
        LibraryViewStyle.fromString(prefs.getString(KEY_ALBUM_VIEW_STYLE, LibraryViewStyle.MODERN.value)!!)
    )
    val albumViewStyle: StateFlow<LibraryViewStyle> = _albumViewStyle.asStateFlow()

    private val _artistViewStyle = MutableStateFlow(
        LibraryViewStyle.fromString(prefs.getString(KEY_ARTIST_VIEW_STYLE, LibraryViewStyle.MODERN.value)!!)
    )
    val artistViewStyle: StateFlow<LibraryViewStyle> = _artistViewStyle.asStateFlow()

    fun setDisplayLanguage(language: DisplayLanguage) {
        prefs.edit().putString(KEY_DISPLAY_LANGUAGE, language.value).apply()
        _displayLanguage.value = language
    }

    fun setAlbumViewStyle(style: LibraryViewStyle) {
        prefs.edit().putString(KEY_ALBUM_VIEW_STYLE, style.value).apply()
        _albumViewStyle.value = style
    }

    fun setArtistViewStyle(style: LibraryViewStyle) {
        prefs.edit().putString(KEY_ARTIST_VIEW_STYLE, style.value).apply()
        _artistViewStyle.value = style
    }

    companion object {
        private const val KEY_DISPLAY_LANGUAGE = "display_language"
        private const val KEY_ALBUM_VIEW_STYLE = "album_view_style"
        private const val KEY_ARTIST_VIEW_STYLE = "artist_view_style"
    }
}
