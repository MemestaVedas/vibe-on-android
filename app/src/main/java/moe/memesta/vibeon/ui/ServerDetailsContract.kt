package moe.memesta.vibeon.ui

import moe.memesta.vibeon.core.presentation.UiText
import moe.memesta.vibeon.data.DiscoveredDevice

data class ServerDetailsState(
    val connectedDevice: DiscoveredDevice? = null,
    val isFavorite: Boolean = false,
    val isBusy: Boolean = false,
    val error: UiText? = null
)

sealed interface ServerDetailsAction {
    data class OnDeviceChanged(val device: DiscoveredDevice?) : ServerDetailsAction
    data object OnToggleFavorite : ServerDetailsAction
    data object OnDisconnect : ServerDetailsAction
    data object OnErrorConsumed : ServerDetailsAction
}

sealed interface ServerDetailsEvent {
    data object DisconnectAndClose : ServerDetailsEvent
}
