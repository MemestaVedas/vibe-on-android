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
        Log.i("LibraryViewModel", "📡 Will connect to: http://$host:$port/api/library")
        
        // First, test server connectivity
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i("LibraryViewModel", "🔍 Testing server connection...")
                val serverInfo = streamClient.getServerInfo()
                if (serverInfo != null) {
                    Log.i("LibraryViewModel", "✅ Server connected: ${serverInfo.name} with ${serverInfo.librarySize} tracks")
                    withContext(Dispatchers.Main) {
                        loadLibrary()
                    }
                } else {
                    Log.e("LibraryViewModel", "❌ Server info returned null")
                    withContext(Dispatchers.Main) {
                        _error.value = "❌ Cannot reach server at $host:$port\n\nThe desktop app may not be running or is on a different network."
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Server check failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _error.value = "❌ Connection test failed\n\nServer: $host:$port\nError: ${e.javaClass.simpleName}\n\n${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun loadLibrary(offset: Int = 0) {
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
                        _error.value = "❌ Failed to load library\n\nConnecting to: $host:$port\n\nMake sure:\n• Desktop app is running\n• Same WiFi network\n• Firewall allows connections"
                        Log.e("LibraryViewModel", "❌ Failed to load library - browseLibrary returned null")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "❌ Connection Failed\n\nServer: $host:$port\nError: ${e.javaClass.simpleName}\n\nChecklist:\n✓ Desktop app running?\n✓ Same WiFi network?\n✓ Correct IP address?\n✓ Firewall disabled?"
                withContext(Dispatchers.Main) {
                    _error.value = errorMsg
                }
                Log.e("LibraryViewModel", "❌ Error loading library from $host:$port: ${e.message}", e)
                e.printStackTrace()
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
