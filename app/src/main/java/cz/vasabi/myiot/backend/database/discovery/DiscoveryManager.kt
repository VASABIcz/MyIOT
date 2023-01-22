package cz.vasabi.myiot.backend.database.discovery

import cz.vasabi.myiot.backend.connections.DeviceConnection

class DiscoveryManager(private val onDeviceResolved: (DeviceConnection) -> Unit) {
     private val discoveryServices: MutableList<DiscoveryService> = mutableListOf()

     fun addService(service: DiscoveryService) {
         service.onDeviceResolved = onDeviceResolved
         discoveryServices.add(service)
     }

     fun close() {
         discoveryServices.forEach {
             it.close()
         }
    }

    fun start() {
        discoveryServices.forEach {
            it.start()
        }
    }
}

