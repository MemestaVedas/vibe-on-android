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
        }
    }

    fun playSong(song: OfflineSong) {
        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
