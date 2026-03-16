package moe.memesta.vibeon.torrent

import android.content.Context
import android.content.SharedPreferences

class TorrentStoragePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_torrent_storage",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SAVE_PATH = "save_path"
    }

    var savePath: String?
        get() = prefs.getString(KEY_SAVE_PATH, null)
        set(value) {
            if (value.isNullOrBlank()) {
                prefs.edit().remove(KEY_SAVE_PATH).apply()
            } else {
                prefs.edit().putString(KEY_SAVE_PATH, value).apply()
            }
        }

    fun hasUserSelectedPath(): Boolean = !savePath.isNullOrBlank()
}
