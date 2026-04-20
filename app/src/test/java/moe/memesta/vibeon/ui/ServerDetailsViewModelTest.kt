package moe.memesta.vibeon.ui

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import moe.memesta.vibeon.core.domain.DataError
import moe.memesta.vibeon.core.domain.EmptyResult
import moe.memesta.vibeon.core.domain.FavoriteServerDevice
import moe.memesta.vibeon.core.domain.Result
import moe.memesta.vibeon.core.domain.ServerFavoritesRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerDetailsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `device change loads favorite status`() = runTest {
        val repository = FakeServerFavoritesRepository(initialFavorites = setOf("Desktop-A"))
        val viewModel = ServerDetailsViewModel(repository)
        val device = DiscoveredDevice(name = "Desktop-A", host = "192.168.1.10", port = 8080)

        viewModel.state.test {
            assertThat(awaitItem().isFavorite).isEqualTo(false)

            viewModel.onAction(ServerDetailsAction.OnDeviceChanged(device))
            var state = awaitItem()
            while (!state.isFavorite) {
                state = awaitItem()
            }
            assertThat(state.isFavorite).isEqualTo(true)
        }
    }

    @Test
    fun `toggle favorite updates state`() = runTest {
        val repository = FakeServerFavoritesRepository()
        val viewModel = ServerDetailsViewModel(repository)
        val device = DiscoveredDevice(name = "Desktop-B", host = "192.168.1.11", port = 8080)

        viewModel.onAction(ServerDetailsAction.OnDeviceChanged(device))

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ServerDetailsAction.OnToggleFavorite)
            var updated = awaitItem()
            while (!updated.isFavorite) {
                updated = awaitItem()
            }
            assertThat(updated.isFavorite).isEqualTo(true)
        }
    }

    private class FakeServerFavoritesRepository(
        initialFavorites: Set<String> = emptySet()
    ) : ServerFavoritesRepository {

        private val favorites = initialFavorites.toMutableSet()

        override suspend fun isFavorite(deviceName: String): Result<Boolean, DataError.Local> {
            return Result.Success(favorites.contains(deviceName))
        }

        override suspend fun setFavorite(device: FavoriteServerDevice): EmptyResult<DataError.Local> {
            favorites.add(device.name)
            return Result.Success(Unit)
        }

        override suspend fun removeFavorite(deviceName: String): EmptyResult<DataError.Local> {
            favorites.remove(deviceName)
            return Result.Success(Unit)
        }
    }
}
