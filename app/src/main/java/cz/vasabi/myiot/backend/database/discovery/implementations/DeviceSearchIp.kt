package cz.vasabi.myiot.backend.database.discovery.implementations

import android.content.ContentValues
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import cz.vasabi.myiot.SingleState

open class DeviceSearchIp(val nsdManager: NsdManager) : NsdManager.DiscoveryListener {
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