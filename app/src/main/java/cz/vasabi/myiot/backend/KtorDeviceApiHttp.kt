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
import cz.vasabi.myiot.Data
import cz.vasabi.myiot.GenericResponse
import cz.vasabi.myiot.SingleState
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
}

class DeviceState(private val device: DeviceInfo): Device by device {
    val connections: SnapshotStateMap<ConnectionType, DeviceConnectionState> = mutableStateMapOf()
}

class DeviceImpl(info: DeviceInfo) : Device {
    override val name: String = info.name
    override val description: String? = info.description
    override val identifier: String = info.identifier
}

interface DeviceCapability {
    val name: String
    val route: String
    val description: String
    val type: String

    suspend fun requestValue()
    suspend fun setValue(value: Data)
    suspend fun onReceived(value: Data)
}

interface DeviceConnection {
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

class DeviceConnectionState(val deviceConnection: DeviceConnection): DeviceConnection by deviceConnection {
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
            capabilities.forEach {
                deviceCapabilities.add(DeviceCapabilityState(it))
            }
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
        deviceConnection.disconnect()
    }
}

data class JsonDeviceCapability(val route: String, val name: String, val description: String, val type: String)

data class DeviceInfo(
    override val name: String,
    override val description: String?,
    override val identifier: String,
    val connectionType: ConnectionType,
    val parent: DeviceConnection,
    val transportLayerInfo: String
): Device

class HttpDeviceCapability(
    override val name: String,
    override val route: String,
    override val description: String,
    override val type: String,
    val info: NsdServiceInfo,
    val client: HttpClient
) : DeviceCapability {
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

class HttpDeviceConnection(private val info: NsdServiceInfo, private val client: HttpClient): DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Http
    private val scope = CoroutineScope(Dispatchers.IO)
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}

    override suspend fun connect() {
        scope.launch {
            while (true) {
                try {
                    val x: List<JsonDeviceCapability> = client.get("http://${info.host.hostAddress}:${info.port}/api/capabilities").body()
                    Log.d(TAG, x.toString())
                    onConnectionChanged(ConnectionState.Connected)
                } catch (e: Exception) {
                    Log.d(TAG, e.message.toString())
                    e.printStackTrace()
                    onConnectionChanged(ConnectionState.Disconnected)
                }
                delay(2000)
            }
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
    }

    override suspend fun getCapabilities(): List<DeviceCapability> {
        // FIXME it should be map
        val x: List<JsonDeviceCapability> = try {
            client.get("http://${info.host.hostAddress}:${info.port}/api/capabilities").body()
        }
        catch (e: Exception) {
            return emptyList()
        }
        val res = x.map {
            return@map HttpDeviceCapability(it.name, it.route, it.description, it.type, info, client)
        }
        return res
    }

    override suspend fun getDeviceInfo(): DeviceInfo? {
        val id = info.attributes["identifier"]?.decodeToString() ?: return null

        val description = info.attributes["identifier"]?.decodeToString()

        return DeviceInfo(info.serviceName, description, id, connectionType, this, "${info.host.hostAddress}:${info.port}}")
    }
}