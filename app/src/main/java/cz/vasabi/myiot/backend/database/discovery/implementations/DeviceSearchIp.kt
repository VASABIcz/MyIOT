package cz.vasabi.myiot.backend.database.discovery.implementations

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import cz.vasabi.myiot.backend.logging.logger

open class DeviceSearchIp(val nsdManager: NsdManager) : NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(regType: String) {
        logger.debug("Service discovery started", this)
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        logger.debug("Service found $service", this)
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        logger.debug("service lost: $service", this)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        logger.debug("Discovery stopped: $serviceType", this)
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        logger.debug("Discovery failed: Error code:$errorCode $serviceType", this)
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            logger.debug("nsdManager.onStartDiscoveryFailed ${e.message}", this)
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        logger.debug("Discovery failed: Error code:$errorCode $serviceType", this)
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            logger.debug("nsdManager.stopServiceDiscovery ${e.message}", this)
        }
    }
}