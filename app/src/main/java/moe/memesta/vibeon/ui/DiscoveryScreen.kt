package moe.memesta.vibeon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.DiscoveredDevice

@Composable
fun DiscoveryScreen(
    viewModel: ConnectionViewModel,
    onDeviceSelected: (DiscoveredDevice) -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val wsIsConnected by viewModel.wsIsConnected.collectAsState()
    var showManualInput by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("5000") }

    DisposableEffect(Unit) {
        viewModel.startScanning()
        onDispose {
            viewModel.stopScanning()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Discovering Vibe-on Servers...",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Show connection status
        if (wsIsConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✓ Connected", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Scanning local network...")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info box
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "No servers found?",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Make sure the desktop app is running and on the same WiFi network.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showManualInput = !showManualInput },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showManualInput) "Hide Manual Entry" else "Enter Server IP Manually")
            }
            
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onDeviceSelected(device) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = device.name, style = MaterialTheme.typography.titleLarge)
                            Text(text = "${device.host}:${device.port}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showManualInput = !showManualInput },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Or Enter Server IP Manually")
            }
        }
        
        // Manual IP Input
        if (showManualInput) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Manual Server Entry",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "💡 Tip: Check your PC's VIBE-ON app for the server IP address. It should be displayed in the mobile pairing popup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "🔍 Example: If your PC shows \"192.168.1.100:5000\", enter \"192.168.1.100\" below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("Server IP (e.g., 192.168.1.100)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextField(
                        value = manualPort,
                        onValueChange = { manualPort = it },
                        label = { Text("Port (default: 5000)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (manualIp.isNotBlank()) {
                                val port = manualPort.toIntOrNull() ?: 5000
                                val device = DiscoveredDevice(
                                    name = "Manual: $manualIp",
                                    host = manualIp,
                                    port = port
                                )
                                onDeviceSelected(device)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualIp.isNotBlank()
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
