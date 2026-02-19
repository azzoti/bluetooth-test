package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class AndroidBluetoothService(private val context: Context) : BluetoothService {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val scanner = adapter?.bluetoothLeScanner
    private var callback: ScanCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    override fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (adapter == null || !adapter.isEnabled) return
        
        // Stop previous scan if any
        stopScan()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (device.name != null) {
                        Log.d("AndroidBluetoothService", "Found device: ${device.name} - ${device.address}")
                        val bluetoothDevice = BluetoothDevice(
                            name = device.name,
                            address = device.address,
                            rssi = result.rssi
                        )
                        handler.post { onDeviceFound(bluetoothDevice) }
                    }
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e("AndroidBluetoothService", "Scan failed with error: $errorCode")
            }
        }
        
        try {
            scanner?.startScan(callback)
        } catch (e: SecurityException) {
            Log.e("AndroidBluetoothService", "Permission missing for bluetooth scan", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        try {
            callback?.let { scanner?.stopScan(it) }
        } catch (e: SecurityException) {
            Log.e("AndroidBluetoothService", "Permission missing for stopping scan", e)
        }
        callback = null
    }
}
