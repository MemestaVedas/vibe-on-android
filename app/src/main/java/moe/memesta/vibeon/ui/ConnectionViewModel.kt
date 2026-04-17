package moe.memesta.vibeon.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.MediaNotificationManager
import moe.memesta.vibeon.data.MusicStreamClient
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository
import moe.memesta.vibeon.data.stats.LocalStatsTracker

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val repository: DiscoveryRepository,
    val localStatsRepository: LocalPlaybackStatsRepository,
    val wsClient: WebSocketClient
) : ViewModel() {
    companion object {
        private const val SUPPORTED_PROTOCOL_MAJOR = 1
        private const val MAX_EVENTS_PER_SYNC = 400
        private const val EVENT_SYNC_BATCH_SIZE = 40
        private const val EVENT_SYNC_BATCH_DELAY_MS = 90L
    }

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

    private val _protocolWarning = MutableStateFlow<String?>(null)
    val protocolWarning: StateFlow<String?> = _protocolWarning.asStateFlow()
    
    private var hasAutoConnected = false
    private var localSyncJob: Job? = null

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
                    // Defer event sync briefly so initial UI transitions are not contending on connect.
                    localSyncJob?.cancel()
                    localSyncJob = viewModelScope.launch {
                        delay(2500)
                        syncLocalEventsToPc()
                    }
                } else {
                    localSyncJob?.cancel()
                    when (_connectionState.value) {
                        ConnectionState.CONNECTING -> _connectionState.value = ConnectionState.FAILED
                        ConnectionState.CONNECTED -> {
                            // Server dropped while connected — show reconnecting state.
                            _connectionState.value = ConnectionState.RECONNECTING
                            hasAutoConnected = false
                        }
                        ConnectionState.RECONNECTING -> {
                            // Keep showing reconnecting until reconnect succeeds or user disconnects.
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
        _protocolWarning.value = null
        repository.stopDiscovery()
        wsClient.connect(device.host, device.port, "VIBE-ON Mobile")

        viewModelScope.launch {
            val info = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                MusicStreamClient(device.host, device.port).getServerInfo()
            }
            val major = info?.protocolVersion
                ?.split('.')
                ?.firstOrNull()
                ?.toIntOrNull()

            if (major != null && major != SUPPORTED_PROTOCOL_MAJOR) {
                val warning = "Server protocol ${info.protocolVersion} may be incompatible with mobile protocol major $SUPPORTED_PROTOCOL_MAJOR."
                _protocolWarning.value = warning
                Log.w("ConnectionViewModel", warning)
            }
        }
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
                val payload = if (events.size > MAX_EVENTS_PER_SYNC) {
                    events.takeLast(MAX_EVENTS_PER_SYNC)
                } else {
                    events
                }

                Log.i(
                    "ConnectionViewModel",
                    "📤 Syncing ${payload.size}/${events.size} local events to PC in batches of $EVENT_SYNC_BATCH_SIZE"
                )

                payload
                    .chunked(EVENT_SYNC_BATCH_SIZE)
                    .forEachIndexed { index, batch ->
                        if (!wsClient.isConnected.value) {
                            Log.w("ConnectionViewModel", "Connection dropped during local event sync; stopping at batch $index")
                            return@forEachIndexed
                        }

                        wsClient.sendSyncPlaybackEvents(batch)

                        val sentCount = ((index + 1) * EVENT_SYNC_BATCH_SIZE).coerceAtMost(payload.size)
                        if (sentCount < payload.size) {
                            delay(EVENT_SYNC_BATCH_DELAY_MS)
                        }
                    }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        statsTracker.stop()
        wsClient.disconnect()
    }
}
