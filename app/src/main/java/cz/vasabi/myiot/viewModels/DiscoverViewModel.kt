package cz.vasabi.myiot.viewModels

import android.net.nsd.NsdManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.vasabi.myiot.backend.DeviceInfo
import cz.vasabi.myiot.backend.DeviceManager
import cz.vasabi.myiot.backend.discovery.DiscoveryManager
import cz.vasabi.myiot.backend.discovery.HttpDeviceDiscoveryService
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(private val client: HttpClient, private val nsdManager: NsdManager, private val deviceManager: DeviceManager): ViewModel() {
    val devices: SnapshotStateList<DeviceInfo> = SnapshotStateList()
    val isDiscovering = mutableStateOf(false) // TODO

    private val discoveryManager = DiscoveryManager {
        viewModelScope.launch {
            it.getDeviceInfo()?.let {
                if (!deviceManager.isRegistered(it))
                    devices.add(it)
            }
        }
    }.apply {
        addService(HttpDeviceDiscoveryService(nsdManager, {}, {}, client))
    }

    init {
        discoveryManager.start()
    }

    fun addDevice(info: DeviceInfo) {
        viewModelScope.launch {
            deviceManager.registerConnection(info)
            devices.remove(info)
        }
    }

    override fun onCleared() {
        discoveryManager.close()
        super.onCleared()
    }
}