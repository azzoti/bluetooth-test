package com.example.bluetoothtest

import platform.CoreBluetooth.*
import platform.Foundation.NSLog
import platform.Foundation.NSNumber
import platform.darwin.NSObject

class IosBluetoothService : BluetoothService {
    private val delegate = BluetoothDelegate()
    private val centralManager = CBCentralManager(delegate, null)
    private var onDeviceFoundCallback: ((BluetoothDevice) -> Unit)? = null

    // We keep track of whether scanning was requested, so we can start when ready
    private var isScanningRequested = false

    override fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        onDeviceFoundCallback = onDeviceFound
        isScanningRequested = true
        
        // Only start scan if powered on. Otherwise, the delegate will handle it.
        if (centralManager.state == CBManagerStatePoweredOn) {
             centralManager.scanForPeripheralsWithServices(null, null)
        }
    }

    override fun stopScan() {
        isScanningRequested = false
        if (centralManager.state == CBManagerStatePoweredOn) {
            centralManager.stopScan()
        }
        onDeviceFoundCallback = null
    }

    private inner class BluetoothDelegate : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn) {
                // If scanning was requested while we were waiting for power on
                if (isScanningRequested) {
                    central.scanForPeripheralsWithServices(null, null)
                }
            } else {
                // Handle powered off or unauthorized state if needed
                if (isScanningRequested) {
                    central.stopScan()
                }
            }
        }

        override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber) {
            val name = didDiscoverPeripheral.name
            if (name != null) {
                NSLog("IosBluetoothService: Found device: $name")
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
