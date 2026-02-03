package moe.memesta.vibeon

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
        lateinit var streamRepository: StreamRepository
    }

    override fun onCreate() {
        super.onCreate()
        
        val dataSourceFactory = {
            P2PDataSource(streamRepository, serviceScope)
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
