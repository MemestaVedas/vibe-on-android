package moe.memesta.vibeon.data.stats

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.MediaSessionData
import java.util.concurrent.TimeUnit

/**
 * Tracks listening sessions observed via the WebSocket player state.
 * Adapted from PixelPlayer's ListeningStatsTracker.
 *
 * Watches [currentTrack] and [isPlayingFlow] flows from [WebSocketClient], accumulates
 * wall-clock time while [isPlayingFlow] is true, and records a [PlaybackEvent] via
 * [statsRepository] whenever a song session ends (song change or disconnection).
 *
 * Designed to be owned by [ConnectionViewModel]. Call [start] once after construction and
 * [stop] when the connection is torn down (or in [ConnectionViewModel.onCleared]).
 */
class LocalStatsTracker(
    private val statsRepository: LocalPlaybackStatsRepository,
    private val currentTrack: StateFlow<MediaSessionData>,
    private val isPlayingFlow: StateFlow<Boolean>,
    private val scope: CoroutineScope
) {

    @Volatile
    private var currentSession: LocalActiveSession? = null

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    fun start() {
        // Watch for track changes — new song → finalize old session, start new one
        scope.launch {
            currentTrack.collect { track ->
                val path = track.path
                val sessionSongId = currentSession?.songId
                if (path != sessionSongId) {
                    finalizeCurrentSession()
                    if (path.isNotBlank() && track.title != "No Track") {
                        startSession(path)
                    }
                }
            }
        }

        // Watch for play/pause toggles — accumulate time while playing
        scope.launch {
            isPlayingFlow.collect { playing ->
                val session = currentSession ?: return@collect
                val nowRealtime = SystemClock.elapsedRealtime()
                if (session.isPlaying) {
                    session.accumulatedMs += (nowRealtime - session.lastRealtimeMs).coerceAtLeast(0L)
                }
                session.isPlaying = playing
                session.lastRealtimeMs = nowRealtime
            }
        }
    }

    /**
     * Finalizes the current session and stops tracking. Call when disconnecting.
     */
    fun stop() {
        finalizeCurrentSession()
    }

    // ---------------------------------------------------------------------------
    // Internal session management
    // ---------------------------------------------------------------------------

    private fun startSession(songId: String) {
        currentSession = LocalActiveSession(
            songId = songId,
            startEpochMs = System.currentTimeMillis(),
            accumulatedMs = 0L,
            lastRealtimeMs = SystemClock.elapsedRealtime(),
            isPlaying = isPlayingFlow.value
        )
        Log.d(TAG, "▶ Local session started: $songId")
    }

    private fun finalizeCurrentSession() {
        val session = currentSession ?: return

        // Accumulate any remaining time if still playing at finalize time
        val nowRealtime = SystemClock.elapsedRealtime()
        if (session.isPlaying) {
            session.accumulatedMs += (nowRealtime - session.lastRealtimeMs).coerceAtLeast(0L)
        }
        currentSession = null

        val listened = session.accumulatedMs.coerceAtLeast(0L)
        if (listened < MIN_SESSION_LISTEN_MS) {
            Log.d(TAG, "⏭ Ignored short session: ${session.songId} — ${listened}ms < ${MIN_SESSION_LISTEN_MS}ms")
            return
        }

        val timestamp = (session.startEpochMs + listened).coerceAtMost(System.currentTimeMillis())
        Log.d(TAG, "⏹ Local session finalized: ${session.songId} — ${listened}ms")

        scope.launch(Dispatchers.IO) {
            statsRepository.recordPlayback(session.songId, listened, timestamp)
        }
    }

    companion object {
        private const val TAG = "LocalStatsTracker"
        private val MIN_SESSION_LISTEN_MS = TimeUnit.SECONDS.toMillis(5) // must listen ≥ 5 s
    }
}

/**
 * Represents an in-progress listening session for a single song.
 */
data class LocalActiveSession(
    val songId: String,
    val startEpochMs: Long,
    var accumulatedMs: Long,
    var lastRealtimeMs: Long,
    var isPlaying: Boolean
)
