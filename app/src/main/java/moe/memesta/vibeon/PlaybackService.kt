package moe.memesta.vibeon

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.MainScope
import moe.memesta.vibeon.data.P2PDataSource
import moe.memesta.vibeon.data.StreamRepository

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = MainScope()

    companion object {
        var streamRepository: StreamRepository? = null
    }

    override fun onCreate() {
        super.onCreate()
        
        val dataSourceFactory: DataSource.Factory = if (streamRepository != null) {
             DataSource.Factory { P2PDataSource(streamRepository!!, serviceScope) }
        } else {
             DefaultDataSource.Factory(this)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory))
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
