package cz.vasabi.myiot.backend.discovery

import android.content.ContentValues
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import cz.vasabi.myiot.SingleState
import cz.vasabi.myiot.backend.DeviceConnection
import cz.vasabi.myiot.backend.HttpDeviceConnection
import io.ktor.client.HttpClient

class HttpDeviceResolver(private val onDeviceResolved: (NsdServiceInfo) -> Unit): NsdManager.ResolveListener {
    override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
        SingleState.events.add("failed to resolve service $p0")
    }

    override fun onServiceResolved(p0: NsdServiceInfo?) {
        SingleState.events.add("resolved service $p0")
        if (p0 == null) return
        onDeviceResolved(p0)
    }
}



class HttpDeviceDiscoveryService(nsdManager: NsdManager, private val discoveryStopped: (String) -> Unit, override var onDeviceResolved: (DeviceConnection) -> Unit, private val client: HttpClient): DeviceSearch(nsdManager),
    DiscoveryService {
    override val isDone: Boolean
        get() = false

    override fun onServiceFound(service: NsdServiceInfo) {
        nsdManager.resolveService(service, HttpDeviceResolver {
            onDeviceResolved(HttpDeviceConnection(it, client))
        })
        super.onServiceFound(service)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        discoveryStopped(serviceType)
        super.onDiscoveryStopped(serviceType)
    }

    override fun start() {
        nsdManager.discoverServices("_iot._tcp", NsdManager.PROTOCOL_DNS_SD, this)
    }

    override fun close() {
        nsdManager.stopServiceDiscovery(this)
    }
}

open class DeviceSearch(open val nsdManager: NsdManager): NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(regType: String) {
        Log.d(ContentValues.TAG, "Service discovery started")
        SingleState.events.add("Service discovery started")
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        SingleState.events.add("Service found $service")
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        Log.e(ContentValues.TAG, "service lost: $service")
        SingleState.events.add("service lost: $service")
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Log.i(ContentValues.TAG, "Discovery stopped: $serviceType")
        SingleState.events.add("Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(ContentValues.TAG, "Discovery failed: Error code:$errorCode")
        SingleState.events.add("Discovery failed: Error code:$errorCode $serviceType")
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            SingleState.events.add("nsdManager.onStartDiscoveryFailed ${e.message}")
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(ContentValues.TAG, "Discovery failed: Error code:$errorCode")
        SingleState.events.add("Discovery failed: Error code:$errorCode $serviceType")
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            SingleState.events.add("nsdManager.stopServiceDiscovery ${e.message}")
        }
    }
}