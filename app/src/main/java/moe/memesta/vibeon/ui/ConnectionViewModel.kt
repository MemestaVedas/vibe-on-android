package moe.memesta.vibeon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.data.WebSocketClient

class ConnectionViewModel(private val repository: DiscoveryRepository) : ViewModel() {
    val wsClient = WebSocketClient()
    
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = repository.discoveredDevices
    val wsIsConnected: StateFlow<Boolean> = wsClient.isConnected
    val wsMessages: StateFlow<String> = wsClient.messages
    val currentTrack = wsClient.currentTrack
    val isPlaying = wsClient.isPlaying

    fun startScanning() {
        repository.startDiscovery()
    }

    fun stopScanning() {
        repository.stopDiscovery()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        // Establish WebSocket connection
        repository.stopDiscovery()
        wsClient.connect(device.host, device.port, "VIBE-ON Mobile")
    }
    
    fun disconnect() {
        wsClient.disconnect()
    }
    
    fun play() {
        wsClient.sendPlay()
    }
    
    fun pause() {
        wsClient.sendPause()
    }
    
    fun next() {
        wsClient.sendNext()
    }
    
    fun previous() {
        wsClient.sendPrevious()
    }
    
    fun seek(positionSecs: Double) {
        wsClient.sendSeek(positionSecs)
    }
    
    fun getLyrics() {
        wsClient.sendGetLyrics()
    }
    
    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
