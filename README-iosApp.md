# iOS Project Setup Guide

This guide explains how to set up the Xcode project to work with the Kotlin Multiplatform shared module.

## Prerequisites
- A Mac with macOS.
- Xcode installed.
- Android Studio installed (for the Java Runtime).

## Step 1: Create the Xcode Project
1.  Open **Xcode**.
2.  Select **Create a new Xcode project**.
3.  Choose **App** under the iOS tab.
4.  Enter the following details:
    *   **Product Name**: `iosApp`
    *   **Organization Identifier**: `com.example.bluetoothtest`
    *   **Interface**: SwiftUI
    *   **Language**: Swift
5.  Save the project in the `iosApp` folder of your project directory (`bluetooth-test/iosApp`).
    *   *Note: Ensure the folder structure becomes `bluetooth-test/iosApp/iosApp.xcodeproj`.*

## Step 2: Replace Swift Files
Replace the content of `ContentView.swift` and `iosAppApp.swift` with the provided code that bridges the Kotlin `MainViewController`.

## Step 3: Link the Kotlin Framework
1.  In Xcode, click on the **project root** in the left navigator (blue icon).
2.  Select the **Target** (`iosApp`).
3.  Go to the **Build Phases** tab.
4.  Click the **+** icon and select **New Run Script Phase**.
5.  Drag the new "Run Script" phase to be the **first** phase (above "Compile Sources").
6.  Paste the following script:
    ```bash
    cd "$SRCROOT/../../"
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
    ```
    *Note: Adjust `JAVA_HOME` if your Android Studio installation is different (e.g. `/jre/` instead of `/jbr/`).*

## Step 4: Configure Build Settings
1.  Go to the **Build Settings** tab.
2.  **Disable Sandboxing**:
    *   Search for **User Script Sandboxing**.
    *   Set it to **No**.
3.  **Linker Flags**:
    *   Search for **Other Linker Flags**.
    *   Add: `-framework ComposeApp`
4.  **Search Paths**:
    *   Search for **Framework Search Paths**.
    *   Add: `$(SRCROOT)/../../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`

## Step 5: Add Permissions
1.  In Xcode, open the **Info** tab of your Target (or edit `Info.plist` directly).
2.  Add a new key: **Privacy - Bluetooth Always Usage Description**.
3.  Set the value to: "This app needs Bluetooth access to scan for devices."
    *   *Note: Also add `CADisableMinimumFrameDurationOnPhone` set to `true` for smoother animations on ProMotion displays.*

## Running the App
1.  Connect your iPhone or select a Simulator.
2.  Press **Cmd + R** to run.
3.  If you get a deployment target error, lower the Deployment Target in **General > Minimum Deployments** to iOS 15.0.

## How it Works: Xcode & Kotlin
You might wonder why Xcode is involved with Kotlin code. Here is the flow:

1.  **Xcode "Run Script" Phase**: When you click build/run in Xcode, the first thing it does (because we put it first) is execute:
    `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
2.  **Gradle & Kotlin/Native Compiler**: This Gradle task invokes the Kotlin/Native compiler. The compiler takes your Kotlin code (`commonMain` + `iosMain`) and compiles it into an **Apple Framework** (binary machine code for iOS ARM64).
3.  **Linking**: Xcode then takes this generated framework (`ComposeApp.framework`) and links it into your iOS app executable, just like it would with any other Swift or Objective-C library.
4.  **Swift Interop**: Your Swift code (`ContentView.swift`) calls the Kotlin code (exposed as Objective-C headers in the framework), allowing the UI to launch.

So, Xcode isn't *compiling* the Kotlin code itself; it's asking Gradle to do it, and then consuming the result.
