package moe.memesta.vibeon.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.data.LibraryRepository
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.local.TrackDao
import moe.memesta.vibeon.data.local.TrackEntity
import moe.memesta.vibeon.data.stats.PlaybackEvent
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator
import moe.memesta.vibeon.data.stats.StatsTimeRange

class StatsViewModel(
    private val repository: LibraryRepository,
    private val trackDao: TrackDao,
    private val wsClient: WebSocketClient
) : ViewModel() {

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
        refreshWeeklyOverview()
        refreshRange(StatsTimeRange.WEEK)
        viewModelScope.launch {
            wsClient.statsUpdated.collect {
                refreshWeeklyOverview()
                refreshRange(_uiState.value.selectedRange)
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000)
                refreshWeeklyOverview()
                refreshRange(_uiState.value.selectedRange)
            }
        }
    }

    fun onRangeSelected(range: StatsTimeRange) {
        if (range == _uiState.value.selectedRange && !_uiState.value.isLoading) return
        refreshRange(range)
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
                    calculator.loadSummary(range, tracks, events)
                }
            }.getOrNull()
            _uiState.update { it.copy(isLoading = false, summary = summary, selectedRange = range) }
        }
    }

    private suspend fun loadTracks(): List<TrackEntity> {
        cachedTracks?.let { if (it.isNotEmpty()) return it }
        val tracks = trackDao.getAllTracks().first()
        cachedTracks = tracks
        return tracks
    }

    private suspend fun loadEvents(range: StatsTimeRange): List<PlaybackEvent> {
        val now = System.currentTimeMillis()
        val (startMs, endMs) = resolveBounds(range, now)
        return repository.getPlaybackEvents(startMs, endMs) ?: emptyList()
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
