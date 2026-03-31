package moe.memesta.vibeon.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import moe.memesta.vibeon.data.DiscoveryRepository

/**
 * Periodic lightweight discovery refresh to keep local device hints warm.
 */
class DiscoverySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = DiscoveryRepository(applicationContext)
        return try {
            repository.startDiscovery()
            delay(DISCOVERY_WINDOW_MS)
            repository.stopDiscovery()
            Log.i(TAG, "Periodic discovery sync completed")
            Result.success()
        } catch (t: Throwable) {
            Log.w(TAG, "Periodic discovery sync failed", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DiscoverySyncWorker"
        private const val DISCOVERY_WINDOW_MS = 5000L
    }
}
