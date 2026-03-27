package moe.memesta.vibeon.ui

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.model.UnifiedSong
import moe.memesta.vibeon.data.model.normalizeTrackString

data class OfflineSong(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Long,
    val uri: String,
    val duration: Long
)

class OfflineSongsViewModel(application: Application) : AndroidViewModel(application) {
    private val _songs = MutableStateFlow<List<OfflineSong>>(emptyList())
    val songs: StateFlow<List<OfflineSong>> = _songs

    private val player = ExoPlayer.Builder(application).build()
    
    private val _currentSong = MutableStateFlow<OfflineSong?>(null)
    val currentSong: StateFlow<OfflineSong?> = _currentSong
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    /** Unified view merging offline songs + server library. Updated by [mergeWithServerLibrary]. */
    private val _unifiedSongs = MutableStateFlow<List<UnifiedSong>>(emptyList())
    val unifiedSongs: StateFlow<List<UnifiedSong>> = _unifiedSongs

    /** Currently playing unified song (for the mini player). */
    private val _currentUnified = MutableStateFlow<UnifiedSong?>(null)
    val currentUnified: StateFlow<UnifiedSong?> = _currentUnified

    init {
        loadSongs()
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun loadSongs() {
        viewModelScope.launch {
            val loadedSongs = withContext(Dispatchers.IO) {
                val songsList = mutableListOf<OfflineSong>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                
                try {
                    getApplication<Application>().contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        "${MediaStore.Audio.Media.TITLE} ASC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            songsList.add(
                                OfflineSong(
                                    id = id,
                                    title = cursor.getString(titleCol) ?: "Unknown Title",
                                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                                    albumId = cursor.getLong(albumIdCol),
                                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                                    duration = cursor.getLong(durationCol)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                songsList
            }
            _songs.value = loadedSongs
            // Build initial unified list (offline-only, no server tracks yet)
            _unifiedSongs.value = buildUnifiedList(loadedSongs, emptyList())
        }
    }

    /**
     * Call this whenever the server library updates. Merges offline + online tracks,
     * deduplicating by normalized (title + artist). Safe to call frequently
     * (runs on IO dispatcher, result pushed via StateFlow).
     */
    fun mergeWithServerLibrary(serverTracks: List<TrackInfo>) {
        viewModelScope.launch(Dispatchers.Default) {
            val merged = buildUnifiedList(_songs.value, serverTracks)
            _unifiedSongs.value = merged
        }
    }

    /**
     * Produce a deduplicated list of [UnifiedSong]:
     * - Offline songs matched to a server track → isOfflineAvailable = true, serverPath set
     * - Offline songs with no server match → offline only
     * Server-only tracks are intentionally excluded (they live in the main Library screen).
     */
    private fun buildUnifiedList(
        offline: List<OfflineSong>,
        server: List<TrackInfo>
    ): List<UnifiedSong> {
        // Build a lookup map: (normalizedTitle, normalizedArtist) → TrackInfo
        val serverIndex = server.associateBy { track ->
            normalizeTrackString(track.title) to normalizeTrackString(track.artist)
        }

        return offline.map { song ->
            val key = normalizeTrackString(song.title) to normalizeTrackString(song.artist)
            val matched = serverIndex[key]
            UnifiedSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                albumId = song.albumId,
                duration = song.duration,
                localUri = song.uri,
                serverPath = matched?.path,
                coverUrl = matched?.coverUrl,
                isOfflineAvailable = true
            )
        }
    }

    fun playSong(song: OfflineSong) {
        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    /** Play a unified song — prefers server path if available, falls back to local URI. */
    fun playUnified(song: UnifiedSong, connectionViewModel: ConnectionViewModel? = null) {
        _currentUnified.value = song
        if (song.serverPath != null && connectionViewModel != null) {
            // Delegate to the server (desktop playback)
            connectionViewModel.playTrack(song.serverPath)
        } else if (song.localUri != null) {
            // Offline-only: use ExoPlayer
            val offline = OfflineSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                albumId = song.albumId,
                uri = song.localUri,
                duration = song.duration
            )
            playSong(offline)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
