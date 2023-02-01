package cz.vasabi.myiot.backend.connections

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vasabi.myiot.backend.database.AppDatabase
import cz.vasabi.myiot.backend.database.HttpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.logging.logger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceManager(
    database: AppDatabase,
    private val client: HttpClient,
    private val objectMapper: ObjectMapper
) {
    val devices: SnapshotStateMap<String, DeviceState> = mutableStateMapOf()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpCapabilityDao = database.httpCapabilityDao()
    private val httpConnectionDao = database.httpConnectionsDao()
    private val tcpCapabilityDao = database.tcpCapabilityDao()
    private val tcpConnectionDao = database.tcpConnectionsDao()
    private val deviceDao = database.deviceDao()


    private suspend fun loadHttpConn(device: DeviceState) {
        val conn = httpConnectionDao.findConnection(device.identifier)

        if (conn == null) {
            logger.warning("mo http conn found", this)
            return
        }

        println("loading connection $conn from database")
        val httpConn = DeviceConnectionState(
            HttpDeviceConnection(conn, client),
            this@DeviceManager
        )
        device.connections[ConnectionType.Http] = httpConn
        val capabilities = httpCapabilityDao.findCapabilities(device.identifier)
        capabilities.forEach { capability ->
            println("loading capability $capability from database")
            httpConn.deviceCapabilities.add(
                DeviceCapabilityState(
                    HttpDeviceCapability(
                        capability,
                        conn,
                        client
                    ), httpConn
                )
            )
        }
        httpConn.connect()
    }

    /* FIXME
    private suspend fun loadTcpConn(device: DeviceState) {
        val conn = tcpConnectionDao.findConnection(device.identifier) ?: return
        println("loading connection $conn from database")
        val deviceConnection = TcpDeviceConnection(conn, objectMapper)
        val tcpConn = DeviceConnectionState(
            deviceConnection,
            this@DeviceManager
        )
        device.connections[ConnectionType.Tcp] = tcpConn
        val capabilities = tcpCapabilityDao.findCapabilities(device.identifier)
        capabilities.forEach { capability ->
            println("loading capability $capability from database")
            tcpConn.deviceCapabilities.add(
                DeviceCapabilityState(
                    TcpDeviceCapability(
                        capability,
                        deviceConnection
                    ), tcpConn
                )
            )
        }
        tcpConn.connect()
    }
     */

    init {
        scope.launch {
            deviceDao.getAll().forEach {
                println("loading device $it from database")
                val device = DeviceState(it)
                // FIXME
                // loadTcpConn(device)
                loadHttpConn(device)
                // FIXME interesting https://stackoverflow.com/questions/66891349/java-lang-illegalstateexception-when-using-state-in-android-jetpack-compose
                withContext(Dispatchers.Main) {
                    devices[device.identifier] = device
                }
            }
        }
    }

    fun registerConnection(info: DeviceInfo) = scope.launch {
        // FIXME looks sus
        if (isRegistered(info)) return@launch

        if (!devices.containsKey(info.identifier)) {
            val device = DeviceState(info)
            if (info.connectionType != ConnectionType.Mock) {
                deviceDao.insertAll(info.toEntity())
            }
            devices[info.identifier] = device
        }

        val device = devices[info.identifier] ?: return@launch

        when (info.parent) {
            is DeviceConnectionState -> throw IllegalStateException()
            is HttpDeviceConnection -> {
                logger.debug("adding http conn to DB", this)
                httpConnectionDao.insertAll((info.parent as HttpDeviceConnection).toEntity())
            }

            // FIXME
            //is TcpDeviceConnection -> {
            //    tcpConnectionDao.insertAll((info.parent as TcpDeviceConnection).toEntity())
            //}

            is MockDeviceConnection -> {}
            else -> TODO()
        }

        device.connections[info.connectionType] =
            DeviceConnectionState(info.parent, this@DeviceManager).apply {
                connect()
            }
    }



    fun isRegistered(info: DeviceInfo): Boolean {
        val device = devices[info.identifier] ?: return false

        device.connections[info.connectionType] ?: return false

        return true
    }

    fun registerCapabilities(capabilities: List<DeviceCapability>, conn: DeviceConnection) = scope.launch {
        capabilities.forEach {
            when (conn) {
                is DeviceConnectionState -> throw IllegalStateException()
                is HttpDeviceConnection -> {
                    httpCapabilityDao.insertAll(it.toEntity(conn.info.identifier) as HttpDeviceCapabilityEntity)
                }

                //is TcpDeviceConnection -> {
                //   tcpCapabilityDao.insertAll(it.toEntity(conn.info.identifier) as TcpDeviceCapabilityEntity)
                //}

                is MockDeviceConnection -> {}
                else -> TODO()
            }
        }
    }
}