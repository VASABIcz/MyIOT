package cz.vasabi.myiot.backend.connections

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


/*
internal data class TcpResponse(
    override val value: String,
    override val type: String,
    val route: String
)

internal sealed interface TcpRequest {
    object Capabilities : TcpRequest
    class Post(val route: String, val data: String) : TcpRequest
    class Get(val route: String) : TcpRequest
}

internal class TcpConnectionManager(
    var host: String,
    var port: Int,
    var onConnectionChanged: suspend (ConnectionState) -> Unit
) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var socket: Connection? = null

    suspend fun reconnect() {
        if (socket?.socket?.isClosed == true) return
        while (true) {
            try {
                socket = aSocket(selectorManager).tcp().connect(host, port) {
                    noDelay = true
                    sendBufferSize = 1
                }.connection()
                onConnectionChanged(ConnectionState.Connected)
                break
            } catch (t: Throwable) {
                logger.warning("socket reconnect ${t.message}", this)
                delay(1000)
            }
        }
    }

    suspend fun readLine(): String {
        while (true) {
            try {
                return socket?.input?.readUTF8Line() ?: continue
            } catch (t: Throwable) {
                logger.debug("socket readLine ${t.message}", this)
                onConnectionChanged(ConnectionState.Disconnected)
                reconnect()
            }
        }
    }

    suspend fun write(line: String) {
        while (true) {
            try {
                socket?.output?.writeStringUtf8(line)
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

class TcpDeviceConnection(val info: IpConnectionInfo, private val objectMapper: ObjectMapper) :
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
                it.onReceived(message.toData())
            }
        }
    }

    private suspend fun socketReader() {
        while (true) {
            val message = connManager.readLine()
            logger.debug("message $message", this)

            if (message.startsWith("capabilities")) {
                handleCapabilitiesResult(message)
            } else if (message.startsWith("value")) {
                handleValueResult(message)
            } else {
                logger.error("tcp conn $this received unknown message: $message", this)
            }
        }
    }

    private suspend fun handleCapabilitiesResult(message: String) {
        val striped = message.replace("capabilities ", "")
        val res: List<JsonDeviceCapability> = try {
            objectMapper.readValue(striped)
        } catch (_: Throwable) {
            logger.error("$message was invalid capability result", this)
            // FIXME not sure about this
            capabilitiesChannel.receive().complete(null)
            return
        }

        val future = capabilitiesChannel.receive()

        if (future.isCancelled) {
            logger.error("we meet again", this)
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
            logger.error("invalid value result $message", this)
            return
        }
        val route = striped.slice(0 until routeEnd)
        val payload = striped.slice(routeEnd + 1 until striped.length)
        val data: GenericHttpResponse = try {
            objectMapper.readValue(payload)
        } catch (_: Throwable) {
            logger.error("failed to parse generic response tcp $message , parsed: $payload", this)
            return
        }
        responseChannel.send(TcpResponse(data.value, data.type, route))
    }

    private suspend fun handleRequests() {
        for (request in requestChannel) {
            when (request) {
                TcpRequest.Capabilities -> connManager.write("capabilities\n")
                is TcpRequest.Get -> connManager.write("get ${request.route}\n")
                is TcpRequest.Post -> connManager.write("post ${request.route} ${request.data}\n")
            }
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
        logger.error("HUH", this)
        requestChannel.send(TcpRequest.Capabilities)
        capabilitiesChannel.send(future)
        logger.error("FUCK", this)
        logger.error("GOOD", this)

        val res = withTimeoutOrNull(1000) {
            future.await()
        }


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
 */