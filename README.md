# AL SMS RECIVE (Native Android Service)

This is the background native Android service application. This project uses Kotlin and Gradle.

## Available Commands

Since this is not a Flutter project, `flutter run` will not work here. Instead, you can use the following custom scripts to build and run the app easily:

### 1. Build and Run on Device
To automatically build the app, install it on your connected phone, open it, and show the live logs:
```powershell
.\run.ps1
```

### 2. Build APK File Only
To only build the `.apk` file (for sharing or manual installation) without running it:
```powershell
.\build.ps1
```
*Note: This script will automatically open the folder where the `app-debug.apk` is saved once the build is finished.*

## Important Notes
- Always make sure your Android device is connected via USB and USB Debugging is enabled before running `.\run.ps1`.
- If you want to modify the main Flutter app (StealthSync), you must navigate to that specific project directory first.
