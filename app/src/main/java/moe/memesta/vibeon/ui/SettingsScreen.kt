package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.data.local.FavoriteDevice
import moe.memesta.vibeon.data.local.FavoritesManager

@Composable
fun SettingsScreen(
    connectionViewModel: ConnectionViewModel,
    favoritesManager: FavoritesManager,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val discoveredDevices by connectionViewModel.discoveredDevices.collectAsState()
    var favorites by remember { mutableStateOf(favoritesManager.getFavorites()) }
    var showRenameDialog by remember { mutableStateOf<FavoriteDevice?>(null) }
    var expandedDevice by remember { mutableStateOf<String?>(null) }
    
    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    val isConnected by connectionViewModel.wsIsConnected.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        )
    ) {
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
                    .clickable {
                        // Trigger manual library refresh
                        // TODO: Call library refresh function
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Library",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Refresh Library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Sync tracks from server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
            .clickable { onToggleExpand() },
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
                Divider()
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

