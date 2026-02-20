# Bluetooth Handling Logic

This document describes the implementation details of the Bluetooth scanning, connection, and disconnection logic within the application.

## Overview
The application uses a shared `BluetoothService` interface to provide platform-agnostic Bluetooth operations. Implementations for Android and iOS handle the specific platform APIs.

## Logic Flow

### 1. Scanning
- **Auto-Start**: Scanning starts automatically when the "Devices" tab is opened.
- **Duration**: Scans run for 10 seconds and then stop automatically.
- **Filtering**: Only devices with a valid name (not null) are reported to the UI.
- **Platform Specifics**:
    - **Android**: Uses `BluetoothLeScanner`. Checks for `BLUETOOTH_SCAN` permissions.
    - **iOS**: Uses `CBCentralManager`. Checks for `CBManagerStatePoweredOn`.

### 2. Connection
- **Trigger**: User clicks on a device item in the "Other Devices" list.
- **Target Filter**: Connection logic is currently restricted to devices named **"Oralable"** or **"ANR M40"**.
- **Action**:
    1.  The device is moved from "Other Devices" list to "My Devices" list.
    2.  `BluetoothService.connect(address)` is called.
- **Platform Specifics**:
    - **Android**: Calls `device.connectGatt(autoConnect = false)`. Upon connection (`STATE_CONNECTED`), it immediately calls `gatt.discoverServices()`.
    - **iOS**: Retrieves the `CBPeripheral` (from cache or UUID) and calls `connectPeripheral`. Upon connection, it triggers `discoverServices`.
- **Service Discovery**: Once connected, the list of discovered Service UUIDs is logged to the platform's console (Logcat or Xcode Console).

### 3. Disconnection & App Lifecycle Integration
To ensure Bluetooth resources are released correctly, the app integrates with platform lifecycles to trigger `disconnectAll()`:

-   **Shared UI (Compose)**:
    -   A `DisposableEffect` is used within the `DevicesTab` composable.
    -   When the tab is closed, user navigates away, or the view is destroyed, `onDispose` triggers `bluetoothService.disconnectAll()`.

-   **Android**:
    -   `MainActivity` overrides `onDestroy()`.
    -   This ensures that if the app is swiped away from Recents or closed by the system, `disconnectAll()` is called to explicitly release `BluetoothGatt` resources.

-   **iOS**:
    -   The `DisposableEffect` in the shared UI handles cleanup if the `MainViewController` is deallocated.
    -   For hard app termination, the iOS system automatically invalidates `CBCentralManager` connections, ensuring no orphaned connections remain.

## Key Files
- `commonMain/.../BluetoothService.kt`: Interface definition.
- `commonMain/.../App.kt`: UI logic for lists and click handling.
- `androidMain/.../AndroidBluetoothService.kt`: Android implementation using `BluetoothGatt`.
- `iosMain/.../IosBluetoothService.kt`: iOS implementation using `CoreBluetooth`.
