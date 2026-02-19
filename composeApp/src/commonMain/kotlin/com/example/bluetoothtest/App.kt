package com.example.bluetoothtest

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    val scope = rememberCoroutineScope()

    fun startScan() {
        if (isScanning) return
        isScanning = true
        devices.clear()
        bluetoothService.startScan { device ->
            // Update list, avoid duplicates if needed
            if (devices.none { it.address == device.address }) {
                devices.add(device)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { startScan() },
            enabled = !isScanning,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isScanning) "Scanning..." else "Scan")
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(devices) { device ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column {
                        Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.h6)
                        Text(text = device.address, style = MaterialTheme.typography.body2)
                        Text(text = "RSSI: ${device.rssi}", style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }
}
