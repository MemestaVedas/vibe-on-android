package moe.memesta.vibeon.data.stats

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Local JSON-file-backed store for playback events recorded on the mobile device.
 * Adapted from PixelPlayer's PlaybackStatsRepository.
 *
 * Events are stored in `vibe_on_playback_history.json` in the app's files directory and
 * survive app restarts. They are merged with remote (PC) events when computing stats.
 */
class LocalPlaybackStatsRepository(context: Context) {

    private val historyFile = File(context.filesDir, "vibe_on_playback_history.json")
    private val fileLock = Any()

    // ---------------------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------------------

    fun recordPlayback(
        songId: String,
        durationMs: Long,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (songId.isBlank() || durationMs <= 0L) return

        val end = timestamp.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val start = (end - duration).coerceAtLeast(0L)

        val event = PlaybackEvent(
            songId = songId,
            timestamp = end,
            durationMs = end - start,
            startTimestamp = start,
            endTimestamp = end,
            output = "mobile"
        )

        synchronized(fileLock) {
            val events = readEventsLocked().toMutableList()
            // Prune events older than 2 years
            val cutoff = end - MAX_HISTORY_AGE_MS
            if (cutoff > 0) events.removeAll { (it.endTimestamp ?: it.timestamp) < cutoff }
            events += event
            writeEventsLocked(events)
        }

        Log.d(TAG, "📊 Recorded local play: $songId — ${durationMs}ms")
    }

    // ---------------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------------

    fun readEvents(): List<PlaybackEvent> = synchronized(fileLock) { readEventsLocked() }

    private fun readEventsLocked(): List<PlaybackEvent> {
        if (!historyFile.exists()) return emptyList()

        val raw = runCatching { historyFile.readText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    PlaybackEvent(
                        songId = obj.optString("songId"),
                        timestamp = obj.optLong("timestamp"),
                        durationMs = obj.optLong("durationMs"),
                        startTimestamp = if (obj.has("startTimestamp")) obj.getLong("startTimestamp") else null,
                        endTimestamp = if (obj.has("endTimestamp")) obj.getLong("endTimestamp") else null,
                        output = obj.optString("output", "mobile")
                    ).takeIf { it.songId.isNotBlank() }
                } catch (e: JSONException) {
                    null
                }
            }
        }.getOrElse {
            Log.w(TAG, "Failed to parse local playback history: ${it.message}")
            emptyList()
        }
    }

    // ---------------------------------------------------------------------------
    // Persist
    // ---------------------------------------------------------------------------

    private fun writeEventsLocked(events: List<PlaybackEvent>) {
        runCatching {
            historyFile.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }
            val arr = JSONArray()
            events.forEach { event ->
                val obj = JSONObject().apply {
                    put("songId", event.songId)
                    put("timestamp", event.timestamp)
                    put("durationMs", event.durationMs)
                    event.startTimestamp?.let { put("startTimestamp", it) }
                    event.endTimestamp?.let { put("endTimestamp", it) }
                    put("output", event.output)
                }
                arr.put(obj)
            }
            historyFile.writeText(arr.toString())
        }.onFailure {
            Log.e(TAG, "Failed to persist local playback history", it)
        }
    }

    companion object {
        private const val TAG = "LocalStatsRepo"
        private val MAX_HISTORY_AGE_MS = TimeUnit.DAYS.toMillis(730) // ~2 years
    }
}
