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

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

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
        val normalizedInput = magnetLink.trim()
        if (!isValidMagnet(normalizedInput)) {
            _lastError.value = "Invalid magnet link."
            return
        }

        if (isDuplicateMagnet(normalizedInput)) {
            _lastError.value = "This torrent is already in your downloads list."
            return
        }

        val dir = File(savePath)
        if (!dir.exists() && !dir.mkdirs()) {
            _lastError.value = "Unable to create download folder."
            return
        }

        val normalizedMagnet = withFallbackTrackers(magnetLink)
        val added = runCatching {
            session.download(normalizedMagnet, dir, torrent_flags_t())
        }.isSuccess

        if (!added) {
            _lastError.value = "Failed to start torrent session for this magnet."
            return
        }

        _lastError.value = null
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
        val handle = findHandle(id)
        val magnet = magnetMap[id] ?: handle?.makeMagnetUri().orEmpty()

        if (handle != null) {
            val removed = runCatching {
                if (deleteFiles) {
                    session.remove(handle, SessionHandle.DELETE_FILES)
                } else {
                    session.remove(handle)
                }
            }.isSuccess

            if (!removed) {
                _lastError.value = "Failed to remove torrent from active session."
            }
        }

        magnetMap.remove(id)
        removePersistedForTorrent(id = id, magnetLink = magnet)
        _downloads.value = _downloads.value.filterNot { it.id == id }
        refreshDownloads()
    }

    fun stop() {
        session.stop()
    }

    fun clearLastError() {
        _lastError.value = null
    }

    fun updateSessionSettings(settings: TorrentSessionSettings) {
        val normalized = settings.normalized()
        settingsRepository.save(normalized)
        _sessionSettings.value = normalized
        // Libtorrent setting APIs vary by binding; apply only if methods are available.
        applySettingsIfSupported(buildSettingsPack(normalized))
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
            val restored = runCatching {
                session.download(entry.magnetLink, dir, torrent_flags_t())
            }.isSuccess

            if (!restored) {
                _lastError.value = "Some saved torrents could not be restored after restart."
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

    private fun removePersistedForTorrent(id: String, magnetLink: String?) {
        val normalizedId = id.trim().lowercase()
        val magnetIdentity = magnetLink
            ?.takeIf { it.isNotBlank() }
            ?.let { magnetIdentityKey(it) }

        val before = persistedDownloads.size
        persistedDownloads.removeAll { persisted ->
            val persistedIdentity = magnetIdentityKey(persisted.magnetLink)
            persistedIdentity == normalizedId || (magnetIdentity != null && persistedIdentity == magnetIdentity)
        }

        if (persistedDownloads.size != before) {
            savePersistedState()
            return
        }

        if (!magnetLink.isNullOrBlank()) {
            removePersistedByMagnet(magnetLink)
        }
    }

    private fun loadPersistedState() {
        if (!stateFile.exists()) return
        runCatching {
            val json = stateFile.readText()
            val root = JSONObject(json)
            val arr = root.optJSONArray("downloads") ?: JSONArray()
            persistedDownloads.clear()
            val seen = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val magnet = item.optString("magnet", "")
                val path = item.optString("savePath", "")
                if (magnet.isBlank() || path.isBlank()) {
                    continue
                }
                if (!isValidMagnet(magnet)) {
                    continue
                }
                val identity = magnetIdentityKey(magnet)
                if (identity in seen) {
                    continue
                }
                seen.add(identity)
                if (File(path).exists() || File(path).mkdirs()) {
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
            val temp = File(stateFile.parentFile, "${stateFile.name}.tmp")
            temp.writeText(root.toString())
            if (stateFile.exists()) {
                stateFile.delete()
            }
            val moved = temp.renameTo(stateFile)
            if (!moved) {
                stateFile.writeText(root.toString())
                temp.delete()
            }
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

    private fun isValidMagnet(magnetLink: String): Boolean {
        if (!magnetLink.startsWith("magnet:?", ignoreCase = true)) return false
        val lower = magnetLink.lowercase()
        return lower.contains("xt=urn:btih:") || lower.contains("xt=urn:btmh:")
    }

    private fun isDuplicateMagnet(magnetLink: String): Boolean {
        val incomingKey = magnetIdentityKey(magnetLink)
        val existingKeys = buildSet {
            magnetMap.values.forEach { add(magnetIdentityKey(it)) }
            persistedDownloads.forEach { add(magnetIdentityKey(it.magnetLink)) }
        }
        return incomingKey in existingKeys
    }

    private fun magnetIdentityKey(magnetLink: String): String {
        val normalized = magnetLink.trim()
        val match = Regex("[?&]xt=urn:(?:btih|btmh):([^&]+)", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
            ?.trim()

        return if (match.isNullOrBlank()) normalized.lowercase() else match
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

    private fun applySettingsIfSupported(settingsPack: SettingsPack) {
        val method = session.javaClass.methods.firstOrNull { candidate ->
            candidate.name == "applySettings" && candidate.parameterTypes.size == 1
        } ?: return

        runCatching {
            method.invoke(session, settingsPack)
        }
    }

    private data class PersistedTorrent(
        val magnetLink: String,
        val savePath: String
    )
}
