package moe.memesta.vibeon.core.presentation

import moe.memesta.vibeon.core.domain.DataError

fun DataError.toUiText(): UiText {
    return when (this) {
        DataError.Network.BAD_REQUEST -> UiText.DynamicString("Bad request")
        DataError.Network.REQUEST_TIMEOUT -> UiText.DynamicString("Request timed out")
        DataError.Network.UNAUTHORIZED -> UiText.DynamicString("Unauthorized")
        DataError.Network.FORBIDDEN -> UiText.DynamicString("Forbidden")
        DataError.Network.NOT_FOUND -> UiText.DynamicString("Resource not found")
        DataError.Network.CONFLICT -> UiText.DynamicString("Conflict detected")
        DataError.Network.TOO_MANY_REQUESTS -> UiText.DynamicString("Too many requests")
        DataError.Network.NO_INTERNET -> UiText.DynamicString("No internet connection")
        DataError.Network.PAYLOAD_TOO_LARGE -> UiText.DynamicString("Payload too large")
        DataError.Network.SERVER_ERROR -> UiText.DynamicString("Server error")
        DataError.Network.SERVICE_UNAVAILABLE -> UiText.DynamicString("Service unavailable")
        DataError.Network.SERIALIZATION -> UiText.DynamicString("Serialization error")
        DataError.Network.UNKNOWN -> UiText.DynamicString("Unknown network error")
        DataError.Local.DISK_FULL -> UiText.DynamicString("Device storage is full")
        DataError.Local.NOT_FOUND -> UiText.DynamicString("Data not found")
        DataError.Local.UNKNOWN -> UiText.DynamicString("Unknown local data error")
    }
}
