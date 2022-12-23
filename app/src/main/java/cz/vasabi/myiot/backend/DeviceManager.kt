package cz.vasabi.myiot.backend

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceManager {
    val devices: SnapshotStateMap<String, DeviceState> = mutableStateMapOf()
    val scope = CoroutineScope(Dispatchers.IO)

    suspend fun registerConnection(info: DeviceInfo) {
        if (isRegistered(info)) return

        info.parent.connect()

        if (!devices.containsKey(info.identifier)) {
            val device = DeviceState(info)
            device.connections[info.connectionType] = DeviceConnectionState(info.parent).apply {
                scope.launch {
                    connect()
                }
            }

            devices[info.identifier] = device
            return
        }
        scope.launch {
            info.parent.connect()
        }
        devices[info.identifier]?.connections?.set(info.parent.connectionType, DeviceConnectionState(info.parent))
    }

    fun isRegistered(info: DeviceInfo): Boolean {
        val device = devices[info.identifier] ?: return false

        device.connections[info.connectionType] ?: return false

        return true
    }
}