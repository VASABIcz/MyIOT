package cz.vasabi.myiot.backend.connections

import JsonDeviceCapability
import cz.vasabi.myiot.backend.api.DataMessage
import cz.vasabi.myiot.backend.api.deserializeMsg
import cz.vasabi.myiot.backend.api.serialize
import cz.vasabi.myiot.backend.database.HttpDeviceConnectionEntity
import cz.vasabi.myiot.backend.logging.logger
import cz.vasabi.myiot.backend.serialization.BinaryDeserializer
import cz.vasabi.myiot.backend.serialization.deserialize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
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

                val resValue: ByteArray = res.body()
                logger.debug("received $resValue", this)
                onReceived(deserializeMsg(resValue.inputStream()))
            } catch (e: Throwable) {
                // FIXME
                logger.debug(e.toString(), this)
            }
        }
    }

    override fun setValue(value: Any, type: String) {
        scope.launch {
            println("serial $type $value")
            val data = serialize(type, value)
            println("ser data ${data.size()} ${data.toByteArray().decodeToString()}")
            data.toByteArray().forEach {
                println("byte $it")
            }
            val res = try {
                logger.debug("sending POST http://${info.host}:${info.port}${route}", this)
                client.post("http://${info.host}:${info.port}${route}") {
                    setBody(data.toByteArray().encodeBase64())
                }
            } catch (_: Exception) {
                return@launch
            }
            logger.debug(res.toString(), this)

            val resValue = deserializeMsg(res.body<ByteArray>().inputStream())
            logger.debug("received $resValue", this)
            onReceived(resValue)
        }
    }

    override var onReceived: suspend (DataMessage) -> Unit = {
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
                    val x = client.get("http://${info.host}:${info.port}/api/capabilities")
                        .body<String>().decodeBase64Bytes()
                    logger.debug("msg size: ${x.size}", this)
                    val res =
                        BinaryDeserializer(x.inputStream()).deserialize<List<JsonDeviceCapability>>()
                    logger.debug(res.toString(), this)
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
        val data: ByteArray = try {
            client.get("http://${info.host}:${info.port}/api/capabilities").body<String>()
                .decodeBase64Bytes()
        } catch (e: Exception) {
            return null
        }
        val deviceCapabilities =
            BinaryDeserializer(data.inputStream()).deserialize<List<JsonDeviceCapability>>()

        if (deviceCapabilities == null) {
            logger.debug("got null device caps http $identifier ${info.name}", this)
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