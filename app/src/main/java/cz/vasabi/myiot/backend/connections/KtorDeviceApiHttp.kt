package cz.vasabi.myiot.backend.connections

import cz.vasabi.myiot.backend.api.Data
import cz.vasabi.myiot.backend.api.GenericHttpResponse
import cz.vasabi.myiot.backend.database.HttpDeviceConnectionEntity
import cz.vasabi.myiot.backend.logging.logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HttpDeviceCapability(
    val device: BaseDeviceCapability,
    private val info: IpConnectionInfo,
    private val client: HttpClient
) : DeviceCapability, BaseDeviceCapability by device {
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun requestValue() {
        scope.launch {
            try {
                logger.debug("sending GET", this)
                val res = client.get("http://${info.host}:${info.port}${route}")

                logger.debug(res.toString(), this)

                val resValue: GenericHttpResponse = res.body()
                logger.debug("received $resValue", this)
                onReceived(resValue.toData())
            } catch (e: Throwable) {
                // FIXME
                logger.debug(e.toString(), this)
            }
        }
    }

    override fun setValue(value: Data) {
        scope.launch {
            val res = try {
                logger.debug("sending POST http://${info.host}:${info.port}${route}", this)
                client.post("http://${info.host}:${info.port}${route}") {
                    setBody(value.jsonBody)
                }
            } catch (_: Exception) {
                return@launch
            }
            logger.debug(res.toString(), this)

            val resValue: GenericHttpResponse = res.body()
            logger.debug("received $resValue", this)
            onReceived(resValue.toData())
        }
    }

    override var onReceived: suspend (Data) -> Unit = {
        logger.debug("what is dis?", this)
    }

    override fun close() {
        scope.cancel()
    }
}

class HttpDeviceConnection(val info: IpConnectionInfo, private val client: HttpClient) :
    DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Http
    private val scope = CoroutineScope(Dispatchers.IO)
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}
    override val identifier: String = info.identifier
    private var isConnected = false

    override fun connect() {
        if (isConnected) return
        isConnected = true
        scope.launch {
            while (true) {
                try {
                    val x: List<JsonDeviceCapability> =
                        client.get("http://${info.host}:${info.port}/api/capabilities").body()
                    logger.debug(x.toString(), this)
                    onConnectionChanged(ConnectionState.Connected)
                } catch (e: Exception) {
                    logger.debug(e.message.toString(), this)
                    logger.error(e.stackTraceToString(), this)
                    onConnectionChanged(ConnectionState.Disconnected)
                }
                delay(5000)
            }
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
    }

    override suspend fun getCapabilities(): List<DeviceCapability>? {
        // FIXME it should be map
        val deviceCapabilities: List<JsonDeviceCapability> = try {
            client.get("http://${info.host}:${info.port}/api/capabilities").body()
        } catch (e: Exception) {
            return null
        }
        val res = deviceCapabilities.map {
            return@map HttpDeviceCapability(it, info, client)
        }
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

    fun toEntity(): HttpDeviceConnectionEntity {
        return HttpDeviceConnectionEntity(
            info.identifier,
            info.host,
            info.port,
            info.description,
            info.name
        )
    }
}