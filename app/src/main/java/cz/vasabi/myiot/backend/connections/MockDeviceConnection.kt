package cz.vasabi.myiot.backend.connections

import cz.vasabi.myiot.backend.api.DataMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MockDeviceConnection(
    private val deviceCapabilities: List<DeviceCapability>? = null,
    private val deviceInfo: DeviceInfo? = null,
) : DeviceConnection {
    override val connectionType: ConnectionType = ConnectionType.Mock
    override var onConnectionChanged: suspend (ConnectionState) -> Unit = {}
    override val identifier: String = UUID.randomUUID().toString()
    val scope = CoroutineScope(Dispatchers.IO)

    override fun connect() {
        scope.launch {
            while (true) {
                delay(1000)
                onConnectionChanged(ConnectionState.Disconnected)
                delay(1000)
                onConnectionChanged(ConnectionState.Loading)
                delay(1000)
                onConnectionChanged(ConnectionState.Connected)
            }
        }
    }

    override suspend fun disconnect() {
        scope.cancel()
    }

    override suspend fun getCapabilities(): List<DeviceCapability> {
        return deviceCapabilities ?: listOf(
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            MockDeviceCapability("/mock", "mock bool capability", "i dont do much", "bool"),
            // MockDeviceCapability("/mock", "mock float capability", "i dont do much", "float"),
            MockIntCapability("/mock", "mock int capability", "i dont do much"),
            MockDeviceCapability("/mock", "mock string capability", "i dont do much", "string")
        )
    }

    override suspend fun getDeviceInfo(): DeviceInfo {
        return deviceInfo ?: DeviceInfoImpl(
            "mock device",
            "this device is used for testing purposes",
            identifier,
            ConnectionType.Mock, this,
            "no info provided"
        )
    }

}

class MockDeviceCapability(
    override val route: String,
    override val name: String,
    override val description: String,
    override val type: String,
    override var onReceived: suspend (DataMessage) -> Unit = {}
) : DeviceCapability {
    override fun requestValue() {}

    override fun setValue(value: Any, type: String) {}

    override fun close() {}
}

class MockIntCapability(
    override val route: String,
    override val name: String,
    override val description: String,
    override var onReceived: suspend (DataMessage) -> Unit = {}
) : DeviceCapability {
    override val type: String = "int"
    val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                // TODO
                // onReceived(Data.I(Random.nextInt(0..30)))
                delay(500)
            }
        }
    }

    override fun requestValue() {}

    override fun setValue(value: Any, type: String) {}

    override fun close() {}
}
