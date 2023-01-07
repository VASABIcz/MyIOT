package cz.vasabi.myiot.backend.connections

import android.net.nsd.NsdServiceInfo
import cz.vasabi.myiot.backend.api.Data
import cz.vasabi.myiot.backend.database.DeviceEntity
import cz.vasabi.myiot.backend.database.HttpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.database.HttpDeviceConnectionEntity

enum class ConnectionType {
    Http,
    Tcp,
    Ws,
    Btl
}

enum class ConnectionState {
    Connected,
    Disconnected,
    Loading
}

interface DeviceInfo : Device {
    val connectionType: ConnectionType
    val parent: DeviceConnection
    val transportLayerInfo: String
}

sealed interface DeviceCapability : BaseDeviceCapability {
    fun requestValue()
    fun setValue(value: Data)
    var onReceived: suspend (Data) -> Unit
    fun close()
}

interface Device {
    val name: String
    val description: String?
    val identifier: String

    fun toEntity(): DeviceEntity {
        return DeviceEntity(identifier, name, description)
    }
}

interface BaseDeviceCapability {
    val route: String
    val name: String
    val description: String
    val type: String

    fun toEntity(identifier: String): BaseDeviceCapability {
        return HttpDeviceCapabilityEntity(identifier, name, route, description, type)
    }
}

interface BaseCapabilityReading {
    val id: Int

    val identifier: String
    val capabilityName: String
    val connectionType: String

    val timestamp: Long
    val type: String
    val value: String
}

interface IpConnectionInfo {
    val host: String
    val port: Int
    val description: String?
    val identifier: String
    val name: String

    fun toEntity(): HttpDeviceConnectionEntity {
        return HttpDeviceConnectionEntity(identifier, host, port, description, name)
    }
}

data class DeviceInfoImpl(
    override val name: String,
    override val description: String?,
    override val identifier: String,
    override val connectionType: ConnectionType,
    override val parent: DeviceConnection,
    override val transportLayerInfo: String
) : DeviceInfo

class NsdIpConnectionInfo(info: NsdServiceInfo) : IpConnectionInfo {
    override val host: String = info.host.hostAddress!!
    override val port: Int = info.port
    override val description: String? = info.attributes["description"]?.decodeToString()
    override val identifier: String = info.attributes["identifier"]?.decodeToString()!!
    override val name: String = info.serviceName
}

data class JsonDeviceCapability(
    override val route: String,
    override val name: String,
    override val description: String,
    override val type: String
) : BaseDeviceCapability