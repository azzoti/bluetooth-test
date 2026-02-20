package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
    private var connectedGatt: BluetoothGatt? = null

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

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, onServicesDiscovered: (List<String>) -> Unit) {
        val device = adapter?.getRemoteDevice(deviceAddress) ?: return
        
        connectedGatt?.close()
        
        Log.d("AndroidBluetoothService", "Connecting to ${device.name} ($deviceAddress)")
        
        connectedGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("AndroidBluetoothService", "Connected to $deviceAddress. Discovering services...")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("AndroidBluetoothService", "Disconnected from $deviceAddress")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val serviceUuids = mutableListOf<String>()
                    
                    gatt.services.forEach { service ->
                        val serviceUuid = service.uuid.toString()
                        serviceUuids.add(serviceUuid)
                        Log.d("AndroidBluetoothService", "Service Discovered: $serviceUuid")
                        
                        service.characteristics.forEach { characteristic ->
                            val props = getPropertiesString(characteristic.properties)
                            Log.d("AndroidBluetoothService", "  Characteristic: ${characteristic.uuid}, Properties: $props")
                        }
                    }
                    
                    handler.post { onServicesDiscovered(serviceUuids) }
                } else {
                    Log.w("AndroidBluetoothService", "onServicesDiscovered received: $status")
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun disconnectAll() {
        Log.d("AndroidBluetoothService", "Disconnecting all devices")
        connectedGatt?.disconnect()
        connectedGatt?.close()
        connectedGatt = null
    }

    private fun getPropertiesString(properties: Int): String {
        val props = mutableListOf<String>()
        if ((properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0) props.add("BROADCAST")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("READ")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) props.add("WRITE_NO_RESPONSE")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("WRITE")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("NOTIFY")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("INDICATE")
        if ((properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) props.add("SIGNED_WRITE")
        return props.joinToString("|")
    }
}
