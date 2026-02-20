package com.example.bluetoothtest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App(bluetoothService: BluetoothService) {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Devices", "Settings", "About")

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Bluetooth Test") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> DevicesTab(bluetoothService)
                    1 -> Text("Settings Tab", modifier = Modifier.padding(16.dp))
                    2 -> Text("About Tab", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun DevicesTab(bluetoothService: BluetoothService) {
    var isScanning by remember { mutableStateOf(false) }
    val myDevices = remember { mutableStateListOf<BluetoothDevice>() } // Initially empty
    val otherDevices = remember { mutableStateListOf<BluetoothDevice>() }
    val scope = rememberCoroutineScope()

    // Disconnect when this composable leaves the composition (e.g. app close or tab switch if not preserved)
    // Note: To truly handle app exit, it's better done in the Activity/ViewController lifecycle, 
    // but DisposableEffect here handles tab switching or if the view is destroyed.
    DisposableEffect(Unit) {
        onDispose {
            bluetoothService.disconnectAll()
        }
    }

    fun startScan() {
        if (isScanning) return
        isScanning = true
        otherDevices.clear()
        bluetoothService.startScan { device ->
            // Update list, avoid duplicates if needed
            if (otherDevices.none { it.address == device.address } && myDevices.none { it.address == device.address }) {
                otherDevices.add(device)
            }
        }
        
        scope.launch {
            delay(10000)
            bluetoothService.stopScan()
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        startScan()
    }

    fun onDeviceClicked(device: BluetoothDevice) {
        if (device.name == "Oralable" || device.name == "ANR M40") {
            // Check if already in my devices to avoid duplicates
            if (myDevices.none { it.address == device.address }) {
                otherDevices.remove(device)
                myDevices.add(device)
                
                bluetoothService.connect(device.address) { services ->
                     // Log handled in service
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { startScan() },
            enabled = !isScanning,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isScanning) "Scanning..." else "Scan")
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            item {
                Text(
                    text = "My Devices",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (myDevices.isEmpty()) {
                item {
                    Text(
                        text = "No devices added yet.",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            } else {
                items(myDevices) { device ->
                    DeviceItem(device, onClick = { /* Already connected */ })
                }
            }

            item {
                Text(
                    text = "Other Devices",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 8.dp)
                )
            }
            
            items(otherDevices) { device ->
                DeviceItem(device, onClick = { onDeviceClicked(device) })
            }
        }
    }
}

@Composable
fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.subtitle1)
            Text(text = device.address, style = MaterialTheme.typography.body2)
            Text(text = "RSSI: ${device.rssi}", style = MaterialTheme.typography.caption)
        }
    }
}
