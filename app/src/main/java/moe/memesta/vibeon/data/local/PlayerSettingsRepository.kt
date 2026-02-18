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

class PlayerSettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_settings",
        Context.MODE_PRIVATE
    )

    private val _displayLanguage = MutableStateFlow(
        DisplayLanguage.fromString(prefs.getString(KEY_DISPLAY_LANGUAGE, DisplayLanguage.ORIGINAL.value)!!)
    )
    val displayLanguage: StateFlow<DisplayLanguage> = _displayLanguage.asStateFlow()

    fun setDisplayLanguage(language: DisplayLanguage) {
        prefs.edit().putString(KEY_DISPLAY_LANGUAGE, language.value).apply()
        _displayLanguage.value = language
    }

    companion object {
        private const val KEY_DISPLAY_LANGUAGE = "display_language"
    }
}
