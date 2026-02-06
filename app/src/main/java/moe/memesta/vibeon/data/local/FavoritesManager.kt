package moe.memesta.vibeon.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteDevice(
    val name: String,
    val host: String,
    val port: Int,
    val nickname: String? = null
)

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_favorites",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_FAVORITES = "favorite_devices"
    }
    
    fun getFavorites(): List<FavoriteDevice> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        val jsonArray = JSONArray(json)
        return (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            FavoriteDevice(
                name = obj.getString("name"),
                host = obj.getString("host"),
                port = obj.getInt("port"),
                nickname = obj.optString("nickname").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    fun addFavorite(device: FavoriteDevice) {
        val favorites = getFavorites().toMutableList()
        // Remove duplicate if exists
        favorites.removeAll { it.name == device.name }
        favorites.add(device)
        saveFavorites(favorites)
    }
    
    fun removeFavorite(deviceName: String) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.name == deviceName }
        saveFavorites(favorites)
    }
    
    fun updateNickname(deviceName: String, nickname: String) {
        val favorites = getFavorites().toMutableList()
        val index = favorites.indexOfFirst { it.name == deviceName }
        if (index != -1) {
            favorites[index] = favorites[index].copy(nickname = nickname)
            saveFavorites(favorites)
        }
    }
    
    fun isFavorite(deviceName: String): Boolean {
        return getFavorites().any { it.name == deviceName }
    }
    
    fun getFavorite(deviceName: String): FavoriteDevice? {
        return getFavorites().firstOrNull { it.name == deviceName }
    }
    
    private fun saveFavorites(favorites: List<FavoriteDevice>) {
        val jsonArray = JSONArray()
        favorites.forEach { device ->
            val obj = JSONObject().apply {
                put("name", device.name)
                put("host", device.host)
                put("port", device.port)
                device.nickname?.let { put("nickname", it) }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES, jsonArray.toString()).apply()
    }
}
