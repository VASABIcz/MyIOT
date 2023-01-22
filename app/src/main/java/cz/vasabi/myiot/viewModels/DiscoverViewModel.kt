package cz.vasabi.myiot.viewModels

import android.net.nsd.NsdManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vasabi.myiot.backend.connections.DeviceInfo
import cz.vasabi.myiot.backend.connections.DeviceManager
import cz.vasabi.myiot.backend.database.discovery.DeviceResolveManager
import cz.vasabi.myiot.backend.database.discovery.DiscoveryManager
import cz.vasabi.myiot.backend.database.discovery.implementations.HttpDeviceDiscoveryService
import cz.vasabi.myiot.backend.database.discovery.implementations.TcpDeviceDiscoveryService
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val client: HttpClient,
    private val nsdManager: NsdManager,
    private val deviceManager: DeviceManager,
    private val objectMapper: ObjectMapper,
    private val deviceResolveManager: DeviceResolveManager
) : ViewModel() {
    val devices: SnapshotStateList<DeviceInfo> = SnapshotStateList()
    val showLoading = mutableStateOf(true)

    var timeoutCoroutine = viewModelScope.launch {
        delay(10_000)
        showLoading.value = false
    }

    private val discoveryManager = DiscoveryManager {
        viewModelScope.launch(Dispatchers.IO) {
            it.getDeviceInfo()?.let {
                if (!deviceManager.isRegistered(it)) {
                    try {
                        devices.add(it)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }.apply {
        addService(HttpDeviceDiscoveryService(nsdManager, {}, {}, client, deviceResolveManager))
        addService(
            TcpDeviceDiscoveryService(
                nsdManager,
                {},
                {},
                objectMapper,
                deviceResolveManager
            )
        )
    }

    init {
        discoveryManager.start()
    }

    fun addDevice(info: DeviceInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            deviceManager.registerConnection(info)
            devices.remove(info)
        }
        timeoutCoroutine.cancel()
        timeoutCoroutine = viewModelScope.launch {
            delay(10_000)
            showLoading.value = false
        }
    }

    override fun onCleared() {
        discoveryManager.close()
        super.onCleared()
    }
}