package moe.memesta.vibeon.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.memesta.vibeon.VibeonApp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.data.SyncStatus
import moe.memesta.vibeon.data.local.FavoriteDevice
import moe.memesta.vibeon.data.local.FavoritesManager
import moe.memesta.vibeon.data.local.LibraryViewStyle
import moe.memesta.vibeon.data.local.ScrubberMode
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable

@Composable
fun SettingsScreen(
    connectionViewModel: ConnectionViewModel,
    libraryViewModel: LibraryViewModel?,
    favoritesManager: FavoritesManager,
    playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository,
    onReplayOnboarding: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val appContext = VibeonApp.instance.applicationContext
    val torrentStoragePreferences = remember { VibeonApp.instance.container.torrentStoragePreferences }
    val defaultTorrentPath = remember {
        val musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: appContext.filesDir
        java.io.File(musicDir, "Vibe-On").absolutePath
    }

    var torrentSavePath by remember { mutableStateOf(torrentStoragePreferences.savePath ?: defaultTorrentPath) }
    var torrentPathError by remember { mutableStateOf<String?>(null) }

    val torrentPathPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            torrentPathError = "No folder selected."
            return@rememberLauncherForActivityResult
        }

        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }

        val resolvedPath = treeUriToPath(uri)
        if (resolvedPath.isNullOrBlank()) {
            torrentPathError = "Selected folder is not supported on this device."
            return@rememberLauncherForActivityResult
        }

        val dir = java.io.File(resolvedPath)
        if (!dir.exists() && !dir.mkdirs()) {
            torrentPathError = "Failed to access selected folder."
            return@rememberLauncherForActivityResult
        }

        torrentStoragePreferences.savePath = dir.absolutePath
        torrentSavePath = dir.absolutePath
    }

    var favorites by remember { mutableStateOf(favoritesManager.getFavorites()) }
    var showRenameDialog by remember { mutableStateOf<FavoriteDevice?>(null) }
    var expandedDevice by remember { mutableStateOf<String?>(null) }
    
    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    val isConnected by connectionViewModel.wsIsConnected.collectAsState()
    val syncStatusState = libraryViewModel?.syncStatus?.collectAsState(initial = SyncStatus())
    val syncStatus = syncStatusState?.value ?: SyncStatus()
    val displayLanguage by playerSettingsRepository.displayLanguage.collectAsState()
    val albumViewStyle by playerSettingsRepository.albumViewStyle.collectAsState()
    val artistViewStyle by playerSettingsRepository.artistViewStyle.collectAsState()
    val scrubberMode by playerSettingsRepository.scrubberMode.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Personalise your Vibe-On experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        // Section: Current Connection (Only show when connected)
        if (isConnected && connectedDevice != null) {
            item {
                Text(
                    text = "Current Connection",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                val device = connectedDevice!!
                val isFavorite = favorites.any { it.name == device.name && it.host == device.host }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.nickname ?: device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${device.host}:${device.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Heart button to add to favorites
                        if (!isFavorite) {
                            IconButton(
                                onClick = {
                                    favoritesManager.addFavorite(
                                        FavoriteDevice(
                                            name = device.name,
                                            host = device.host,
                                            port = device.port,
                                            nickname = device.nickname
                                        )
                                    )
                                    favorites = favoritesManager.getFavorites()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Add to Favorites",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Already in Favorites",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Spacer
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        // Section: Favorites
        item {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (favorites.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No favorite devices yet.\nLong-press a device in discovery to add to favorites.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        items(favorites) { favorite ->
            FavoriteDeviceCard(
                favorite = favorite,
                isExpanded = expandedDevice == favorite.name,
                onToggleExpand = {
                    expandedDevice = if (expandedDevice == favorite.name) null else favorite.name
                },
                onRename = { showRenameDialog = favorite },
                onRemove = {
                    favoritesManager.removeFavorite(favorite.name)
                    favorites = favoritesManager.getFavorites()
                }
            )
        }
        
        // Spacer
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Library Appearance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Albums View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Legacy keeps the simpler layout. Modern stays within Material 3 Expressive with richer hierarchy and utility chips.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ViewStyleSelector(
                        selectedStyle = albumViewStyle,
                        onStyleSelected = playerSettingsRepository::setAlbumViewStyle
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Artists View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Use a separate style so users can keep artists simple even if albums use the modern layout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ViewStyleSelector(
                        selectedStyle = artistViewStyle,
                        onStyleSelected = playerSettingsRepository::setArtistViewStyle
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Tutorial",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { onReplayOnboarding() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replay onboarding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Show Welcome and tutorial again",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Replay onboarding",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Torrent Downloads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Default Download Folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Torrent files are written to this folder by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = torrentSavePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { torrentPathPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Choose Folder")
                        }
                        OutlinedButton(
                            onClick = {
                                torrentStoragePreferences.savePath = defaultTorrentPath
                                torrentSavePath = defaultTorrentPath
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use App Default")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Section: Player Settings
        item {
            Text(
                text = "Player Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Display Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose which metadata to show (Original, Romaji, English)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Language Selection Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            moe.memesta.vibeon.data.local.DisplayLanguage.ORIGINAL,
                            moe.memesta.vibeon.data.local.DisplayLanguage.ROMAJI
                        ).forEach { language ->
                            val isSelected = displayLanguage == language
                            val title = if (language == moe.memesta.vibeon.data.local.DisplayLanguage.ORIGINAL) {
                                "Japanese"
                            } else {
                                language.name.lowercase().replaceFirstChar { it.uppercase() }
                            }
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { playerSettingsRepository.setDisplayLanguage(language) },
                                label = { Text(title) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ListItem(
                        headlineContent = { Text("Progress bar style") },
                        supportingContent = {
                            Text(if (scrubberMode == ScrubberMode.WAVEFORM) "Waveform" else "Classic bar")
                        },
                        trailingContent = {
                            Switch(
                                checked = scrubberMode == ScrubberMode.WAVEFORM,
                                onCheckedChange = {
                                    playerSettingsRepository.setScrubberMode(
                                        if (it) ScrubberMode.WAVEFORM else ScrubberMode.CLASSIC
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }

        // Spacer
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Section: Quick Actions
        item {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(enabled = !syncStatus.isSyncing) {
                        // Trigger manual library refresh
                        libraryViewModel?.viewModelScope?.launch {
                            libraryViewModel.fetchStats() // refresh stats too
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (syncStatus.isSyncing) 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (syncStatus.isSyncing) Icons.Default.Sync else Icons.Default.Refresh,
                            contentDescription = "Refresh Library",
                            tint = if (syncStatus.isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (syncStatus.isSyncing) "Syncing Library..." else "Refresh Library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (syncStatus.isSyncing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (syncStatus.isSyncing) syncStatus.statusText else "Sync tracks from server",
                                style = MaterialTheme.typography.bodySmall,
                                color = (if (syncStatus.isSyncing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    if (syncStatus.isSyncing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { syncStatus.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
    
    // Rename Dialog
    showRenameDialog?.let { device ->
        RenameDeviceDialog(
            currentName = device.nickname ?: device.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newNickname ->
                favoritesManager.updateNickname(device.name, newNickname)
                favorites = favoritesManager.getFavorites()
                showRenameDialog = null
            }
        )
    }

    if (torrentPathError != null) {
        AlertDialog(
            onDismissRequest = { torrentPathError = null },
            title = { Text("Folder Selection Failed") },
            text = { Text(torrentPathError ?: "Unable to use selected folder.") },
            confirmButton = {
                TextButton(onClick = { torrentPathError = null }) {
                    Text("OK")
                }
            }
        )
    }
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

@Composable
fun FavoriteDeviceCard(
    favorite: FavoriteDevice,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = favorite.nickname ?: favorite.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (favorite.nickname != null) {
                        Text(
                            text = favorite.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // Device Details
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailRow("Host", favorite.host)
                        DetailRow("Port", favorite.port.toString())
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRename,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename")
                    }
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewStyleSelector(
    selectedStyle: LibraryViewStyle,
    onStyleSelected: (LibraryViewStyle) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(LibraryViewStyle.LEGACY, LibraryViewStyle.MODERN).forEach { style ->
            val isSelected = selectedStyle == style
            val title = if (style == LibraryViewStyle.LEGACY) "Legacy" else "Modern"

            FilterChip(
                selected = isSelected,
                onClick = { onStyleSelected(style) },
                label = { Text(title) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RenameDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Device") },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nickname") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

