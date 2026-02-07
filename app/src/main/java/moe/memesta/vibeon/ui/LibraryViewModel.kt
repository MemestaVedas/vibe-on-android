package moe.memesta.vibeon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import moe.memesta.vibeon.data.LibraryRepository
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.WebSocketClient

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val wsClient: WebSocketClient
) : ViewModel() {
    
    // Expose baseUrl for cover art loading
    val baseUrl: String = repository.baseUrl
    
    private val _tracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val tracks: StateFlow<List<TrackInfo>> = _tracks

    // Derived lists
    private val _albums = MutableStateFlow<List<String>>(emptyList())
    val albums: StateFlow<List<String>> = _albums

    private val _artists = MutableStateFlow<List<String>>(emptyList())
    val artists: StateFlow<List<String>> = _artists

    // UI-ready derived data for HomeScreen
    private val _homeAlbums = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val homeAlbums: StateFlow<List<AlbumInfo>> = _homeAlbums

    private val _homeArtists = MutableStateFlow<List<ArtistItemData>>(emptyList())
    val homeArtists: StateFlow<List<ArtistItemData>> = _homeArtists

    private val _featuredAlbums = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val featuredAlbums: StateFlow<List<AlbumInfo>> = _featuredAlbums
    
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
    
    // Optimized List for AlbumsGridScreen
    private val _filteredAlbums = MutableStateFlow<List<AlbumInfo>>(emptyList())
    val filteredAlbums: StateFlow<List<AlbumInfo>> = _filteredAlbums
    
    private val _stats = MutableStateFlow<moe.memesta.vibeon.data.LibraryStats?>(null)
    val stats: StateFlow<moe.memesta.vibeon.data.LibraryStats?> = _stats
    
    private val _currentOffset = MutableStateFlow(0)
    val currentOffset: StateFlow<Int> = _currentOffset
    
    private var totalTracks = 0
    private val pageSize = 50
    
    init {
        Log.i("LibraryViewModel", "🔌 Initializing LibraryViewModel")
        
        // 1. Observe Database (Single Source of Truth)
        viewModelScope.launch {
            repository.tracks.collect { tracks ->
                Log.i("LibraryViewModel", "💾 Database updated: ${tracks.size} tracks")
                updateLocalState(tracks)
            }
        }
        
        // 2. Observe WebSocket for library data arriving from server
        viewModelScope.launch {
            wsClient.library.collect { tracks ->
                if (tracks.isNotEmpty()) {
                    Log.i("LibraryViewModel", "📡 Received ${tracks.size} tracks from WebSocket -> Saving to DB")
                    repository.saveTracks(tracks)
                }
            }
        }
        
        // 3. Trigger initial refresh
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshLibrary()
            fetchStats()
            _isLoading.value = false
        }
        
        // 4. Listen for connection to retry
        viewModelScope.launch {
            wsClient.isConnected.collect { connected ->
                if (connected) {
                    repository.refreshLibrary()
                    fetchStats()
                }
            }
        }
    }
    
    private suspend fun updateLocalState(tracks: List<TrackInfo>) {
        withContext(Dispatchers.Default) {
            val albumModels = tracks.groupBy { it.album }
                .map { (album, albumTracks) ->
                    AlbumInfo(
                        name = album,
                        artist = albumTracks.firstOrNull()?.artist ?: "",
                        coverUrl = albumTracks.firstOrNull()?.coverUrl
                    )
                }
                .sortedBy { it.name }

            val artistModels = tracks.groupBy { it.artist }
                .map { (artist, artistTracks) ->
                    ArtistItemData(
                        name = artist,
                        followerCount = "${artistTracks.size} Tracks",
                        photoUrl = artistTracks.firstOrNull()?.coverUrl
                    )
                }
                .sortedBy { it.name }
            
            val featured = albumModels.shuffled().take(5)

            withContext(Dispatchers.Main) {
                _tracks.value = tracks
                _albums.value = albumModels.map { it.name }
                _artists.value = artistModels.map { it.name }
                _homeAlbums.value = albumModels
                _homeArtists.value = artistModels
                _featuredAlbums.value = featured

                // Initial population of filtered albums
                updateFilteredAlbums(tracks, _searchQuery.value)
                
                totalTracks = tracks.size
                _error.value = null
            }
        }
    }

    fun searchLibrary(query: String) {
        _searchQuery.value = query
        
        if (query.isEmpty()) {
            _songResults.value = emptyList()
            _albumResults.value = emptyList()
            _artistResults.value = emptyList()
            updateFilteredAlbums(_tracks.value, "")
            return
        }
        
        // Client-side filtering (fast and offline-capable)
        updateFilteredAlbums(_tracks.value, query)
        
        viewModelScope.launch(Dispatchers.Default) {
             val tracks = _tracks.value
             val filtered = tracks.filter { 
                 it.title.contains(query, ignoreCase = true) || 
                 it.artist.contains(query, ignoreCase = true) || 
                 it.album.contains(query, ignoreCase = true) 
             }
             
             withContext(Dispatchers.Main) {
                 _songResults.value = filtered
                 _albumResults.value = filtered.map { 
                     AlbumInfo(it.album, it.artist, it.coverUrl) 
                 }.distinctBy { it.name }
                 _artistResults.value = filtered.map { 
                     ArtistItemData(it.artist, "${filtered.count { t -> t.artist == it.artist }} Tracks", it.coverUrl) 
                 }.distinctBy { it.name }
             }
        }
    }
    
    fun fetchStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stats = repository.getStats()
                withContext(Dispatchers.Main) {
                    _stats.value = stats
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error fetching stats: ${e.message}")
            }
        }
    }
    
    fun playTrack(track: TrackInfo, context: List<TrackInfo> = emptyList()) {
        if (context.isNotEmpty()) {
            val paths = context.map { it.path }
            wsClient.sendSetQueue(paths)
        }
        
        wsClient.sendPlayTrack(track.path)
        val isMobile = wsClient.isMobilePlayback.value
        Log.i("LibraryViewModel", "▶️ Playing: ${track.title} (Mobile Mode: $isMobile)")
        if (isMobile) {
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
        val artist = _tracks.value.find { it.album == albumName }?.artist ?: ""
        wsClient.sendPlayAlbum(albumName, artist)
        Log.i("LibraryViewModel", "▶️ Sent PlayAlbum command: $albumName")
        if (wsClient.isMobilePlayback.value) {
            wsClient.sendStartMobilePlayback()
        }
    }
    
    fun playArtist(artistName: String) {
        wsClient.sendPlayArtist(artistName)
        Log.i("LibraryViewModel", "▶️ Sent PlayArtist command: $artistName")
        if (wsClient.isMobilePlayback.value) {
            wsClient.sendStartMobilePlayback()
        }
    }
    
    // Pagination NO OP as we load full DB now, but kept for compatibility if needed
    fun loadNextPage() { }
    fun loadPreviousPage() { }
    
    private fun updateFilteredAlbums(tracks: List<TrackInfo>, query: String) {
        viewModelScope.launch(Dispatchers.Default) {
             val filtered = if (query.isEmpty()) {
                 tracks
             } else {
                 tracks.filter {
                     it.album.contains(query, ignoreCase = true) ||
                     it.artist.contains(query, ignoreCase = true)
                 }
             }

            val albumModels = filtered.groupBy { it.album }
                .map { (album, albumTracks) ->
                    AlbumInfo(
                        name = album,
                        artist = albumTracks.firstOrNull()?.artist ?: "",
                        coverUrl = albumTracks.firstOrNull()?.coverUrl
                    )
                }
                .sortedBy { it.name }

            withContext(Dispatchers.Main) {
                _filteredAlbums.value = albumModels
            }
        }
    }
}
