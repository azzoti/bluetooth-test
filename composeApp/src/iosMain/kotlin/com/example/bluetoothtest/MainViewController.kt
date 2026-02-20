package com.example.bluetoothtest

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val bluetoothService = IosBluetoothService()
    
    val viewController = ComposeUIViewController {
        App(bluetoothService)
    }
    
    // Note: iOS lifecycle handling for app exit is typically done in the AppDelegate/SceneDelegate or by observing notifications.
    // However, since we created the service here, we can't easily attach it to the App delegate without restructuring.
    // For a simple implementation, we rely on the system closing resources, but adding an explicit cleanup if the view controller is deallocated (unlikely for main view) is good practice.
    
    return viewController
}
