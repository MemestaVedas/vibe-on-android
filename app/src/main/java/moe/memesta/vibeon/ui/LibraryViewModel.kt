package moe.memesta.vibeon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import moe.memesta.vibeon.data.LibraryRepository
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.ui.utils.parseAlbum

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val wsClient: WebSocketClient
) : ViewModel() {
    
    // Expose baseUrl for cover art loading
    val baseUrl: String = repository.baseUrl
    
    val syncStatus = repository.syncStatus
    
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
    
    // Album sorting
    private val _currentAlbumSortOption = MutableStateFlow<SortOption>(SortOption.AlbumDefault)
    val currentAlbumSortOption: StateFlow<SortOption> = _currentAlbumSortOption
    
    // Track sorting
    private val _currentTrackSortOption = MutableStateFlow<SortOption>(SortOption.TrackDefault)
    val currentTrackSortOption: StateFlow<SortOption> = _currentTrackSortOption

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedTracks: kotlinx.coroutines.flow.Flow<PagingData<TrackInfo>> =
        combine(_searchQuery, _currentTrackSortOption) { query, sort ->
            query.trim() to sort
        }
            .distinctUntilChanged()
            .flatMapLatest { (query, sort) ->
                repository.getPagedTracks(query = query, sortOption = sort)
            }
            .cachedIn(viewModelScope)

    // Artist sorting
    private val _currentArtistSortOption = MutableStateFlow<SortOption>(SortOption.ArtistDefault)
    val currentArtistSortOption: StateFlow<SortOption> = _currentArtistSortOption
    
    private val _stats = MutableStateFlow<moe.memesta.vibeon.data.LibraryStats?>(null)
    val stats: StateFlow<moe.memesta.vibeon.data.LibraryStats?> = _stats
    
    private val _currentOffset = MutableStateFlow(0)
    val currentOffset: StateFlow<Int> = _currentOffset
    
    private var totalTracks = 0
    private val pageSize = 50
    private val _isManualRefreshRunning = MutableStateFlow(false)
    val isManualRefreshRunning: StateFlow<Boolean> = _isManualRefreshRunning
    
    init {
        Log.i("LibraryViewModel", "🔌 Initializing LibraryViewModel")
        
        // 1. Observe Database (Single Source of Truth)
        viewModelScope.launch {
            repository.tracks.collect { tracks ->
                Log.i("LibraryViewModel", "💾 Database updated: ${tracks.size} tracks")
                updateLocalState(tracks)
            }
        }

        // Keep stats in sync when connection becomes active, without forcing a full library sync.
        viewModelScope.launch {
            wsClient.isConnected.collect { connected ->
                if (connected) {
                    fetchStats()
                }
            }
        }
    }

    fun refreshLibraryManually() {
        if (_isManualRefreshRunning.value) return

        viewModelScope.launch {
            _isManualRefreshRunning.value = true
            _isLoading.value = _tracks.value.isEmpty()
            try {
                repository.refreshLibrary()
                fetchStats()
            } finally {
                _isLoading.value = false
                _isManualRefreshRunning.value = false
            }
        }
    }
    
    private suspend fun updateLocalState(tracks: List<TrackInfo>) {
        withContext(Dispatchers.Default) {
            val albumModels = tracks.groupBy { track ->
                parseAlbum(track.album, track.discNumber).baseName
            }
                .map { (album, albumTracks) ->
                    AlbumInfo(
                        name = album,
                        artist = albumTracks.firstOrNull()?.artist ?: "",
                        coverUrl = albumTracks.firstOrNull()?.coverUrl,
                        albumMainColor = albumTracks.firstOrNull()?.albumMainColor,
                        songCount = albumTracks.size,
                        nameRomaji = albumTracks.firstOrNull()?.albumRomaji,
                        nameEn = albumTracks.firstOrNull()?.albumEn,
                        artistRomaji = albumTracks.firstOrNull()?.artistRomaji,
                        artistEn = albumTracks.firstOrNull()?.artistEn
                    )
                }
                .sortedBy { it.name }

            val artistModels = tracks.groupBy { it.artist }
                .map { (artist, artistTracks) ->
                    ArtistItemData(
                        name = artist,
                        followerCount = "${artistTracks.size} Tracks",
                        photoUrl = artistTracks.firstOrNull()?.coverUrl,
                        mainColor = artistTracks.firstOrNull()?.albumMainColor,
                        nameRomaji = artistTracks.firstOrNull()?.artistRomaji,
                        nameEn = artistTracks.firstOrNull()?.artistEn
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
                  _albumResults.value = filtered
                      .groupBy { it.album }
                      .map { (albumName, albumTracks) ->
                          AlbumInfo(
                              name = albumName,
                              artist = albumTracks.firstOrNull()?.artist ?: "",
                              coverUrl = albumTracks.firstOrNull()?.coverUrl,
                              albumMainColor = albumTracks.firstOrNull()?.albumMainColor,
                              songCount = albumTracks.size,
                              nameRomaji = albumTracks.firstOrNull()?.albumRomaji,
                              nameEn = albumTracks.firstOrNull()?.albumEn,
                              artistRomaji = albumTracks.firstOrNull()?.artistRomaji,
                              artistEn = albumTracks.firstOrNull()?.artistEn
                          )
                      }
                  _artistResults.value = filtered.map { 
                      ArtistItemData(
                          name = it.artist, 
                          followerCount = "${filtered.count { t -> t.artist == it.artist }} Tracks", 
                          photoUrl = it.coverUrl,
                          mainColor = it.albumMainColor,
                          nameRomaji = it.artistRomaji,
                          nameEn = it.artistEn
                      ) 
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

    fun playTrack(track: TrackInfo) {
        val playbackContext = currentTrackContext()
        playTrack(track, playbackContext)
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

            val albumModels = filtered.groupBy { track ->
                parseAlbum(track.album, track.discNumber).baseName
            }
                .map { (album, albumTracks) ->
                    AlbumInfo(
                        name = album,
                        artist = albumTracks.firstOrNull()?.artist ?: "",
                        coverUrl = albumTracks.firstOrNull()?.coverUrl,
                        albumMainColor = albumTracks.firstOrNull()?.albumMainColor,
                        songCount = albumTracks.size,
                        nameRomaji = albumTracks.firstOrNull()?.albumRomaji,
                        nameEn = albumTracks.firstOrNull()?.albumEn,
                        artistRomaji = albumTracks.firstOrNull()?.artistRomaji,
                        artistEn = albumTracks.firstOrNull()?.artistEn
                    )
                }
                .let { albums -> sortAlbums(albums, _currentAlbumSortOption.value) }

            withContext(Dispatchers.Main) {
                _filteredAlbums.value = albumModels
            }
        }
    }
    
    private fun sortAlbums(albums: List<AlbumInfo>, sortOption: SortOption): List<AlbumInfo> {
        return when (sortOption) {
            SortOption.AlbumDefault -> albums // Original grouping order
            SortOption.AlbumTitleAZ -> albums.sortedBy { it.name.lowercase() }
            SortOption.AlbumTitleZA -> albums.sortedByDescending { it.name.lowercase() }
            SortOption.AlbumArtist -> albums.sortedBy { it.artist.lowercase() }
            SortOption.AlbumSongCountAsc -> albums.sortedWith(
                compareBy<AlbumInfo> { it.songCount }.thenBy { it.name.lowercase() }
            )
            SortOption.AlbumSongCountDesc -> albums.sortedWith(
                compareByDescending<AlbumInfo> { it.songCount }.thenBy { it.name.lowercase() }
            )
            else -> albums
        }
    }
    
    fun setAlbumSortOption(option: SortOption) {
        _currentAlbumSortOption.value = option
        updateFilteredAlbums(_tracks.value, _searchQuery.value)
    }
    
    fun sortTracks(tracks: List<TrackInfo>, sortOption: SortOption): List<TrackInfo> {
        return when (sortOption) {
            SortOption.TrackDefault -> tracks.sortedWith(
                compareBy<TrackInfo> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() }
            )
            SortOption.TrackTitleAZ -> tracks.sortedBy { it.title.lowercase() }
            SortOption.TrackTitleZA -> tracks.sortedByDescending { it.title.lowercase() }
            SortOption.TrackDurationAsc -> tracks.sortedWith(compareBy { it.duration })
            SortOption.TrackDurationDesc -> tracks.sortedWith(compareByDescending { it.duration })
            else -> tracks
        }
    }

    private fun filteredTracks(tracks: List<TrackInfo>, query: String): List<TrackInfo> {
        if (query.isBlank()) return tracks
        return tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    private fun currentTrackContext(): List<TrackInfo> {
        val filtered = filteredTracks(_tracks.value, _searchQuery.value)
        return sortTracks(filtered, _currentTrackSortOption.value)
    }
    
    fun setTrackSortOption(option: SortOption) {
        _currentTrackSortOption.value = option
    }

    fun setArtistSortOption(option: SortOption) {
        _currentArtistSortOption.value = option
    }
}
