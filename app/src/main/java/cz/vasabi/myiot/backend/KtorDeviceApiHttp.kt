package cz.vasabi.myiot.backend

import android.content.ContentValues.TAG
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import cz.vasabi.myiot.SingleState
import cz.vasabi.myiot.backend.api.Data
import cz.vasabi.myiot.backend.api.GenericResponse
import cz.vasabi.myiot.backend.database.DeviceEntity
import cz.vasabi.myiot.backend.database.HttpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.database.HttpDeviceConnectionEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

interface Device {
    val name: String
    val description: String?
    val identifier: String

    fun toEntity(): DeviceEntity {
        return DeviceEntity(identifier, name, description)
    }
}

class DeviceState(private val device: Device): Device by device {
    val connections: SnapshotStateMap<ConnectionType, DeviceConnectionState> = mutableStateMapOf()
}

class DeviceImpl(info: DeviceInfo) : Device {
    override val name: String = info.name
    override val description: String? = info.description
    override val identifier: String = info.identifier
}

interface DeviceCapability: BaseDeviceCapability {
    override val name: String
    override val route: String
    override val description: String
    override val type: String

    suspend fun requestValue()
    suspend fun setValue(value: Data)
    suspend fun onReceived(value: Data)
}

sealed interface DeviceConnection {
    val connectionType: ConnectionType
    var onConnectionChanged: suspend (ConnectionState) -> Unit

    suspend fun connect()
    suspend fun disconnect()
    suspend fun getCapabilities(): List<DeviceCapability>
    suspend fun getDeviceInfo(): DeviceInfo?
}


class DeviceCapabilityState(deviceCapability: DeviceCapability): DeviceCapability by deviceCapability {
    val responses: Channel<Data> = Channel()


    override suspend fun onReceived(value: Data) {
        responses.send(value)
    }
}

class DeviceConnectionState(private val deviceConnection: DeviceConnection, private val deviceManager: DeviceManager): DeviceConnection by deviceConnection {
    val deviceCapabilities: SnapshotStateList<DeviceCapabilityState> = mutableStateListOf()
    val connected: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Loading)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            deviceConnection.onConnectionChanged = {
                connected.value = it
            }
            // deviceConnection.connect()
            val capabilities = getCapabilities()
            deviceCapabilities.clear()
            deviceCapabilities.addAll(capabilities.map { DeviceCapabilityState(it) })
            deviceManager.registerCapabilities(capabilities, deviceConnection)
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
        deviceConnection.disconnect()
    }
}


interface BaseDeviceCapability {
    val route: String
    val name: String
    val description: String
    val type: String

    fun toEntity(identifier: String): HttpDeviceCapabilityEntity {
        return HttpDeviceCapabilityEntity(identifier, name, route, description, type)
    }
}

data class JsonDeviceCapability(
    override val route: String,
    override val name: String,
    override val description: String,
    override val type: String
    ): BaseDeviceCapability

data class DeviceInfo(
    override val name: String,
    override val description: String?,
    override val identifier: String,
    val connectionType: ConnectionType,
    val parent: DeviceConnection,
    val transportLayerInfo: String
): Device

class HttpDeviceCapability(
    val device: BaseDeviceCapability,
    private val info: HttpConnectionInfo,
    private val client: HttpClient
) : DeviceCapability, BaseDeviceCapability by device {
    override suspend fun requestValue() {
        val res = try { client.get("http://${info.host}:${info.port}${route}") }
        catch (_: Throwable) {return}
        SingleState.events.add(res.toString())

        val resValue: GenericResponse = res.body()
        SingleState.events.add("received $resValue")
        onReceived(resValue.toData())
    }

    override suspend fun setValue(value: Data) {
        val res = try {
            client.post("http://${info.host}:${info.port}${route}") {
                setBody(value.jsonBody)
            }
        }
        catch (_: Exception) {
            return
        }
        SingleState.events.add(res.toString())

        val resValue: GenericResponse = res.body()
        SingleState.events.add("received $resValue")
        onReceived(resValue.toData())
    }

    override suspend fun onReceived(value: Data) {}
}


interface HttpConnectionInfo {
    val host: String
    val port: Int
    val description: String?
    val identifier: String
    val name: String

    fun toEntity(): HttpDeviceConnectionEntity {
        return HttpDeviceConnectionEntity(identifier, host, port, description, name)
    }
}

class NsdHttpConnectionInfo(info: NsdServiceInfo): HttpConnectionInfo {
    override val host: String = info.host.hostAddress!!
    override val port: Int = info.port
    override val description: String? = info.attributes["description"]?.decodeToString()
    override val identifier: String = info.attributes["identifier"]?.decodeToString()!!
    override val name: String = info.serviceName

}

/*
data class HttpConnectionInfo(val host: String, val port: Int, val description: String?, val identifier: String, val name: String) {
    companion object {
        fun fromNsdInfo(info: NsdServiceInfo): HttpConnectionInfo {
            return HttpConnectionInfo(
                info.host.hostAddress!!,
                info.port,
                info.attributes["description"]?.decodeToString(),
                info.attributes["identifier"]?.decodeToString()!!,
                info.serviceName
            )
        }
    }
}

 */

class HttpDeviceConnection(val info: HttpConnectionInfo, private val client: HttpClient): DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Http
    private val scope = CoroutineScope(Dispatchers.IO)
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}
    private var isConnected = false

    override suspend fun connect() {
        if (isConnected) return
        isConnected = true
        scope.launch {
            while (true) {
                try {
                    val x: List<JsonDeviceCapability> = client.get("http://${info.host}:${info.port}/api/capabilities").body()
                    Log.d(TAG, x.toString())
                    onConnectionChanged(ConnectionState.Connected)
                } catch (e: Exception) {
                    Log.d(TAG, e.message.toString())
                    e.printStackTrace()
                    onConnectionChanged(ConnectionState.Disconnected)
                }
                delay(5000)
            }
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
    }

    override suspend fun getCapabilities(): List<DeviceCapability> {
        // FIXME it should be map
        val deviceCapabilities: List<JsonDeviceCapability> = try {
            client.get("http://${info.host}:${info.port}/api/capabilities").body()
        }
        catch (e: Exception) {
            return emptyList()
        }
        val res = deviceCapabilities.map {
            return@map HttpDeviceCapability(it, info, client)
        }
        return res
    }

    override suspend fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(info.name, info.description, info.identifier, connectionType, this, "${info.host}:${info.port}")
    }
}