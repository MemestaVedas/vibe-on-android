package moe.memesta.vibeon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.memesta.vibeon.core.domain.FavoriteServerDevice
import moe.memesta.vibeon.core.domain.ServerFavoritesRepository
import moe.memesta.vibeon.core.domain.onFailure
import moe.memesta.vibeon.core.domain.onSuccess
import moe.memesta.vibeon.core.presentation.UiText
import moe.memesta.vibeon.core.presentation.toUiText

class ServerDetailsViewModel(
    private val serverFavoritesRepository: ServerFavoritesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ServerDetailsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ServerDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: ServerDetailsAction) {
        when (action) {
            is ServerDetailsAction.OnDeviceChanged -> onDeviceChanged(action.device)
            ServerDetailsAction.OnToggleFavorite -> onToggleFavorite()
            ServerDetailsAction.OnDisconnect -> onDisconnect()
            ServerDetailsAction.OnErrorConsumed -> _state.update { it.copy(error = null) }
        }
    }

    private fun onDeviceChanged(device: moe.memesta.vibeon.data.DiscoveredDevice?) {
        _state.update { it.copy(connectedDevice = device) }

        if (device == null) {
            _state.update { it.copy(isFavorite = false) }
            return
        }

        viewModelScope.launch {
            serverFavoritesRepository.isFavorite(device.name)
                .onSuccess { isFav ->
                    _state.update { it.copy(isFavorite = isFav) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toUiText()) }
                }
        }
    }

    private fun onToggleFavorite() {
        val current = _state.value
        val device = current.connectedDevice ?: return
        if (current.isBusy) return

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }

            val result = if (current.isFavorite) {
                serverFavoritesRepository.removeFavorite(device.name)
            } else {
                serverFavoritesRepository.setFavorite(
                    FavoriteServerDevice(
                        name = device.name,
                        host = device.host,
                        port = device.port,
                        nickname = device.nickname
                    )
                )
            }

            result
                .onSuccess {
                    _state.update {
                        it.copy(
                            isFavorite = !current.isFavorite,
                            isBusy = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isBusy = false,
                            error = error.toUiText()
                        )
                    }
                }
        }
    }

    private fun onDisconnect() {
        viewModelScope.launch {
            _events.send(ServerDetailsEvent.DisconnectAndClose)
        }
    }
}
