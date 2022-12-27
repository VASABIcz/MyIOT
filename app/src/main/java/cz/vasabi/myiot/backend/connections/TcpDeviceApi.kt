package cz.vasabi.myiot.backend.connections

import android.content.ContentValues.TAG
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.vasabi.myiot.backend.api.Data
import cz.vasabi.myiot.backend.api.GenericHttpResponse
import cz.vasabi.myiot.backend.api.GenericResponse
import cz.vasabi.myiot.backend.database.TcpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.database.TcpDeviceConnectionEntity
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.connection
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/*

protocol description


verb data?+\n

requests:
    get /switch
    post /switch {"value": true, "type": "bool"}
    capabilities
responses:
    capabilities [{...}, ...]
    value /switch {"value": true, "type": "bool"}
 */

internal data class TcpResponse(
    override val value: String,
    override val type: String,
    val route: String
) : GenericResponse

internal sealed interface TcpRequest {
    object Capabilities : TcpRequest
    class Post(val route: String, val data: String) : TcpRequest
    class Get(val route: String) : TcpRequest
}

class TcpDeviceConnection(val info: IpConnectionInfo, private val objectMapper: ObjectMapper) :
    DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Tcp
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}
    private val scope = CoroutineScope(Dispatchers.IO)

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val responseChannel = Channel<TcpResponse>(5)
    private val capabilitiesChannel = Channel<CompletableDeferred<List<DeviceCapability>?>>(5)
    private val requestChannel = Channel<TcpRequest>(5)

    private val capabilities = hashMapOf<String, MutableList<TcpDeviceCapability>>()
    private lateinit var socket: Socket
    private lateinit var conn: Connection

    override fun connect() {
        // TODO try catch
        scope.launch {
            socket = aSocket(selectorManager).tcp().connect(info.host, info.port) {
                noDelay = true
                sendBufferSize = 1
            }
            conn = socket.connection()
            scope.launch {
                socketReader()
            }
            scope.launch {
                handleRequests()
            }
            scope.launch {
                handleResponses()
            }
        }
    }

    private suspend fun handleResponses() {
        for (message in responseChannel) {
            processMessage(message)
        }
    }

    private fun processMessage(message: TcpResponse) {
        val capability = capabilities[message.route]

        if (capability == null) {
            Log.w(TAG, "undelivered tcp message $message")
            return
        }

        scope.launch {
            capability.forEach {
                it.onReceived(message.toData())
            }
        }
    }

    private suspend fun socketReader() {
        while (true) {
            val message = conn.input.readUTF8Line(1024)
            Log.d(TAG, "message $message")

            if (message == null) {
                Log.w(TAG, "tcp conn $this received null message")
                continue
            }

            if (message.startsWith("capabilities")) {
                handleCapabilitiesResult(message)
            } else if (message.startsWith("value")) {
                handleValueResult(message)
            } else {
                Log.e(TAG, "tcp conn $this received unknown message: $message")
            }
        }
    }

    private suspend fun handleCapabilitiesResult(message: String) {
        val striped = message.replace("capabilities ", "")
        val res: List<JsonDeviceCapability> = try {
            objectMapper.readValue(striped)
        } catch (_: Throwable) {
            Log.e(TAG, "$message was invalid capability result")
            // FIXME not sure about this
            capabilitiesChannel.receive().complete(null)
            return
        }

        val future = capabilitiesChannel.receive()

        if (future.isCancelled) {
            Log.e(TAG, "we meet again")
            return
        }

        future.complete(res.map {
            TcpDeviceCapability(it, this)
        })
    }

    private suspend fun handleValueResult(message: String) {
        // value /switch {"value": true, "type": "bool"}
        val striped = message.replace("value ", "")
        val routeEnd = striped.indexOf(" ")
        if (routeEnd == -1) {
            Log.e(TAG, "invalid value result $message")
            return
        }
        val route = striped.slice(0 until routeEnd)
        val payload = striped.slice(routeEnd + 1 until striped.length)
        val data: GenericHttpResponse = try {
            objectMapper.readValue(payload)
        } catch (_: Throwable) {
            Log.e(TAG, "failed to parse generic response tcp $message , parsed: $payload")
            return
        }
        responseChannel.send(TcpResponse(data.value, data.type, route))
    }

    private suspend fun handleRequests() {
        for (request in requestChannel) {
            when (request) {
                TcpRequest.Capabilities -> conn.output.writeStringUtf8("capabilities\n")
                is TcpRequest.Get -> conn.output.writeStringUtf8("get ${request.route}\n")
                is TcpRequest.Post -> conn.output.writeStringUtf8("post ${request.route} ${request.data}\n")
            }
            conn.output.flush()
            Log.e(TAG, "finished writing $request")
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            socket.close()
            selectorManager.close()
        }
    }

    override suspend fun getCapabilities(): List<DeviceCapability>? {
        Log.w(TAG, "tcp requesting capabilities")
        val future = CompletableDeferred<List<DeviceCapability>?>()
        Log.e(TAG, "HUH")
        requestChannel.send(TcpRequest.Capabilities)
        capabilitiesChannel.send(future)
        Log.e(TAG, "FUCK")
        Log.e(TAG, "GOOD")

        val res = withTimeoutOrNull(1000) {
            future.await()
        }

        Log.w(TAG, "getCapabilities tcp $res")

        return res
    }

    override suspend fun getDeviceInfo(): DeviceInfo {
        return DeviceInfoImpl(
            info.name,
            info.description,
            info.identifier,
            connectionType,
            this,
            "${info.host}:${info.port}"
        )
    }

    fun registerCapability(cap: TcpDeviceCapability) {
        println("registering $cap ${cap.route}")
        val base = capabilities[cap.route]
        if (base == null) {
            capabilities[cap.route] = mutableListOf(cap)
        } else {
            base.add(cap)
        }
    }

    fun removeRoute(capability: TcpDeviceCapability) {
        val caps = capabilities[capability.route]
        caps?.remove(capability)
    }

    fun requestValue(route: String) {
        scope.launch {
            requestChannel.send(TcpRequest.Get(route))
        }
    }

    fun setValue(route: String, value: String) {
        scope.launch {
            requestChannel.send(TcpRequest.Post(route, value))
        }
    }

    fun toEntity(): TcpDeviceConnectionEntity {
        return TcpDeviceConnectionEntity(
            info.identifier,
            info.host,
            info.port,
            info.description,
            info.name
        )
    }
}

class TcpDeviceCapability(
    val device: BaseDeviceCapability,
    private val parent: TcpDeviceConnection
) : DeviceCapability, BaseDeviceCapability by device {
    init {
        parent.registerCapability(this)

    }

    override fun toEntity(identifier: String): TcpDeviceCapabilityEntity {
        return TcpDeviceCapabilityEntity(identifier, name, route, description, type)
    }

    override fun requestValue() {
        parent.requestValue(route)
    }

    override fun setValue(value: Data) {
        parent.setValue(route, value.jsonBody)
    }

    override var onReceived: suspend (Data) -> Unit = {}

    override fun close() {
        parent.removeRoute(this)
    }
}