package com.example.bluetoothtest

import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID
import platform.darwin.NSObject

class IosBluetoothService : BluetoothService {
    private val delegate = BluetoothDelegate()
    private val centralManager = CBCentralManager(delegate, null)
    private var onDeviceFoundCallback: ((BluetoothDevice) -> Unit)? = null
    private var onServicesDiscoveredCallback: ((List<String>) -> Unit)? = null

    private var isScanningRequested = false
    private val discoveredPeripherals = mutableMapOf<String, CBPeripheral>()
    private var connectedPeripheral: CBPeripheral? = null

    override fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        onDeviceFoundCallback = onDeviceFound
        isScanningRequested = true
        discoveredPeripherals.clear()
        
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

    override fun connect(deviceAddress: String, onServicesDiscovered: (List<String>) -> Unit) {
        onServicesDiscoveredCallback = onServicesDiscovered
        disconnectAll()

        val peripheral = discoveredPeripherals[deviceAddress]
        if (peripheral != null) {
            NSLog("IosBluetoothService: Connecting to cached peripheral ${peripheral.name}")
            centralManager.connectPeripheral(peripheral, null)
            connectedPeripheral = peripheral
        } else {
            val uuid = NSUUID(uUIDString = deviceAddress)
            val retrieved = centralManager.retrievePeripheralsWithIdentifiers(listOf(uuid))
            val p = retrieved.firstOrNull() as? CBPeripheral
            if (p != null) {
                NSLog("IosBluetoothService: Connecting to retrieved peripheral ${p.name}")
                discoveredPeripherals[deviceAddress] = p
                centralManager.connectPeripheral(p, null)
                connectedPeripheral = p
            } else {
                NSLog("IosBluetoothService: Peripheral not found for address $deviceAddress")
            }
        }
    }

    override fun disconnectAll() {
        connectedPeripheral?.let {
            NSLog("IosBluetoothService: Disconnecting from ${it.name}")
            centralManager.cancelPeripheralConnection(it)
        }
        connectedPeripheral = null
    }

    private inner class BluetoothDelegate : NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn) {
                if (isScanningRequested) {
                    central.scanForPeripheralsWithServices(null, null)
                }
            } else {
                if (isScanningRequested) {
                    central.stopScan()
                }
            }
        }

        override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber) {
            val name = didDiscoverPeripheral.name
            if (name != null) {
                val uuidString = didDiscoverPeripheral.identifier.UUIDString
                discoveredPeripherals[uuidString] = didDiscoverPeripheral
                
                val device = BluetoothDevice(
                    name = name,
                    address = uuidString,
                    rssi = RSSI.intValue
                )
                onDeviceFoundCallback?.invoke(device)
            }
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            NSLog("IosBluetoothService: Connected to ${didConnectPeripheral.name}")
            didConnectPeripheral.delegate = this
            didConnectPeripheral.discoverServices(null)
        }
        
        // Removed conflicting overload: centralManager(..., didFailToConnectPeripheral: ..., error: ...)
        // To fix "Conflicting overloads" error with didDisconnectPeripheral.
        
        override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?) {
            NSLog("IosBluetoothService: Disconnected from ${didDisconnectPeripheral.name}")
            if (connectedPeripheral == didDisconnectPeripheral) {
                connectedPeripheral = null
            }
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            if (didDiscoverServices == null) {
                val services = peripheral.services?.map { (it as CBService).UUID.UUIDString } ?: emptyList()
                
                peripheral.services?.forEach { service ->
                    val cbService = service as CBService
                    NSLog("IosBluetoothService: Service Discovered: ${cbService.UUID.UUIDString}")
                    peripheral.discoverCharacteristics(null, cbService)
                }
                
                onServicesDiscoveredCallback?.invoke(services)
            } else {
                NSLog("IosBluetoothService: Error discovering services: ${didDiscoverServices.localizedDescription}")
            }
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService: CBService, error: NSError?) {
            if (error == null) {
                didDiscoverCharacteristicsForService.characteristics?.forEach { characteristic ->
                    val cbChar = characteristic as CBCharacteristic
                    val props = getPropertiesString(cbChar.properties)
                    NSLog("IosBluetoothService:   Characteristic: ${cbChar.UUID.UUIDString}, Properties: $props")
                }
            } else {
                NSLog("IosBluetoothService: Error discovering characteristics for service ${didDiscoverCharacteristicsForService.UUID.UUIDString}: ${error.localizedDescription}")
            }
        }
    }

    private fun getPropertiesString(properties: CBCharacteristicProperties): String {
        val props = mutableListOf<String>()
        if ((properties and CBCharacteristicPropertyBroadcast) != 0.toULong()) props.add("BROADCAST")
        if ((properties and CBCharacteristicPropertyRead) != 0.toULong()) props.add("READ")
        if ((properties and CBCharacteristicPropertyWriteWithoutResponse) != 0.toULong()) props.add("WRITE_NO_RESPONSE")
        if ((properties and CBCharacteristicPropertyWrite) != 0.toULong()) props.add("WRITE")
        if ((properties and CBCharacteristicPropertyNotify) != 0.toULong()) props.add("NOTIFY")
        if ((properties and CBCharacteristicPropertyIndicate) != 0.toULong()) props.add("INDICATE")
        if ((properties and CBCharacteristicPropertyAuthenticatedSignedWrites) != 0.toULong()) props.add("SIGNED_WRITE")
        return props.joinToString("|")
    }
}
