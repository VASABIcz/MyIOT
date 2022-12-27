package cz.vasabi.myiot.backend.discovery

import cz.vasabi.myiot.backend.connections.DeviceConnection

interface DiscoveryService {
    var onDeviceResolved: (DeviceConnection) -> Unit
    val isDone: Boolean

    fun close()
    fun start()
}