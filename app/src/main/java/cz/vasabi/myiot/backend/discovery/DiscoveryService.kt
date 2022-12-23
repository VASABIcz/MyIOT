package cz.vasabi.myiot.backend.discovery

import cz.vasabi.myiot.backend.DeviceConnection

interface DiscoveryService {
    var onDeviceResolved: (DeviceConnection) -> Unit
    val isDone: Boolean

    fun close()
    fun start()
}