package cz.vasabi.myiot.backend.database.discovery

import cz.vasabi.myiot.backend.connections.DeviceConnection

interface DiscoveryService {
    var onDeviceResolved: (DeviceConnection) -> Unit
    val isDone: Boolean

    fun close()
    fun start()
}