package cz.vasabi.myiot.viewModels

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import cz.vasabi.myiot.backend.Device
import cz.vasabi.myiot.backend.DeviceManager
import cz.vasabi.myiot.backend.DeviceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(private val deviceManager: DeviceManager): ViewModel() {

    // TODO query db for last selected device
    var selectedDevice = mutableStateOf(deviceManager.devices.values.firstOrNull())

    val devices: SnapshotStateMap<String, DeviceState>
        get() = deviceManager.devices
}