package moe.memesta.vibeon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import moe.memesta.vibeon.data.MusicStreamClient
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.WebSocketClient

class LibraryViewModel(
    private val host: String,
    private val port: Int = 5000,
    private val wsClient: WebSocketClient
) : ViewModel() {
    private val streamClient = MusicStreamClient(host, port)
    
    // Expose baseUrl for cover art loading
    val baseUrl: String = streamClient.getBaseUrl()
    
    private val _tracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val tracks: StateFlow<List<TrackInfo>> = _tracks
    
    // Expose player state for MiniPlayer
    val currentTrack = wsClient.currentTrack
    val isPlaying = wsClient.isPlaying
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _currentOffset = MutableStateFlow(0)
    val currentOffset: StateFlow<Int> = _currentOffset
    
    private var totalTracks = 0
    private val pageSize = 50
    
    init {
        Log.i("LibraryViewModel", "🔌 Initializing for server: $host:$port")
        
        // Observe WebSocket library updates
        viewModelScope.launch {
            wsClient.library.collect { tracks ->
                if (tracks.isNotEmpty()) {
                    Log.i("LibraryViewModel", "📚 Received ${tracks.size} tracks from WebSocket")
                    withContext(Dispatchers.Main) {
                        _tracks.value = tracks
                        totalTracks = tracks.size
                        _isLoading.value = false
                        _error.value = null
                    }
                }
            }
        }
        
        // Request library via WebSocket on init
        viewModelScope.launch(Dispatchers.IO) {
            // Wait for connection
            var retries = 0
            while (!wsClient.isConnected.value && retries < 5) {
                kotlinx.coroutines.delay(500)
                retries++
            }
            
            if (wsClient.isConnected.value) {
                Log.i("LibraryViewModel", "📡 Requesting library via WebSocket...")
                wsClient.sendGetLibrary()
            } else {
                Log.w("LibraryViewModel", "⚠️ WebSocket not connected, falling back to HTTP...")
                loadLibrary() // Fallback to HTTP
            }
        }
    }
    
    fun loadLibrary(offset: Int = 0) {
        // Prefer WebSocket if connected
        if (wsClient.isConnected.value) {
            wsClient.sendGetLibrary()
            _isLoading.value = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                _error.value = null
                _currentOffset.value = offset
            }
            
            try {
                Log.i("LibraryViewModel", "📚 Loading library from http://$host:$port/api/library...")
                val response = streamClient.browseLibrary(offset, pageSize)
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        _tracks.value = response.tracks
                        totalTracks = response.total
                        Log.i("LibraryViewModel", "✅ Loaded ${response.tracks.size} tracks (total: ${response.total})")
                    } else {
                        _error.value = "❌ Failed to load library\n\nConnecting to: $host:$port"
                    }
                }
            } catch (e: Exception) {
                // Error handling...
                withContext(Dispatchers.Main) { _isLoading.value = false }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun searchLibrary(query: String) {
        _searchQuery.value = query
        
        if (query.isEmpty()) {
            loadLibrary(0)
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                _error.value = null
                _currentOffset.value = 0
            }
            
            try {
                val results = streamClient.searchLibrary(query, 0, pageSize)
                withContext(Dispatchers.Main) {
                    if (results != null) {
                        _tracks.value = results
                        totalTracks = results.size
                        Log.i("LibraryViewModel", "✅ Found ${results.size} results for '$query'")
                    } else {
                        _error.value = "No results found"
                        _tracks.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Search error: ${e.message}"
                }
                Log.e("LibraryViewModel", "❌ Error searching library: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun playTrack(track: TrackInfo) {
        wsClient.sendPlayTrack(track.path)
        Log.i("LibraryViewModel", "▶️ Playing: ${track.title} by ${track.artist}")
    }
    
    fun sendPlay() {
        wsClient.sendPlay()
    }

    fun sendPause() {
        wsClient.sendPause()
    }
    
    fun loadNextPage() {
        val nextOffset = _currentOffset.value + pageSize
        if (nextOffset < totalTracks && _searchQuery.value.isEmpty()) {
            loadLibrary(nextOffset)
        }
    }
    
    fun loadPreviousPage() {
        val prevOffset = (_currentOffset.value - pageSize).coerceAtLeast(0)
        if (_searchQuery.value.isEmpty()) {
            loadLibrary(prevOffset)
        }
    }
}
