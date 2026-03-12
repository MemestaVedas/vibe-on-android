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
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository
import moe.memesta.vibeon.data.stats.LocalStatsTracker

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}

class ConnectionViewModel(
    private val repository: DiscoveryRepository,
    val localStatsRepository: LocalPlaybackStatsRepository,
    val wsClient: WebSocketClient
) : ViewModel() {
    private val statsTracker by lazy {
        LocalStatsTracker(
            statsRepository = localStatsRepository,
            currentTrack = wsClient.currentTrack,
            isPlayingFlow = wsClient.isPlaying,
            scope = viewModelScope,
            wsClient = wsClient
        )
    }
    
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
    val currentIndex: StateFlow<Int> = wsClient.currentIndex
    val library: StateFlow<List<moe.memesta.vibeon.data.TrackInfo>> = wsClient.library
    
    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice.asStateFlow()
    
    private var hasAutoConnected = false

    init {
        // Wire WebSocketClient into the notification manager so it tracks PC state
        MediaNotificationManager.attach(wsClient)
        // Start local stats tracking
        statsTracker.start()

        // Observe discovered devices and auto-connect to favorites or the first discovered device
        viewModelScope.launch {
            discoveredDevices.collect { devices ->
                val targetDevice = devices.firstOrNull { it.isFavorite } ?: devices.firstOrNull()
                if (targetDevice != null && !hasAutoConnected && _connectionState.value == ConnectionState.IDLE) {
                    Log.i("ConnectionViewModel", "⭐ Auto-connecting to: ${targetDevice.nickname ?: targetDevice.name}")
                    connectToDevice(targetDevice)
                    hasAutoConnected = true
                }
            }
        }
        
        // Observe WebSocket connection state
        viewModelScope.launch {
            wsIsConnected.collect { connected ->
                if (connected) {
                    _connectionState.value = ConnectionState.CONNECTED
                    // Sync any locally stored events to the PC on reconnect
                    syncLocalEventsToPc()
                } else {
                    when (_connectionState.value) {
                        ConnectionState.CONNECTING -> _connectionState.value = ConnectionState.FAILED
                        ConnectionState.CONNECTED -> {
                            // Server dropped while we were connected — allow reconnect
                            _connectionState.value = ConnectionState.IDLE
                            hasAutoConnected = false
                        }
                        else -> { /* already IDLE or FAILED */ }
                    }
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
        wsClient.sendStopMobilePlayback()
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

    fun playTrack(path: String) {
        wsClient.sendPlayTrack(path)
    }

    fun setQueue(paths: List<String>) {
        wsClient.sendSetQueue(paths)
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
    
    fun createPlaylist(name: String, songPaths: List<String>, customization: PlaylistCustomization) {
        wsClient.sendCreatePlaylist(name, songPaths, customization)
    }

    /**
     * Push locally recorded playback events to the PC so they end up in the shared SQLite DB.
     * The PC server de-duplicates by (song_id, timestamp_ms) so re-sending is safe.
     */
    private fun syncLocalEventsToPc() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val events = localStatsRepository.readEvents()
            if (events.isNotEmpty()) {
                Log.i("ConnectionViewModel", "📤 Syncing ${events.size} local events to PC")
                wsClient.sendSyncPlaybackEvents(events)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        statsTracker.stop()
        wsClient.disconnect()
    }
}
