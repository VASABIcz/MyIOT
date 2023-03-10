package cz.vasabi.myiot.backend.connections

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import cz.vasabi.myiot.backend.api.DataMessage
import cz.vasabi.myiot.backend.database.CapabilityReadingDao
import cz.vasabi.myiot.backend.logging.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

open class DeviceState(private val device: Device) : Device by device {
    val connections: SnapshotStateMap<ConnectionType, DeviceConnectionState> = mutableStateMapOf()
    val isFavourite = mutableStateOf(false)

    val connectionState: ConnectionState
        get() {
            return if (connections.any { it.value.connected.value == ConnectionState.Connected }) {
                ConnectionState.Connected
            } else if (connections.any { it.value.connected.value == ConnectionState.Loading }) {
                ConnectionState.Loading
            } else {
                ConnectionState.Disconnected
            }
        }
}

class DeviceCapabilityState(
    private val deviceCapability: DeviceCapability,
    private val parent: DeviceConnection
) : DeviceCapability by deviceCapability {
    private val scope = CoroutineScope(Dispatchers.IO)
    val responses: Channel<DataMessage> = Channel()

    @Inject
    lateinit var readingDao: CapabilityReadingDao
    val readings = mutableStateListOf<DataMessage>()

    init {
        deviceCapability.onReceived = { value ->
            logger.debug("adding new value to channel $value $deviceCapability", this)
            /*
            scope.launch {
                if (deviceCapability.type == "int") {
                    readings.add(Reading(Instant.now(), value.value.toDouble().toFloat()))
                }
                /*
                readingDao.insertAll(
                    CapabilityReadingEntity(
                        parent.identifier,
                        deviceCapability.name,
                        parent.connectionType.toString(),
                        deviceCapability.type,
                        value.value
                    )
                )
                */
            }

             */
            responses.send(value)
        }
    }

    override fun close() {
        scope.cancel()
        deviceCapability.close()
    }
}

class DeviceConnectionState(
    private val deviceConnection: DeviceConnection,
    private val deviceManager: DeviceManager
) : DeviceConnection by deviceConnection {
    val deviceCapabilities: SnapshotStateList<DeviceCapabilityState> = mutableStateListOf()
    val connected: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Loading)
    private val scope = CoroutineScope(Dispatchers.IO)
    val drawCapabilities = mutableStateOf(true)

    fun invertShow() {
        drawCapabilities.value = !drawCapabilities.value
    }

    init {
        deviceConnection.onConnectionChanged = {
            if (it == ConnectionState.Connected && connected.value != ConnectionState.Connected) {
                logger.debug("device just reconnected POG ${deviceCapabilities.size}", this)
                deviceCapabilities.forEach {
                    scope.launch {
                        logger.debug("requesting value $it", this)
                        it.requestValue()
                    }
                }
            }

            connected.value = it
        }
        updateCapabilities()
    }

    override suspend fun disconnect() {
        scope.cancel()
        deviceConnection.disconnect()
    }

    fun updateCapabilities() = scope.launch {
        val capabilities = getCapabilities() ?: return@launch
        deviceCapabilities.forEach {
            it.close()
        }
        deviceCapabilities.clear()
        deviceCapabilities.addAll(capabilities.map { DeviceCapabilityState(it, deviceConnection) })
        deviceManager.registerCapabilities(capabilities, deviceConnection)
    }
}