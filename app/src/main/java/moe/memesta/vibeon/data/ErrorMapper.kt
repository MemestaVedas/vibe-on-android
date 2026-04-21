package moe.memesta.vibeon.data

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import moe.memesta.vibeon.core.domain.DataError

object ErrorMapper {
    fun mapHttpCode(code: Int): DataError.Network = when (code) {
        400 -> DataError.Network.BAD_REQUEST
        401 -> DataError.Network.UNAUTHORIZED
        403 -> DataError.Network.FORBIDDEN
        404 -> DataError.Network.NOT_FOUND
        408 -> DataError.Network.REQUEST_TIMEOUT
        409 -> DataError.Network.CONFLICT
        413 -> DataError.Network.PAYLOAD_TOO_LARGE
        429 -> DataError.Network.TOO_MANY_REQUESTS
        500 -> DataError.Network.SERVER_ERROR
        503 -> DataError.Network.SERVICE_UNAVAILABLE
        else -> DataError.Network.UNKNOWN
    }

    fun mapException(throwable: Throwable): DataError.Network = when (throwable) {
        is ConnectException -> DataError.Network.NO_INTERNET
        is UnknownHostException -> DataError.Network.NO_INTERNET
        is SocketTimeoutException -> DataError.Network.REQUEST_TIMEOUT
        else -> DataError.Network.UNKNOWN
    }
}
