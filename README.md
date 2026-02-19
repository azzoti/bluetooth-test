# Bluetooth Test App

This is a Kotlin Multiplatform project targeting Android and iOS.

## Features
- Cross-platform UI using Compose Multiplatform.
- Bluetooth scanning on Android and iOS.
- Three tabs: Devices, Settings, About.
- Auto-scan on Devices tab open (10s duration).
- Manual scan button.

## Android
Open this project in Android Studio and run the `androidApp` configuration.
Make sure to grant Location/Bluetooth permissions on older Android versions if not prompted (though runtime permission request is implemented).

## iOS
1. Open `iosApp` folder in Xcode (you might need to generate an Xcode project or use the provided Swift files in a new Xcode project).
2. Ensure you link the `ComposeApp` framework built by the Gradle task `:composeApp:embedAndSignAppleFrameworkForXcode`.
3. Add `NSBluetoothAlwaysUsageDescription` to your `Info.plist` to allow Bluetooth usage.

## Architecture
- `composeApp`: Shared code and UI.
- `androidApp`: Android entry point.
- `iosApp`: iOS entry point.
