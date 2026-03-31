package moe.memesta.vibeon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import moe.memesta.vibeon.data.worker.SyncScheduler

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

        // Keep discovery hints warm in the background without changing foreground UX.
        SyncScheduler.schedulePeriodicDiscoverySync(this)
    }
}
