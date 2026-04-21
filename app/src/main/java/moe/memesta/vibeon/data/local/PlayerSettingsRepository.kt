package moe.memesta.vibeon.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.model.TransitionMode
import moe.memesta.vibeon.data.model.TransitionSettings

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

enum class ScrubberMode(val value: String) {
    WAVEFORM("WAVEFORM"),
    CLASSIC("CLASSIC");

    companion object {
        fun fromString(value: String): ScrubberMode {
            return entries.find { it.value == value } ?: WAVEFORM
        }
    }
}

enum class NowPlayingFontMode(val value: String) {
    AUTOMATIC("automatic"),
    DYNAMIC("dynamic");

    companion object {
        fun fromString(value: String): NowPlayingFontMode {
            return entries.find { it.value == value } ?: AUTOMATIC
        }
    }
}

enum class WidgetFontMode(val value: String) {
    DYNAMIC("dynamic"),
    MANUAL("manual");

    companion object {
        fun fromString(value: String): WidgetFontMode {
            return entries.find { it.value == value } ?: DYNAMIC
        }
    }
}

class PlayerSettingsRepository(context: Context) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_settings",
        Context.MODE_PRIVATE
    )
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = repositoryScope,
        produceFile = { context.preferencesDataStoreFile("vibe_on_player_preferences") }
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

    val scrubberMode: StateFlow<ScrubberMode> = dataStore.data
        .map { preferences ->
            ScrubberMode.fromString(preferences[KEY_SCRUBBER_MODE] ?: ScrubberMode.WAVEFORM.value)
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = ScrubberMode.WAVEFORM
        )

    val sheetHintShown: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_SHEET_HINT_SHOWN] ?: false }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val artGestureHintShown: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_ART_GESTURE_HINT_SHOWN] ?: false }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val transitionSettings: StateFlow<TransitionSettings> = dataStore.data
        .map { preferences ->
            TransitionSettings(
                mode = TransitionMode.fromString(
                    preferences[KEY_TRANSITION_MODE] ?: TransitionMode.NONE.value
                ),
                durationMs = (preferences[KEY_TRANSITION_DURATION_MS] ?: 1200)
                    .coerceIn(250, 5000)
            )
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = TransitionSettings()
        )

    val nowPlayingFontMode: StateFlow<NowPlayingFontMode> = dataStore.data
        .map { preferences ->
            NowPlayingFontMode.fromString(
                preferences[KEY_NOW_PLAYING_FONT_MODE] ?: NowPlayingFontMode.AUTOMATIC.value
            )
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = NowPlayingFontMode.AUTOMATIC
        )

    val widgetFontMode: StateFlow<WidgetFontMode> = dataStore.data
        .map { preferences ->
            WidgetFontMode.fromString(
                preferences[KEY_WIDGET_FONT_MODE] ?: WidgetFontMode.DYNAMIC.value
            )
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = WidgetFontMode.DYNAMIC
        )

    val widgetManualWidth: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_MANUAL_WIDTH] ?: 100 }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = 100
        )

    val widgetManualWeight: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_MANUAL_WEIGHT] ?: 640 }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = 640
        )

    val widgetManualRoundness: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_MANUAL_ROUNDNESS] ?: 140 }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = 140
        )

    fun getDisplayLanguage(): DisplayLanguage = _displayLanguage.value

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

    fun setScrubberMode(mode: ScrubberMode) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SCRUBBER_MODE] = mode.value
            }
        }
    }

    fun toggleScrubberMode() {
        val nextMode = if (scrubberMode.value == ScrubberMode.WAVEFORM) {
            ScrubberMode.CLASSIC
        } else {
            ScrubberMode.WAVEFORM
        }
        setScrubberMode(nextMode)
    }

    fun setSheetHintShown(shown: Boolean) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SHEET_HINT_SHOWN] = shown
            }
        }
    }

    fun setArtGestureHintShown(shown: Boolean) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_ART_GESTURE_HINT_SHOWN] = shown
            }
        }
    }

    fun setTransitionMode(mode: TransitionMode) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_TRANSITION_MODE] = mode.value
            }
        }
    }

    fun setTransitionDurationMs(durationMs: Int) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_TRANSITION_DURATION_MS] = durationMs.coerceIn(250, 5000)
            }
        }
    }

    fun setNowPlayingFontMode(mode: NowPlayingFontMode) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_NOW_PLAYING_FONT_MODE] = mode.value
            }
        }
    }

    fun setWidgetFontMode(mode: WidgetFontMode) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_WIDGET_FONT_MODE] = mode.value
            }
        }
    }

    fun setWidgetManualWidth(width: Int) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_WIDGET_MANUAL_WIDTH] = width.coerceIn(75, 125)
            }
        }
    }

    fun setWidgetManualWeight(weight: Int) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_WIDGET_MANUAL_WEIGHT] = weight.coerceIn(300, 900)
            }
        }
    }

    fun setWidgetManualRoundness(roundness: Int) {
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_WIDGET_MANUAL_ROUNDNESS] = roundness.coerceIn(0, 200)
            }
        }
    }

    companion object {
        private const val KEY_DISPLAY_LANGUAGE = "display_language"
        private const val KEY_ALBUM_VIEW_STYLE = "album_view_style"
        private const val KEY_ARTIST_VIEW_STYLE = "artist_view_style"
        private val KEY_SCRUBBER_MODE = stringPreferencesKey("SCRUBBER_MODE")
        private val KEY_SHEET_HINT_SHOWN = booleanPreferencesKey("sheet_hint_shown")
        private val KEY_ART_GESTURE_HINT_SHOWN = booleanPreferencesKey("art_gesture_hint_shown")
        private val KEY_TRANSITION_MODE = stringPreferencesKey("transition_mode")
        private val KEY_TRANSITION_DURATION_MS = intPreferencesKey("transition_duration_ms")
        private val KEY_NOW_PLAYING_FONT_MODE = stringPreferencesKey("now_playing_font_mode")
        private val KEY_WIDGET_FONT_MODE = stringPreferencesKey("widget_font_mode")
        private val KEY_WIDGET_MANUAL_WIDTH = intPreferencesKey("widget_manual_width")
        private val KEY_WIDGET_MANUAL_WEIGHT = intPreferencesKey("widget_manual_weight")
        private val KEY_WIDGET_MANUAL_ROUNDNESS = intPreferencesKey("widget_manual_roundness")
    }
}
