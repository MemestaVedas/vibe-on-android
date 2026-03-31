package moe.memesta.vibeon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VibeonApp : Application() {

    lateinit var container: AppContainer

    companion object {
        lateinit var instance: VibeonApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        // Initialize torrent manager on app startup so persisted torrents restore immediately.
        container.torrentDownloadManager
    }
}
