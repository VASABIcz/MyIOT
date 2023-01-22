package cz.vasabi.myiot.backend.database.discovery.implementations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import cz.vasabi.myiot.backend.connections.DeviceConnection
import cz.vasabi.myiot.backend.database.discovery.DiscoveryService


// TODO

class DeviceSearchBLE(
    override var onDeviceResolved: (DeviceConnection) -> Unit,
    private val bluetoothAdapter: BluetoothAdapter
) : DiscoveryService {
    override var isDone: Boolean = false

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun start() {
        TODO("Not yet implemented")
        // TODO permission checks
    }


    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000


    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
        }
    }
}