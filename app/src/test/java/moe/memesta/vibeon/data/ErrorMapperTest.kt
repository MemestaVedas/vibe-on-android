package moe.memesta.vibeon.data

import moe.memesta.vibeon.core.domain.DataError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {
    @Test
    fun mapHttpCode_knownCodes_returnsExpectedNetworkError() {
        assertEquals(DataError.Network.BAD_REQUEST, ErrorMapper.mapHttpCode(400))
        assertEquals(DataError.Network.UNAUTHORIZED, ErrorMapper.mapHttpCode(401))
        assertEquals(DataError.Network.NOT_FOUND, ErrorMapper.mapHttpCode(404))
        assertEquals(DataError.Network.TOO_MANY_REQUESTS, ErrorMapper.mapHttpCode(429))
        assertEquals(DataError.Network.SERVER_ERROR, ErrorMapper.mapHttpCode(500))
    }

    @Test
    fun mapHttpCode_unknownCode_returnsUnknown() {
        assertEquals(DataError.Network.UNKNOWN, ErrorMapper.mapHttpCode(599))
    }

    @Test
    fun mapException_connectionProblems_returnNoInternet() {
        assertEquals(DataError.Network.NO_INTERNET, ErrorMapper.mapException(ConnectException("failed")))
        assertEquals(DataError.Network.NO_INTERNET, ErrorMapper.mapException(UnknownHostException("failed")))
    }

    @Test
    fun mapException_timeoutAndUnknown_returnExpectedError() {
        assertEquals(DataError.Network.REQUEST_TIMEOUT, ErrorMapper.mapException(SocketTimeoutException("failed")))
        assertEquals(DataError.Network.UNKNOWN, ErrorMapper.mapException(IllegalStateException("failed")))
    }
}
