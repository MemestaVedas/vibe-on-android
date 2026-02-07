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
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
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

    // Derived lists
    private val _albums = MutableStateFlow<List<String>>(emptyList())
    val albums: StateFlow<List<String>> = _albums

    private val _artists = MutableStateFlow<List<String>>(emptyList())
    val artists: StateFlow<List<String>> = _artists
    
    // Expose player state for MiniPlayer
    val currentTrack = wsClient.currentTrack
    val isPlaying = wsClient.isPlaying
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Categorized search results
    private val _songResults = MutableStateFlow<List<TrackInfo>>(emptyList())
    val songResults: StateFlow<List<TrackInfo>> = _songResults
    
    private val _albumResults = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val albumResults: StateFlow<List<AlbumInfo>> = _albumResults
    
    private val _artistResults = MutableStateFlow<List<ArtistItemData>>(emptyList())
    val artistResults: StateFlow<List<ArtistItemData>> = _artistResults
    
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
                        // Calculate derived data
                        _albums.value = tracks.map { it.album }.distinct().sorted()
                        _artists.value = tracks.map { it.artist }.distinct().sorted()
                        
                        totalTracks = tracks.size
                        _isLoading.value = false
                        _error.value = null
                    }
                }
            }
        }
        
        // Request library via WebSocket on init
        viewModelScope.launch { // Use Main dispatcher for state collection
            wsClient.isConnected.collect { connected ->
                if (connected) {
                    Log.i("LibraryViewModel", "📡 WebSocket connected, requesting library...")
                    wsClient.sendGetLibrary()
                    _isLoading.value = true
                } else {
                     Log.w("LibraryViewModel", "⚠️ WebSocket disconnected")
                     // Optionally handle disconnect UI state
                }
            }
        }
        
        // Initial fallback check
        viewModelScope.launch(Dispatchers.IO) {
            // Give WS a moment to connect
            kotlinx.coroutines.delay(1000)
            if (!wsClient.isConnected.value && _tracks.value.isEmpty()) {
                 Log.w("LibraryViewModel", "⚠️ WebSocket not connected after timeout, falling back to HTTP...")
                 loadLibrary()
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
                        // Calculate derived data
                        _albums.value = response.tracks.map { it.album }.distinct().sorted()
                        _artists.value = response.tracks.map { it.artist }.distinct().sorted()
                        
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
            // Clear search results when query is empty
            _songResults.value = emptyList()
            _albumResults.value = emptyList()
            _artistResults.value = emptyList()
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
                    if (results != null && results.isNotEmpty()) {
                        _tracks.value = results
                        totalTracks = results.size
                        
                        // Log first result to debug coverUrl
                        results.firstOrNull()?.let {
                            Log.i("LibraryViewModel", "📸 Sample search result - Title: ${it.title}, CoverUrl: ${it.coverUrl}")
                        }
                        
                        // Populate categorized results
                        _songResults.value = results
                        
                        _albumResults.value = results
                            .groupBy { it.album }
                            .map { (album, albumTracks) ->
                                AlbumInfo(
                                    name = album,
                                    artist = albumTracks.firstOrNull()?.artist ?: "",
                                    coverUrl = albumTracks.firstOrNull()?.coverUrl
                                )
                            }
                            .distinctBy { it.name }
                        
                        _artistResults.value = results
                            .groupBy { it.artist }
                            .map { (artist, artistTracks) ->
                                ArtistItemData(
                                    name = artist,
                                    followerCount = "${artistTracks.size} Tracks",
                                    photoUrl = artistTracks.firstOrNull()?.coverUrl
                                )
                            }
                            .distinctBy { it.name }
                        
                        Log.i("LibraryViewModel", "✅ Found ${results.size} results → ${_albumResults.value.size} albums, ${_artistResults.value.size} artists")
                    } else {
                        _error.value = "No results found"
                        _tracks.value = emptyList()
                        _songResults.value = emptyList()
                        _albumResults.value = emptyList()
                        _artistResults.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Search error: ${e.message}"
                    _songResults.value = emptyList()
                    _albumResults.value = emptyList()
                    _artistResults.value = emptyList()
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
        
        val isMobile = wsClient.isMobilePlayback.value
        Log.i("LibraryViewModel", "▶️ Playing: ${track.title} (Mobile Mode: $isMobile)")
        
        if (isMobile) {
            // Force server to acknowledge mobile mode (pausing PC playback that PlayTrack started)
            wsClient.sendStartMobilePlayback()
        }
    }
    
    fun sendPlay() {
        wsClient.sendPlay()
    }

    fun sendPause() {
        wsClient.sendPause()
    }
    
    fun playAlbum(albumName: String) {
        val albumTracks = _tracks.value.filter { it.album == albumName }
        if (albumTracks.isNotEmpty()) {
            playTrack(albumTracks.first())
            Log.i("LibraryViewModel", "▶️ Playing album: $albumName (${albumTracks.size} tracks)")
        }
    }
    
    fun playArtist(artistName: String) {
        val artistTracks = _tracks.value.filter { it.artist == artistName }
        if (artistTracks.isNotEmpty()) {
            playTrack(artistTracks.first())
            Log.i("LibraryViewModel", "▶️ Playing artist: $artistName (${artistTracks.size} tracks)")
        }
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
