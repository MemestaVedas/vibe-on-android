package moe.memesta.vibeon.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import moe.memesta.vibeon.data.LibraryRepository
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.local.TrackDao
import moe.memesta.vibeon.data.local.TrackEntity
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository
import moe.memesta.vibeon.data.stats.PlaybackEvent
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator
import moe.memesta.vibeon.data.stats.StatsTimeRange

class StatsViewModel(
    private val repository: LibraryRepository,
    private val trackDao: TrackDao,
    private val wsClient: WebSocketClient,
    private val localStatsRepository: LocalPlaybackStatsRepository
) : ViewModel() {

    val mediaBaseUrl: String = repository.baseUrl

    data class StatsUiState(
        val selectedRange: StatsTimeRange = StatsTimeRange.WEEK,
        val isLoading: Boolean = true,
        val summary: PlaybackStatsCalculator.PlaybackStatsSummary? = null,
        val availableRanges: List<StatsTimeRange> = StatsTimeRange.values().toList()
    )

    private val calculator = PlaybackStatsCalculator()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _weeklyOverview = MutableStateFlow<PlaybackStatsCalculator.PlaybackStatsSummary?>(null)
    val weeklyOverview: StateFlow<PlaybackStatsCalculator.PlaybackStatsSummary?> = _weeklyOverview.asStateFlow()

    @Volatile
    private var cachedTracks: List<TrackEntity>? = null

    init {
        Log.d("StatsViewModel", "🎯 StatsViewModel initialized")
        refreshWeeklyOverview()
        refreshRange(StatsTimeRange.WEEK)
        viewModelScope.launch {
            wsClient.statsUpdated.collect {
                Log.d("StatsViewModel", "📊 Stats update signal received from PC")
                refreshWeeklyOverview()
                refreshRange(_uiState.value.selectedRange)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(30000)
                Log.d("StatsViewModel", "🔄 Auto-refreshing stats...")
                refreshWeeklyOverview()
                refreshRange(_uiState.value.selectedRange)
            }
        }
    }

    fun onRangeSelected(range: StatsTimeRange) {
        if (range == _uiState.value.selectedRange && !_uiState.value.isLoading) return
        Log.d("StatsViewModel", "📅 Range selected: ${range.displayName}")
        refreshRange(range)
    }

    fun forceRefresh() {
        Log.d("StatsViewModel", "🔄 Force refresh triggered")
        cachedTracks = null
        refreshWeeklyOverview()
        refreshRange(_uiState.value.selectedRange)
    }

    private fun refreshWeeklyOverview() {
        viewModelScope.launch {
            val summary = runCatching {
                withContext(Dispatchers.IO) {
                    val tracks = loadTracks()
                    val events = loadEvents(StatsTimeRange.WEEK)
                    calculator.loadSummary(StatsTimeRange.WEEK, tracks, events)
                }
            }.getOrNull()
            _weeklyOverview.value = summary
        }
    }

    private fun refreshRange(range: StatsTimeRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRange = range) }
            val summary = runCatching {
                withContext(Dispatchers.IO) {
                    val tracks = loadTracks()
                    val events = loadEvents(range)
                    Log.d("StatsViewModel", "📊 Calculating summary for ${range.displayName}...")
                    Log.d("StatsViewModel", "  Tracks: ${tracks.size}, Events: ${events.size}")
                    calculator.loadSummary(range, tracks, events)
                }
            }.onSuccess { summary ->
                Log.d("StatsViewModel", "✅ Summary loaded: ${summary.totalDurationMs}ms, ${summary.totalPlayCount} plays")
            }.onFailure { e ->
                Log.e("StatsViewModel", "❌ Failed to load summary", e)
            }.getOrNull()
            _uiState.update { it.copy(isLoading = false, summary = summary, selectedRange = range) }
        }
    }

    private suspend fun loadTracks(): List<TrackEntity> {
        cachedTracks?.let { if (it.isNotEmpty()) return it }
        val tracks = trackDao.getTracksDeduped().first()
        Log.d("StatsViewModel", "📚 Loaded ${tracks.size} tracks from DB")
        cachedTracks = tracks
        return tracks
    }

    private suspend fun loadEvents(range: StatsTimeRange): List<PlaybackEvent> {
        val now = System.currentTimeMillis()
        val (startMs, endMs) = resolveBounds(range, now)

        Log.d("StatsViewModel", "📅 Loading events for ${range.displayName}: $startMs to $endMs")

        // Fetch remote events from the PC server (now contains both desktop + synced mobile events)
        val remoteEvents: List<PlaybackEvent> = runCatching {
            repository.getPlaybackEvents(startMs, endMs)
        }.onSuccess { events ->
            Log.d("StatsViewModel", "🖥️ Remote events (desktop + mobile): ${events?.size ?: 0}")
        }.onFailure { e ->
            Log.w("StatsViewModel", "⚠️ Failed to fetch remote events: ${e.message}")
        }.getOrNull() ?: emptyList()

        // Load local (mobile-recorded) events as fallback for any not yet synced to PC
        val lowerBound = startMs ?: Long.MIN_VALUE
        val allLocal = runCatching {
            localStatsRepository.readEvents()
        }.onFailure { e ->
            Log.e("StatsViewModel", "❌ Failed to read local events", e)
        }.getOrElse { emptyList() }
        
        val localEvents = allLocal.filter { event ->
            val end = event.endTimestamp ?: event.timestamp
            val start = event.startTimestamp ?: (end - event.durationMs)
            end >= lowerBound && start <= endMs
        }

        // Deduplicate: remote DB already has synced mobile events,
        // so only add local events not present in the remote set
        val remoteKeys = remoteEvents.map { Triple(it.songId, it.timestamp, it.durationMs) }.toSet()
        val uniqueLocal = localEvents.filter { ev ->
            Triple(ev.songId, ev.timestamp, ev.durationMs) !in remoteKeys
        }
        Log.d("StatsViewModel", "📱 Local-only (unsynced) events: ${uniqueLocal.size}")

        val combined = remoteEvents + uniqueLocal
        Log.d("StatsViewModel", "📊 Total events: ${combined.size}")
        return combined
    }

    private fun resolveBounds(range: StatsTimeRange, nowMillis: Long): Pair<Long?, Long> {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis)
        return when (range) {
            StatsTimeRange.DAY -> {
                val start = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to nowMillis
            }
            StatsTimeRange.WEEK -> {
                val start = now.atZone(zoneId)
                    .toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                start to nowMillis
            }
            StatsTimeRange.MONTH -> {
                val start = YearMonth.from(now.atZone(zoneId))
                    .atDay(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                start to nowMillis
            }
            StatsTimeRange.YEAR -> {
                val start = now.atZone(zoneId)
                    .toLocalDate()
                    .withDayOfYear(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                start to nowMillis
            }
            StatsTimeRange.ALL -> null to nowMillis
        }
    }
}
