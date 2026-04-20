package moe.memesta.vibeon.core.domain

data class FavoriteServerDevice(
    val name: String,
    val host: String,
    val port: Int,
    val nickname: String? = null
)

interface ServerFavoritesRepository {
    suspend fun isFavorite(deviceName: String): Result<Boolean, DataError.Local>
    suspend fun setFavorite(device: FavoriteServerDevice): EmptyResult<DataError.Local>
    suspend fun removeFavorite(deviceName: String): EmptyResult<DataError.Local>
}
