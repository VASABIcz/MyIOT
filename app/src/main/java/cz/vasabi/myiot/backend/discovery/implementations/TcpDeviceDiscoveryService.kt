package cz.vasabi.myiot.backend.discovery.implementations

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vasabi.myiot.backend.connections.DeviceConnection
import cz.vasabi.myiot.backend.connections.NsdIpConnectionInfo
import cz.vasabi.myiot.backend.connections.TcpDeviceConnection
import cz.vasabi.myiot.backend.discovery.DeviceResolveManager
import cz.vasabi.myiot.backend.discovery.DiscoveryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TcpDeviceDiscoveryService(
    nsdManager: NsdManager,
    private val discoveryStopped: (String) -> Unit,
    override var onDeviceResolved: (DeviceConnection) -> Unit,
    private val objectMapper: ObjectMapper,
    private val serviceResolveManager: DeviceResolveManager
) : DeviceSearchIp(nsdManager), DiscoveryService {
    override val isDone: Boolean
        get() = false

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onServiceFound(service: NsdServiceInfo) {
        super.onServiceFound(service)

        scope.launch {
            val res = serviceResolveManager.resolveAsync(service).await()
            onDeviceResolved(TcpDeviceConnection(NsdIpConnectionInfo(res), objectMapper))
        }
    }

    override fun onDiscoveryStopped(serviceType: String) {
        discoveryStopped(serviceType)
        super.onDiscoveryStopped(serviceType)
    }

    override fun start() {
        nsdManager.discoverServices("_iotTcp._tcp", NsdManager.PROTOCOL_DNS_SD, this)
    }

    override fun close() {
        scope.cancel()
        try {
            nsdManager.stopServiceDiscovery(this)
        } catch (_: Throwable) {
        }
    }
}