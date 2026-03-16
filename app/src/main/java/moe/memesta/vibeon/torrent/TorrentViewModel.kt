package moe.memesta.vibeon.torrent

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.VibeonApp
import java.io.File

sealed class TorrentUiState {
    object Idle : TorrentUiState()
    object Searching : TorrentUiState()
    data class Results(val items: List<NyaaSearchResult>) : TorrentUiState()
    data class Error(val message: String) : TorrentUiState()
}

class TorrentViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val storagePreferences = VibeonApp.instance.container.torrentStoragePreferences
    val downloadManager = VibeonApp.instance.container.torrentDownloadManager

    private val _searchState = MutableStateFlow<TorrentUiState>(TorrentUiState.Idle)
    val searchState: StateFlow<TorrentUiState> = _searchState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow(NyaaCategory.AUDIO)
    val category: StateFlow<NyaaCategory> = _category.asStateFlow()

    private val _sort = MutableStateFlow(NyaaSort.SEEDERS)
    val sort: StateFlow<NyaaSort> = _sort.asStateFlow()

    private val _order = MutableStateFlow(NyaaOrder.DESC)
    val order: StateFlow<NyaaOrder> = _order.asStateFlow()

    private val _savePath = MutableStateFlow(storagePreferences.savePath)
    val savePath: StateFlow<String?> = _savePath.asStateFlow()

    val downloads = downloadManager.downloads
    val lastDownloadError = downloadManager.lastError

    // Default download directory: Music/Vibe-On/
    val defaultSavePath: String
        get() {
            val musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: appContext.filesDir
            return File(musicDir, "Vibe-On").absolutePath
        }

    fun hasConfiguredSavePath(): Boolean = !_savePath.value.isNullOrBlank()

    fun setSavePath(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) return

        val dir = File(normalized)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        storagePreferences.savePath = dir.absolutePath
        _savePath.value = dir.absolutePath
    }

    fun setSavePathFromTreeUri(uri: Uri): Boolean {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }

        val path = treeUriToPath(uri) ?: return false
        setSavePath(path)
        return true
    }

    fun getEffectiveSavePath(): String = _savePath.value ?: defaultSavePath

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: NyaaCategory) { _category.value = c }
    fun setSort(s: NyaaSort) { _sort.value = s }
    fun setOrder(o: NyaaOrder) { _order.value = o }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        _searchState.value = TorrentUiState.Searching
        viewModelScope.launch {
            try {
                val results = NyaaSearchService.search(q, _category.value, _sort.value, _order.value)
                _searchState.value = if (results.isEmpty()) {
                    TorrentUiState.Error("No results found for \"$q\"")
                } else {
                    TorrentUiState.Results(results)
                }
            } catch (e: Exception) {
                _searchState.value = TorrentUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun addMagnet(magnetLink: String, savePath: String = getEffectiveSavePath()) {
        downloadManager.addMagnet(magnetLink, savePath)
    }

    fun pauseDownload(id: String) = downloadManager.pause(id)
    fun resumeDownload(id: String) = downloadManager.resume(id)
    fun removeDownload(id: String, deleteFiles: Boolean) = downloadManager.remove(id, deleteFiles)
    fun clearDownloadError() = downloadManager.clearLastError()

    override fun onCleared() {
        super.onCleared()
    }

    private fun treeUriToPath(uri: Uri): String? {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
        val parts = docId.split(":", limit = 2)
        if (parts.isEmpty()) return null

        val volume = parts[0]
        val relative = if (parts.size > 1) parts[1] else ""

        val base = when (volume.lowercase()) {
            "primary" -> Environment.getExternalStorageDirectory().absolutePath
            else -> "/storage/$volume"
        }

        return if (relative.isBlank()) base else "$base/$relative"
    }
}
