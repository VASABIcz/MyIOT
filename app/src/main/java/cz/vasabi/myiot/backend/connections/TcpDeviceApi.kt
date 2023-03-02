package cz.vasabi.myiot.backend.connections

import JsonDeviceCapability
import cz.vasabi.myiot.backend.api.DataMessage
import cz.vasabi.myiot.backend.database.TcpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.database.TcpDeviceConnectionEntity
import cz.vasabi.myiot.backend.logging.logger
import cz.vasabi.myiot.backend.serialization.BinaryDeserializer
import cz.vasabi.myiot.backend.serialization.BinarySerializer
import cz.vasabi.myiot.backend.serialization.serialize
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.connection
import io.ktor.network.sockets.isClosed
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeAvailable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteOrder

/* FIXME

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



data class TcpResponse(val route: String, val type: String, val value: ByteArray)

internal sealed interface TcpRequest {
    object Capabilities : TcpRequest
    class Post(val route: String, val data: Any) : TcpRequest
    class Get(val route: String) : TcpRequest
}

internal class TcpConnectionManager(
    var host: String,
    var port: Int,
    var onConnectionChanged: suspend (ConnectionState) -> Unit
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)

    @Volatile
    @get:Synchronized
    @set:Synchronized
    private var socket: Connection? = null

    suspend fun reconnect() {
        if (socket?.socket?.isClosed == true) return
        while (true) {
            try {
                socket = aSocket(selectorManager).tcp().connect(host, port).connection()
                onConnectionChanged(ConnectionState.Connected)
                return
            } catch (t: Throwable) {
                logger.warning("socket reconnect ${t.message}", this)
                delay(1000)
            }
        }
    }

    suspend fun readPacket(): ByteArray {
        while (true) {
            try {
                val len = socket?.input?.readInt()?.let {
                    if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                        it.reverseByteOrder()
                    } else {
                        it
                    }
                } ?: continue
                println("allocating $len")
                val buf = ByteArray(len)
                socket?.input?.readFully(buf)
                return buf
            } catch (t: Throwable) {
                logger.debug("socket readLine ${t.message}", this)
                onConnectionChanged(ConnectionState.Disconnected)
                reconnect()
                delay(1000)
            }
        }
    }

    suspend fun writePacket(data: ByteArray) {
        while (true) {
            try {
                if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                    socket?.output?.writeInt(data.size.reverseByteOrder())
                } else {
                    socket?.output?.writeInt(data.size)
                }
                socket?.output?.writeAvailable(data)
                socket?.output?.flush()
                return
            } catch (t: Throwable) {
                logger.debug("socket write ${t.message}", this)
                onConnectionChanged(ConnectionState.Disconnected)
                reconnect()
            }
        }
    }

    fun close() {
        socket?.socket?.close()
        socket?.socket?.dispose()
    }
}

class TcpDeviceConnection(val info: IpConnectionInfo) :
    DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Tcp
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}
        set(value) {
            connManager.onConnectionChanged = value
            field = value
        }
    override val identifier = info.identifier
    private val scope = CoroutineScope(Dispatchers.IO)

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val responseChannel = Channel<TcpResponse>()
    private val capabilitiesChannel = Channel<CompletableDeferred<List<DeviceCapability>?>>()
    private val requestChannel = Channel<TcpRequest>()

    private val capabilities = hashMapOf<String, MutableList<TcpDeviceCapability>>()
    private val connManager = TcpConnectionManager(info.host, info.port, onConnectionChanged)

    override fun connect() {
        // TODO try catch
        scope.launch {
            connManager.reconnect()
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
            logger.debug("undelivered tcp message $message", this)
            return
        }

        scope.launch {
            capability.forEach {
                it.onReceived(
                    DataMessage(
                        message.type,
                        BinaryDeserializer(message.value.inputStream())
                    )
                )
            }
        }
    }

    private suspend fun socketReader() {
        while (true) {
            val res = connManager.readPacket()
            val des = BinaryDeserializer(res.inputStream())

            val msgType = des.readString()

            when (msgType) {
                "capabilities" -> {
                    val capabilities = mutableListOf<JsonDeviceCapability>()
                    val capabilitiesCount = des.readInt() ?: continue

                    repeat(capabilitiesCount) {
                        val route = des.readString() ?: return@repeat
                        val name = des.readString() ?: return@repeat
                        val description = des.readString() ?: return@repeat
                        val type = des.readString() ?: return@repeat
                        capabilities.add(JsonDeviceCapability(route, name, description, type))
                    }
                    val c = capabilitiesChannel.receive()
                    c.complete(capabilities.map { TcpDeviceCapability(it, this) })
                }

                "value" -> {
                    val route = des.readString() ?: continue
                    val type = des.readString() ?: continue
                    val remaining = des.array.readBytes()

                    responseChannel.send(TcpResponse(route, type, remaining))
                }

                else -> {
                    println("unknown message type $msgType")
                }
            }
            des.array.close()
        }
    }

    private suspend fun handleRequests() {
        for (request in requestChannel) {
            val serializer = BinarySerializer()
            when (request) {
                TcpRequest.Capabilities -> {
                    serializer.writeString("capabilities")
                }

                is TcpRequest.Get -> {
                    serializer.writeString("get")
                    serializer.writeString(request.route)
                }

                is TcpRequest.Post -> {
                    serializer.writeString("post")
                    serializer.writeString(request.route)
                    val res = serializer.serialize(request.data)
                    if (!res) {
                        logger.warning("tcp failed to serialize ${request.data}", this)
                        continue
                    }
                }
            }
            connManager.writePacket(serializer.data.toByteArray())
            logger.debug("finished writing $request", this)
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            selectorManager.close()
            connManager.close()
        }
    }

    override suspend fun getCapabilities(): List<DeviceCapability>? {
        logger.debug("tcp requesting capabilities", this)
        val future = CompletableDeferred<List<DeviceCapability>?>()
        println("i am waiting")
        requestChannel.send(TcpRequest.Capabilities)
        println("i am 123")
        capabilitiesChannel.send(future)
        println("i am waiting")
        val res = withTimeoutOrNull(1000) {
            future.await()
        }


        logger.debug("KYS $res")
        logger.error("getCapabilities tcp $res", this)

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

    fun setValue(route: String, value: Any) {
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

    override fun setValue(value: Any, type: String) {
        parent.setValue(route, value)
    }

    override var onReceived: suspend (DataMessage) -> Unit = {}

    override fun close() {
        parent.removeRoute(this)
    }
}