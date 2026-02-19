package com.example.bluetoothtest

interface BluetoothService {
    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit)
    fun stopScan()
}
