package moe.memesta.vibeon.torrent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.memesta.vibeon.R
import moe.memesta.vibeon.VibeonApp
import kotlin.math.roundToInt

class TorrentDownloadService : Service() {

    companion object {
        const val ACTION_START = "moe.memesta.vibeon.torrent.START"
        private const val CHANNEL_ID = "torrent_downloads"
        private const val CHANNEL_NAME = "Torrent Downloads"
        private const val NOTIFICATION_ID = 4102
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification(title = "Torrent downloads", text = "Preparing torrent session..."))

        val downloadManager = VibeonApp.instance.container.torrentDownloadManager
        serviceScope.launch {
            downloadManager.downloads.collectLatest { downloads ->
                if (downloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }

                val active = downloads.firstOrNull {
                    it.state == TorrentState.DOWNLOADING || it.state == TorrentState.DOWNLOADING_METADATA
                } ?: downloads.first()

                val percent = (active.progress * 100f).roundToInt().coerceIn(0, 100)
                val totalSpeed = downloads.sumOf { it.downloadSpeed }

                val stateText = when (active.state) {
                    TorrentState.DOWNLOADING_METADATA -> "Fetching metadata..."
                    TorrentState.DOWNLOADING -> "${formatSpeed(totalSpeed)} • $percent%"
                    TorrentState.FINISHED -> "Finished"
                    TorrentState.SEEDING -> "Seeding"
                    TorrentState.PAUSED -> "Paused"
                    TorrentState.ERROR -> active.error ?: "Error"
                    TorrentState.CHECKING_FILES -> "Checking files..."
                }

                val text = if (downloads.size == 1) {
                    stateText
                } else {
                    "$stateText • ${downloads.size} torrents"
                }

                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = active.name,
                        text = text,
                        progress = percent,
                        indeterminate = active.state == TorrentState.DOWNLOADING_METADATA
                    )
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        title: String,
        text: String,
        progress: Int = 0,
        indeterminate: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.finalmono)

        // Add a slightly scaled large icon so the logo appears a bit bigger in the
        // notification content (scale ~1.25x). Small icon size is controlled by
        // the system; using a large icon gives a larger presence in the shade.
        try {
            val scale = 1.25f
            val d: Drawable? = ContextCompat.getDrawable(this, R.drawable.finalmono)
            if (d != null) {
                val w = (d.intrinsicWidth * scale).toInt().coerceAtLeast(1)
                val h = (d.intrinsicHeight * scale).toInt().coerceAtLeast(1)
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bm)
                d.setBounds(0, 0, w, h)
                d.draw(canvas)
                builder.setLargeIcon(bm)
            }
        } catch (t: Throwable) {
            // If anything goes wrong, ignore and continue with the small icon only.
        }

        builder.setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (indeterminate) {
            builder.setProgress(100, 0, true)
        } else {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Torrent download progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0L) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var value = bytesPerSecond.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        val rounded = if (value >= 100) "%.0f" else if (value >= 10) "%.1f" else "%.2f"
        return rounded.format(value) + " " + units[unitIndex]
    }
}
