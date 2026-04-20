package moe.memesta.vibeon.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.core.domain.DataError
import moe.memesta.vibeon.core.domain.EmptyResult
import moe.memesta.vibeon.core.domain.FavoriteServerDevice
import moe.memesta.vibeon.core.domain.Result
import moe.memesta.vibeon.core.domain.ServerFavoritesRepository

class SharedPrefsServerFavoritesRepository(
    private val favoritesManager: FavoritesManager
) : ServerFavoritesRepository {

    override suspend fun isFavorite(deviceName: String): Result<Boolean, DataError.Local> {
        return withContext(Dispatchers.IO) {
            runCatching {
                favoritesManager.isFavorite(deviceName)
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(DataError.Local.UNKNOWN) }
            )
        }
    }

    override suspend fun setFavorite(device: FavoriteServerDevice): EmptyResult<DataError.Local> {
        return withContext(Dispatchers.IO) {
            runCatching {
                favoritesManager.addFavorite(
                    FavoriteDevice(
                        name = device.name,
                        host = device.host,
                        port = device.port,
                        nickname = device.nickname
                    )
                )
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error(DataError.Local.UNKNOWN) }
            )
        }
    }

    override suspend fun removeFavorite(deviceName: String): EmptyResult<DataError.Local> {
        return withContext(Dispatchers.IO) {
            runCatching {
                favoritesManager.removeFavorite(deviceName)
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error(DataError.Local.UNKNOWN) }
            )
        }
    }
}
