package moe.memesta.vibeon.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiscoveryRepository(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_vibe-on._tcp."
    private var isDiscoveryStarted = false
    
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("Discovery", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("Discovery", "Service found: ${service.serviceName}")
            if (service.serviceType == serviceType) {
                nsdManager.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d("Discovery", "Service lost: ${service.serviceName}")
            _discoveredDevices.value = _discoveredDevices.value.filter { it.name != service.serviceName }
        }

        override fun onDiscoveryStopped(regType: String) {
            Log.d("Discovery", "Discovery stopped: $regType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Discovery", "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Discovery", "Stop discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("Discovery", "Resolve failed: Error code:$errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("Discovery", "Resolve Succeeded: $serviceInfo")
            
            val serviceName = serviceInfo.serviceName
            val host = serviceInfo.host
            val allAddresses = host?.hostAddress
            
            Log.d("Discovery", "Service: $serviceName, hostAddresses: $allAddresses")
            
            // ONLY use IPv4 addresses - filter out IPv6 and .local hostnames
            val resolvedHost = when {
                // Check if we have a valid IPv4 address (no colons, not link-local)
                allAddresses != null && 
                !allAddresses.contains(':') && 
                !allAddresses.startsWith("fe80") &&
                !allAddresses.endsWith(".local") -> allAddresses
                // No valid address - skip this service
                else -> null
            }
            
            Log.d("Discovery", "Service: $serviceName, IP: $allAddresses, Using: $resolvedHost")
            
            // Add the device ONLY if we have a valid IPv4 address
            if (resolvedHost != null) {
                val device = DiscoveredDevice(
                    name = serviceName,
                    host = resolvedHost,
                    port = serviceInfo.port
                )
                _discoveredDevices.value = (_discoveredDevices.value + device).distinctBy { it.name }
                Log.i("Discovery", "✅ Added device: ${device.name} at ${device.host}:${device.port}")
            } else {
                Log.w("Discovery", "❌ Could not resolve IPv4 address for: $serviceName (got: $allAddresses)")
            }
        }
    }

    fun startDiscovery() {
        if (!isDiscoveryStarted) {
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                isDiscoveryStarted = true
            } catch (e: Exception) {
                Log.e("Discovery", "Failed to start discovery: ${e.message}")
            }
        }
    }

    fun stopDiscovery() {
        if (isDiscoveryStarted) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
                isDiscoveryStarted = false
            } catch (e: Exception) {
                Log.e("Discovery", "Failed to stop discovery: ${e.message}")
                isDiscoveryStarted = false
            }
        }
    }
}

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int
)
