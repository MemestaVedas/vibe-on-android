package moe.memesta.vibeon.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.memesta.vibeon.torrent.NyaaCategory
import moe.memesta.vibeon.torrent.NyaaOrder
import moe.memesta.vibeon.torrent.NyaaSearchResult
import moe.memesta.vibeon.torrent.NyaaSort
import moe.memesta.vibeon.torrent.TorrentDownload
import moe.memesta.vibeon.torrent.TorrentState
import moe.memesta.vibeon.torrent.TorrentUiState
import moe.memesta.vibeon.torrent.TorrentViewModel
import moe.memesta.vibeon.ui.theme.Dimens

@Composable
fun TorrentsScreen(
    onBackPressed: () -> Unit,
    viewModel: TorrentViewModel = viewModel()
) {
    val searchState by viewModel.searchState.collectAsState()
    val query by viewModel.query.collectAsState()
    val category by viewModel.category.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val order by viewModel.order.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val lastDownloadError by viewModel.lastDownloadError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<TorrentDownload?>(null) }
    var pendingMagnet by remember { mutableStateOf<String?>(null) }
    var folderPickerError by remember { mutableStateOf<String?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val magnet = pendingMagnet
        pendingMagnet = null

        if (uri == null || magnet == null) {
            if (magnet != null) {
                folderPickerError = "No folder selected."
            }
            return@rememberLauncherForActivityResult
        }

        val saved = viewModel.setSavePathFromTreeUri(uri)
        if (!saved) {
            folderPickerError = "Selected folder is not supported on this device. Please choose a folder from internal shared storage."
            return@rememberLauncherForActivityResult
        }

        viewModel.addMagnet(magnet, viewModel.getEffectiveSavePath())
        selectedTab = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBackPressed() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
                Text(
                    text = "Torrents",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Downloads") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Search Nyaa") }
                )
            }

            when (selectedTab) {
                0 -> TorrentDownloadsTab(
                    downloads = downloads,
                    onPause = viewModel::pauseDownload,
                    onResume = viewModel::resumeDownload,
                    onRemove = { deleteTarget = it }
                )
                else -> TorrentSearchTab(
                    query = query,
                    category = category,
                    sort = sort,
                    order = order,
                    state = searchState,
                    onQueryChange = viewModel::setQuery,
                    onCategoryChange = viewModel::setCategory,
                    onSortChange = viewModel::setSort,
                    onOrderChange = viewModel::setOrder,
                    onSearch = viewModel::search,
                    onDownload = { magnet ->
                        if (viewModel.hasConfiguredSavePath()) {
                            viewModel.addMagnet(magnet, viewModel.getEffectiveSavePath())
                            selectedTab = 0
                        } else {
                            pendingMagnet = magnet
                            folderPickerLauncher.launch(null)
                        }
                    }
                )
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove torrent?") },
            text = { Text("Choose whether to remove only from list, or also delete files from disk.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = deleteTarget ?: return@TextButton
                    viewModel.removeDownload(target.id, deleteFiles = true)
                    deleteTarget = null
                }) {
                    Text("Delete files")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val target = deleteTarget ?: return@TextButton
                        viewModel.removeDownload(target.id, deleteFiles = false)
                        deleteTarget = null
                    }) {
                        Text("List only")
                    }
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (folderPickerError != null) {
        AlertDialog(
            onDismissRequest = { folderPickerError = null },
            title = { Text("Folder Selection Failed") },
            text = { Text(folderPickerError ?: "Unable to use selected folder.") },
            confirmButton = {
                TextButton(onClick = {
                    folderPickerError = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = null
        )
    }

    if (lastDownloadError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDownloadError() },
            title = { Text("Torrent Error") },
            text = { Text(lastDownloadError ?: "Unexpected torrent error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDownloadError() }) {
                    Text("OK")
                }
            },
            dismissButton = null
        )
    }
}

@Composable
private fun TorrentSearchTab(
    query: String,
    category: NyaaCategory,
    sort: NyaaSort,
    order: NyaaOrder,
    state: TorrentUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (NyaaCategory) -> Unit,
    onSortChange: (NyaaSort) -> Unit,
    onOrderChange: (NyaaOrder) -> Unit,
    onSearch: () -> Unit,
    onDownload: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text("Search query") },
            placeholder = { Text("Search Nyaa audio torrents") },
            trailingIcon = {
                IconButton(onClick = onSearch, enabled = query.isNotBlank()) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = category == NyaaCategory.AUDIO,
                onClick = { onCategoryChange(NyaaCategory.AUDIO) },
                label = { Text("Audio") }
            )
            FilterChip(
                selected = category == NyaaCategory.ALL,
                onClick = { onCategoryChange(NyaaCategory.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = order == NyaaOrder.DESC,
                onClick = { onOrderChange(NyaaOrder.DESC) },
                label = { Text("Desc") }
            )
            FilterChip(
                selected = order == NyaaOrder.ASC,
                onClick = { onOrderChange(NyaaOrder.ASC) },
                label = { Text("Asc") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NyaaSort.entries.forEach { option ->
                FilterChip(
                    selected = sort == option,
                    onClick = { onSortChange(option) },
                    label = { Text(option.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSearch,
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search")
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            TorrentUiState.Idle -> EmptyHint("Search for torrents to start downloading")
            TorrentUiState.Searching -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is TorrentUiState.Error -> EmptyHint(state.message)
            is TorrentUiState.Results -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.magnetLink }) { result ->
                    NyaaResultCard(
                        result = result,
                        onDownload = { onDownload(result.magnetLink) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TorrentDownloadsTab(
    downloads: List<TorrentDownload>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (TorrentDownload) -> Unit
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            contentAlignment = Alignment.Center
        ) {
            EmptyHint("No active downloads yet")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(downloads, key = { it.id }) { item ->
            TorrentDownloadCard(
                item = item,
                onPause = { onPause(item.id) },
                onResume = { onResume(item.id) },
                onRemove = { onRemove(item) }
            )
        }
    }
}

@Composable
private fun TorrentDownloadCard(
    item: TorrentDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { item.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${(item.progress * 100f).toInt()}% • ${formatBytes(item.downloadedSize)} / ${formatBytes(item.totalSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${item.state.name.replace('_', ' ')} • ↓ ${formatSpeed(item.downloadSpeed)} • peers ${item.peers}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val canPause = item.state == TorrentState.DOWNLOADING || item.state == TorrentState.DOWNLOADING_METADATA
                val canResume = item.state == TorrentState.PAUSED

                OutlinedButton(
                    onClick = onPause,
                    enabled = canPause
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause")
                }

                OutlinedButton(
                    onClick = onResume,
                    enabled = canResume
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resume")
                }

                OutlinedButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun NyaaResultCard(result: NyaaSearchResult, onDownload: () -> Unit) {
    ElevatedCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${result.category} • ${result.size} • S:${result.seeds} • L:${result.leechers}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = result.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDownload) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val rounded = if (value >= 100) "%.0f" else if (value >= 10) "%.1f" else "%.2f"
    return rounded.format(value) + " " + units[unitIndex]
}

private fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/s"
