package cz.vasabi.myiot.backend.connections

sealed interface DeviceConnection {
    val connectionType: ConnectionType
    var onConnectionChanged: suspend (ConnectionState) -> Unit

    fun connect()
    suspend fun disconnect()
    suspend fun getCapabilities(): List<DeviceCapability>?
    suspend fun getDeviceInfo(): DeviceInfo?
}