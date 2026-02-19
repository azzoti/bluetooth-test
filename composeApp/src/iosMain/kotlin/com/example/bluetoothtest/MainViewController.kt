package com.example.bluetoothtest

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val bluetoothService = IosBluetoothService()
    return ComposeUIViewController {
        App(bluetoothService)
    }
}
