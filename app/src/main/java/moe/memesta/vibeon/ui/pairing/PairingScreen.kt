package moe.memesta.vibeon.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.ui.theme.VibeonTheme


import moe.memesta.vibeon.data.DiscoveredDevice
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    devices: List<DiscoveredDevice>,
    onConnect: (String, Int) -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onNavigateToScan: () -> Unit
) {
    var ipPart1 by remember { mutableStateOf("") }
    var ipPart2 by remember { mutableStateOf("") }
    var ipPart3 by remember { mutableStateOf("") }
    var ipPart4 by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // H1 Title
        Text(
            text = "VIBE-ON!",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Available Clients Section
        Text(
            text = "Available Clients",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Device List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp) // Increased height a bit
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            if (devices.isEmpty()) {
                item {
                   Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                       Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                           Spacer(modifier = Modifier.height(8.dp))
                           Text("Scanning for devices...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                       }
                   }
                }
            } else {
                items(devices.size) { index ->
                    val device = devices[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDeviceSelected(device) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Computer, // Use generic icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < devices.size - 1) {
                         Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Scan QR Button
        Button(
            onClick = onNavigateToScan,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Scan QR Code")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Or connect manually", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Manual IP Entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4 Octets
            IpBox(value = ipPart1, onValueChange = { if(it.length <= 3) ipPart1 = it }, modifier = Modifier.weight(1f))
            Text(".", style = MaterialTheme.typography.headlineSmall)
            IpBox(value = ipPart2, onValueChange = { if(it.length <= 3) ipPart2 = it }, modifier = Modifier.weight(1f))
            Text(".", style = MaterialTheme.typography.headlineSmall)
            IpBox(value = ipPart3, onValueChange = { if(it.length <= 3) ipPart3 = it }, modifier = Modifier.weight(1f))
            Text(".", style = MaterialTheme.typography.headlineSmall)
            IpBox(value = ipPart4, onValueChange = { if(it.length <= 3) ipPart4 = it }, modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Port
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                val ip = "$ipPart1.$ipPart2.$ipPart3.$ipPart4"
                onConnect(ip, port.toIntOrNull() ?: 5000)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Connect Manually")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Preview
@Composable
fun PairingPreview() {
    VibeonTheme {
        PairingScreen(
            devices = emptyList(),
            onConnect = { _,_ -> }, 
            onDeviceSelected = {},
            onNavigateToScan = {}
        )
    }
}
