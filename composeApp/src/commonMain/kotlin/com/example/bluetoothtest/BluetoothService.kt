package com.example.bluetoothtest

interface BluetoothService {
    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit)
    fun stopScan()
    fun connect(deviceAddress: String, onServicesDiscovered: (List<String>) -> Unit)
    fun disconnectAll()
}
