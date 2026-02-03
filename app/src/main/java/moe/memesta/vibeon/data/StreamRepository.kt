package moe.memesta.vibeon.data

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uniffi.vibe_on_core.*

class StreamRepository : StreamEventHandler {
    private val connectionManager = ConnectionManager()
    
    private val _streamEvents = MutableSharedFlow<StreamEvent>(extraBufferCapacity = 64)
    val streamEvents: SharedFlow<StreamEvent> = _streamEvents.asSharedFlow()

    init {
        connectionManager.setHandler(this)
    }

    override fun onHeader(header: StreamHeader) {
        Log.d("Stream", "Received header: ${header.title} by ${header.artist}")
        _streamEvents.tryEmit(StreamEvent.HeaderReceived(header))
    }

    override fun onChunk(sequence: ULong, data: ByteArray, isLast: Boolean) {
        Log.d("Stream", "Received chunk $sequence, last=$isLast")
        _streamEvents.tryEmit(StreamEvent.ChunkReceived(sequence, data, isLast))
    }

    override fun onError(message: String) {
        Log.e("Stream", "Error: $message")
        _streamEvents.tryEmit(StreamEvent.ErrorOccurred(message))
    }

    suspend fun connectToPeer(multiaddr: String) {
        connectionManager.connectToPeer(multiaddr)
    }

    suspend fun requestTrack(trackPath: String, startByte: ULong = 0uL) {
        connectionManager.requestTrack(trackPath, startByte)
    }
}

sealed class StreamEvent {
    data class HeaderReceived(val header: StreamHeader) : StreamEvent()
    data class ChunkReceived(val sequence: ULong, val data: ByteArray, val isLast: Boolean) : StreamEvent()
    data class ErrorOccurred(val message: String) : StreamEvent()
}
