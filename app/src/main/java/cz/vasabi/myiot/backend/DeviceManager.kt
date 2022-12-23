package cz.vasabi.myiot.backend

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import cz.vasabi.myiot.backend.database.AppDatabase
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceManager(
    database: AppDatabase,
    private val client: HttpClient
    ) {
    val devices: SnapshotStateMap<String, DeviceState> = mutableStateMapOf()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpCapabilityDao = database.httpCapabilityDao()
    private val httpConnectionDao = database.httpConnectionsDao()
    private val deviceDao = database.deviceDao()

    init {
        scope.launch {
            deviceDao.getAll().forEach {
                println("loading device $it from database")
                val device = DeviceState(it)
                val conn = httpConnectionDao.findConnection(it.identifier)
                if (conn != null) {
                    println("loading connection $conn from database")
                    val httpConn = DeviceConnectionState(
                        HttpDeviceConnection(conn, client),
                        this@DeviceManager
                    )
                    device.connections[ConnectionType.Http] = httpConn
                    val capabilities = httpCapabilityDao.findCapabilities(it.identifier)
                    capabilities.forEach { capability ->
                        println("loading capability $capability from database")
                        httpConn.deviceCapabilities.add(DeviceCapabilityState(HttpDeviceCapability(capability, conn, client)))
                    }
                    httpConn.connect()
                }
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
                is DeviceConnectionState -> TODO()
                is HttpDeviceConnection -> {
                    httpConnectionDao.insertAll(info.parent.info.toEntity())
                }
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
                is DeviceConnectionState -> TODO()
                is HttpDeviceConnection -> {
                    httpCapabilityDao.insertAll(it.toEntity(conn.info.identifier))
                }
            }
        }
    }
}