package cz.vasabi.myiot.backend.discovery

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import cz.vasabi.myiot.backend.DeviceConnection

class BLEDiscoveryService(
    override var onDeviceResolved: (DeviceConnection) -> Unit,
    val bluetoothAdapter: BluetoothAdapter
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

    private fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothAdapter.bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
        }
    }


}