package cz.vasabi.myiot.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vasabi.myiot.backend.connections.ConnectionState
import cz.vasabi.myiot.backend.connections.ConnectionType
import cz.vasabi.myiot.backend.connections.DeviceCapability
import cz.vasabi.myiot.backend.connections.DeviceConnection
import cz.vasabi.myiot.backend.connections.DeviceInfo
import cz.vasabi.myiot.backend.connections.DeviceManager
import cz.vasabi.myiot.backend.connections.MockDeviceConnection
import cz.vasabi.myiot.backend.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(val database: AppDatabase, val deviceManager: DeviceManager): ViewModel() {
    fun clearDatabase() = viewModelScope.launch(Dispatchers.IO) {
        database.deviceDao().wipeTable()
        database.httpCapabilityDao().wipeTable()
        database.httpConnectionsDao().wipeTable()
        database.tcpCapabilityDao().wipeTable()
        database.tcpConnectionsDao().wipeTable()
    }

    fun makeMockDevice() = viewModelScope.launch {
        val d = MockDeviceConnection().getDeviceInfo()

        deviceManager.registerConnection(d)
    }
}