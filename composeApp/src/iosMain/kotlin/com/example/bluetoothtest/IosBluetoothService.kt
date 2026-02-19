package com.example.bluetoothtest

import platform.CoreBluetooth.*
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosBluetoothService : BluetoothService {
    private val delegate = BluetoothDelegate()
    private val centralManager = CBCentralManager(delegate, null)
    private var onDeviceFoundCallback: ((BluetoothDevice) -> Unit)? = null

    override fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        onDeviceFoundCallback = onDeviceFound
        if (centralManager.state == CBManagerStatePoweredOn) {
            centralManager.scanForPeripheralsWithServices(null, null)
        }
    }

    override fun stopScan() {
        centralManager.stopScan()
        onDeviceFoundCallback = null
    }

    private inner class BluetoothDelegate : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn) {
                // Determine if we should be scanning
                if (onDeviceFoundCallback != null) {
                     central.scanForPeripheralsWithServices(null, null)
                }
            }
        }

        override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber) {
            val name = didDiscoverPeripheral.name
            if (name != null) {
                val device = BluetoothDevice(
                    name = name,
                    address = didDiscoverPeripheral.identifier.UUIDString,
                    rssi = RSSI.intValue
                )
                onDeviceFoundCallback?.invoke(device)
            }
        }
    }
}
