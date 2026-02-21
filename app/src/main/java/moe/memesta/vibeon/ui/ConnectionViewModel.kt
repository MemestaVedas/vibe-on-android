package moe.memesta.vibeon.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.MediaNotificationManager
import moe.memesta.vibeon.data.WebSocketClient

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}

class ConnectionViewModel(private val repository: DiscoveryRepository) : ViewModel() {
    val wsClient = WebSocketClient()
    
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = repository.discoveredDevices
    val wsIsConnected: StateFlow<Boolean> = wsClient.isConnected
    val wsMessages: StateFlow<String> = wsClient.messages
    val currentTrack = wsClient.currentTrack
    val isPlaying = wsClient.isPlaying
    val lyrics = wsClient.lyrics
    val isLoadingLyrics = wsClient.isLoadingLyrics
    
    // Shuffle, Repeat, Volume, Favorites, Playlists
    val isShuffled: StateFlow<Boolean> = wsClient.isShuffled
    val repeatMode: StateFlow<String> = wsClient.repeatMode
    val volume: StateFlow<Double> = wsClient.volume
    val favorites: StateFlow<Set<String>> = wsClient.favorites
    val playlists: StateFlow<List<moe.memesta.vibeon.data.PlaylistInfo>> = wsClient.playlists
    val currentPlaylistTracks: StateFlow<List<moe.memesta.vibeon.data.TrackInfo>> = wsClient.currentPlaylistTracks
    val queue: StateFlow<List<moe.memesta.vibeon.data.QueueItem>> = wsClient.queue
    val library: StateFlow<List<moe.memesta.vibeon.data.TrackInfo>> = wsClient.library
    
    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice.asStateFlow()
    
    private var hasAutoConnected = false

    init {
        // Wire WebSocketClient into the notification manager so it tracks PC state
        MediaNotificationManager.attach(wsClient)

        // Observe discovered devices and auto-connect to favorites
        viewModelScope.launch {
            discoveredDevices.collect { devices ->
                val favoriteDevice = devices.firstOrNull { it.isFavorite }
                if (favoriteDevice != null && !hasAutoConnected && _connectionState.value == ConnectionState.IDLE) {
                    Log.i("ConnectionViewModel", "⭐ Auto-connecting to favorite: ${favoriteDevice.nickname ?: favoriteDevice.name}")
                    connectToDevice(favoriteDevice)
                    hasAutoConnected = true
                }
            }
        }
        
        // Observe WebSocket connection state
        viewModelScope.launch {
            wsIsConnected.collect { connected ->
                if (connected) {
                    _connectionState.value = ConnectionState.CONNECTED
                } else if (_connectionState.value == ConnectionState.CONNECTING) {
                    _connectionState.value = ConnectionState.FAILED
                }
            }
        }
    }

    fun startScanning() {
        repository.startDiscovery()
    }

    fun stopScanning() {
        repository.stopDiscovery()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDevice.value = device
        repository.stopDiscovery()
        wsClient.connect(device.host, device.port, "VIBE-ON Mobile")
    }
    
    fun disconnect() {
        wsClient.disconnect()
        _connectionState.value = ConnectionState.IDLE
        _connectedDevice.value = null
        hasAutoConnected = false
    }
    
    fun play() {
        wsClient.sendPlay()
    }
    
    fun pause() {
        wsClient.sendPause()
    }
    
    fun next() {
        Log.i("ConnectionViewModel", "⏭️ User clicked Next")
        wsClient.sendNext()
    }
    
    fun previous() {
        Log.i("ConnectionViewModel", "⏮️ User clicked Previous")
        wsClient.sendPrevious()
    }
    
    fun seek(positionSecs: Double) {
        wsClient.sendSeek(positionSecs)
    }
    
    fun getLyrics() {
        wsClient.sendGetLyrics()
    }
    
    fun toggleShuffle() {
        wsClient.sendToggleShuffle()
    }
    
    fun toggleRepeat() {
        wsClient.sendToggleRepeat()
    }
    
    fun toggleFavorite(trackPath: String) {
        wsClient.sendToggleFavorite(trackPath)
    }
    
    fun setVolume(volume: Double) {
        wsClient.sendSetVolume(volume)
    }
    
    fun getPlaylists() {
        wsClient.sendGetPlaylists()
    }
    
    fun getPlaylistTracks(playlistId: String) {
        wsClient.sendGetPlaylistTracks(playlistId)
    }
    
    fun addToPlaylist(playlistId: String, trackPath: String) {
        wsClient.sendAddToPlaylist(playlistId, trackPath)
    }
    
    fun removeFromPlaylist(playlistId: String, playlistTrackId: Long) {
        wsClient.sendRemoveFromPlaylist(playlistId, playlistTrackId)
    }

    fun reorderPlaylistTracks(playlistId: String, trackIds: List<Long>) {
        wsClient.sendReorderPlaylistTracks(playlistId, trackIds)
    }
    
    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
