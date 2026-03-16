package moe.memesta.vibeon.torrent

import android.content.Intent
import android.os.Build
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentStatus
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

data class TorrentDownload(
    val id: String,             // info-hash hex
    val name: String,
    val magnetLink: String,
    val savePath: String,
    val progress: Float,        // 0f–1f
    val downloadSpeed: Long,    // bytes/sec
    val uploadSpeed: Long,
    val seeds: Int,
    val peers: Int,
    val totalSize: Long,
    val downloadedSize: Long,
    val state: TorrentState,
    val error: String? = null
)

enum class TorrentState {
    CHECKING_FILES,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    PAUSED,
    ERROR
}

class TorrentDownloadManager(
    private val context: Context,
    private val settingsRepository: TorrentSessionSettingsRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = SessionManager()

    private val _downloads = MutableStateFlow<List<TorrentDownload>>(emptyList())
    val downloads: StateFlow<List<TorrentDownload>> = _downloads.asStateFlow()

    private val _sessionSettings = MutableStateFlow(settingsRepository.load())
    val sessionSettings: StateFlow<TorrentSessionSettings> = _sessionSettings.asStateFlow()

    /** Maps info-hash → magnet link so we can reconstruct state */
    private val magnetMap = mutableMapOf<String, String>()
    private val persistedDownloads = mutableListOf<PersistedTorrent>()

    // Lifted from desktop torrent backend defaults to improve peer discovery.
    private val fallbackTrackers = listOf(
        "udp://tracker.openbittorrent.com:6969/announce",
        "http://tracker.openbittorrent.com:80/announce",
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://open.stealth.si:80/announce"
    )

    private val stateFile = File(context.filesDir, "torrent_state.json")

    init {
        val settings = buildSettingsPack(_sessionSettings.value)
        session.start(SessionParams(settings))
        loadPersistedState()
        restorePersistedDownloads()

        session.addListener(object : AlertListener {
            override fun types(): IntArray? = null // listen to all
            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.TORRENT_FINISHED -> {
                        refreshDownloads()
                    }
                    AlertType.TORRENT_ERROR -> {
                        refreshDownloads()
                    }
                    else -> {}
                }
            }
        })

        // Poll status every 1.5 s
        scope.launch {
            while (true) {
                delay(1500)
                refreshDownloads()
            }
        }
    }

    fun addMagnet(magnetLink: String, savePath: String) {
        val dir = File(savePath).apply { mkdirs() }
        val normalizedMagnet = withFallbackTrackers(magnetLink)
        session.download(normalizedMagnet, dir, torrent_flags_t())
        upsertPersisted(normalizedMagnet, dir.absolutePath)
        ensureForegroundService()
        refreshDownloads()
    }

    fun pause(id: String) {
        findHandle(id)?.pause()
        refreshDownloads()
    }

    fun resume(id: String) {
        findHandle(id)?.resume()
        refreshDownloads()
    }

    fun remove(id: String, deleteFiles: Boolean) {
        val handle = findHandle(id) ?: return
        val magnet = magnetMap[id] ?: handle.makeMagnetUri().orEmpty()
        if (deleteFiles) {
            session.remove(handle, SessionHandle.DELETE_FILES)
        } else {
            session.remove(handle)
        }
        magnetMap.remove(id)
        if (magnet.isNotBlank()) {
            removePersistedByMagnet(magnet)
        }
        refreshDownloads()
    }

    fun stop() {
        session.stop()
    }

    fun updateSessionSettings(settings: TorrentSessionSettings) {
        val normalized = settings.normalized()
        settingsRepository.save(normalized)
        _sessionSettings.value = normalized
        // Libtorrent setting APIs vary by binding; apply only if methods are available.
        runCatching {
            session.swig().applySettings(buildSettingsPack(normalized).swig())
        }
    }

    private fun findHandle(id: String): TorrentHandle? {
        return session.find(org.libtorrent4j.Sha1Hash.parseHex(id))
    }

    private fun refreshDownloads() {
        val handles = SessionHandle(session.swig()).torrents()
        val list = handles.map { handle ->
            val status = handle.status()
            val infoHash = handle.infoHash().toHex()
            val name = handle.name.ifBlank { "Downloading metadata..." }
            val magnet = magnetMap.getOrElse(infoHash) {
                handle.makeMagnetUri().also { if (it.isNotBlank()) magnetMap[infoHash] = it }
            }

            val state = when (status.state()) {
                TorrentStatus.State.CHECKING_FILES,
                TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING_FILES
                TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
                TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
                TorrentStatus.State.FINISHED -> TorrentState.FINISHED
                TorrentStatus.State.SEEDING -> TorrentState.SEEDING
                else -> {
                    val paused = status.flags().and_(org.libtorrent4j.TorrentFlags.PAUSED)
                        .ne(torrent_flags_t())
                    if (paused) TorrentState.PAUSED else TorrentState.DOWNLOADING
                }
            }

            TorrentDownload(
                id = infoHash,
                name = name,
                magnetLink = magnet,
                savePath = handle.savePath(),
                progress = status.progress(),
                downloadSpeed = status.downloadPayloadRate().toLong(),
                uploadSpeed = status.uploadPayloadRate().toLong(),
                seeds = status.numSeeds(),
                peers = status.numPeers(),
                totalSize = status.totalWanted(),
                downloadedSize = status.totalWantedDone(),
                state = state,
                error = null
            )
        }
        _downloads.value = list
    }

    private fun restorePersistedDownloads() {
        if (persistedDownloads.isEmpty()) return

        persistedDownloads.forEach { entry ->
            val dir = File(entry.savePath).apply { mkdirs() }
            runCatching {
                session.download(entry.magnetLink, dir, torrent_flags_t())
            }
        }

        ensureForegroundService()
        refreshDownloads()
    }

    private fun ensureForegroundService() {
        val intent = Intent(context, TorrentDownloadService::class.java).apply {
            action = TorrentDownloadService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun upsertPersisted(magnetLink: String, savePath: String) {
        val existingIndex = persistedDownloads.indexOfFirst { it.magnetLink == magnetLink }
        if (existingIndex >= 0) {
            persistedDownloads[existingIndex] = PersistedTorrent(magnetLink, savePath)
        } else {
            persistedDownloads.add(PersistedTorrent(magnetLink, savePath))
        }
        savePersistedState()
    }

    private fun removePersistedByMagnet(magnetLink: String) {
        val before = persistedDownloads.size
        persistedDownloads.removeAll { it.magnetLink == magnetLink }
        if (persistedDownloads.size != before) {
            savePersistedState()
        }
    }

    private fun loadPersistedState() {
        if (!stateFile.exists()) return
        runCatching {
            val json = stateFile.readText()
            val root = JSONObject(json)
            val arr = root.optJSONArray("downloads") ?: JSONArray()
            persistedDownloads.clear()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val magnet = item.optString("magnet", "")
                val path = item.optString("savePath", "")
                if (magnet.isNotBlank() && path.isNotBlank()) {
                    persistedDownloads.add(PersistedTorrent(magnet, path))
                }
            }
        }
    }

    private fun savePersistedState() {
        runCatching {
            val arr = JSONArray()
            persistedDownloads.forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("magnet", entry.magnetLink)
                        put("savePath", entry.savePath)
                    }
                )
            }
            val root = JSONObject().apply { put("downloads", arr) }
            stateFile.writeText(root.toString())
        }
    }

    private fun withFallbackTrackers(magnetLink: String): String {
        if (!magnetLink.startsWith("magnet:?", ignoreCase = true)) return magnetLink

        val existing = "&tr=" in magnetLink || "?tr=" in magnetLink
        if (existing) return magnetLink

        val trackers = fallbackTrackers.joinToString(separator = "") {
            "&tr=${URLEncoder.encode(it, "UTF-8")}"
        }
        return magnetLink + trackers
    }

    private fun buildSettingsPack(sessionSettings: TorrentSessionSettings): SettingsPack {
        val normalized = sessionSettings.normalized()
        return SettingsPack().apply {
            connectionsLimit(normalized.connectionsLimit)
            activeDownloads(normalized.activeDownloads)
            activeSeeds(normalized.activeSeeds)
            setEnableDht(normalized.enableDht)
            applyOptionalIntSetting("downloadRateLimit", normalized.maxDownloadRateBytes)
            applyOptionalIntSetting("uploadRateLimit", normalized.maxUploadRateBytes)
            applyOptionalBooleanSetting("setEnableLsd", normalized.enableLsd)
            applyOptionalBooleanSetting("setEnableUtp", normalized.enableUtp)
        }
    }

    private fun SettingsPack.applyOptionalIntSetting(methodName: String, value: Int) {
        runCatching {
            javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
                .invoke(this, value)
        }
    }

    private fun SettingsPack.applyOptionalBooleanSetting(methodName: String, value: Boolean) {
        runCatching {
            javaClass.getMethod(methodName, Boolean::class.javaPrimitiveType)
                .invoke(this, value)
        }
    }

    private data class PersistedTorrent(
        val magnetLink: String,
        val savePath: String
    )
}
