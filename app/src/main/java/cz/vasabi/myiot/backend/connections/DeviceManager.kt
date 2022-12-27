package cz.vasabi.myiot.backend.connections

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vasabi.myiot.backend.database.AppDatabase
import cz.vasabi.myiot.backend.database.HttpDeviceCapabilityEntity
import cz.vasabi.myiot.backend.database.TcpDeviceCapabilityEntity
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val conn = httpConnectionDao.findConnection(device.identifier) ?: return
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

    init {
        scope.launch {
            deviceDao.getAll().forEach {
                println("loading device $it from database")
                val device = DeviceState(it)
                loadTcpConn(device)
                loadHttpConn(device)
                devices[device.identifier] = device
            }
        }
    }

    fun registerConnection(info: DeviceInfo) = scope.launch {
        if (isRegistered(info)) return@launch

        if (!devices.containsKey(info.identifier)) {
            val device = DeviceState(info)

            deviceDao.insertAll(info.toEntity())

            when (info.parent) {
                is DeviceConnectionState -> throw IllegalStateException()
                is HttpDeviceConnection -> {
                    httpConnectionDao.insertAll((info.parent as HttpDeviceConnection).toEntity())
                }

                is TcpDeviceConnection -> {
                    tcpConnectionDao.insertAll((info.parent as TcpDeviceConnection).toEntity())
                }

                else -> throw IllegalStateException()
            }

            device.connections[info.connectionType] = DeviceConnectionState(info.parent, this@DeviceManager).apply {
                scope.launch {
                    connect()
                }
            }

            devices[info.identifier] = device
            return@launch
        }
        devices[info.identifier]?.connections?.set(info.parent.connectionType, DeviceConnectionState(info.parent, this@DeviceManager))
        scope.launch {
            info.parent.connect()
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

                is TcpDeviceConnection -> {
                    tcpCapabilityDao.insertAll(it.toEntity(conn.info.identifier) as TcpDeviceCapabilityEntity)
                    // Log.e(TAG, "TODO implement ME registerCapabilities")
                }

                else -> throw IllegalStateException()
            }
        }
    }
}