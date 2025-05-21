# Photo Organizer App

An Android application that automatically organizes photos into categories using machine learning.

## Features

- Scans device photos and categorizes them using ML
- Organizes photos into different folders based on content
- Allows manual review and correction of categorizations
- Supports categories: People, Nature, Documents, Screenshots, and Others
- Modern Material Design UI
- Learns from user corrections

## Requirements

- Android Studio Arctic Fox or newer
- Android SDK 33 (Android 13)
- Minimum SDK: 24 (Android 7.0)
- Kotlin 1.8.0 or newer

## Building the App

1. Clone the repository
2. Open the project in Android Studio
3. Wait for Gradle sync to complete
4. Build the project using one of these methods:
   - Click "Build > Make Project"
   - Use keyboard shortcut (Ctrl+F9 on Windows/Linux, Cmd+F9 on macOS)
   - Run `./gradlew assembleDebug` in terminal

## Testing

To install and test the app:

1. Connect an Android device or start an emulator
2. Run the app using one of these methods:
   - Click "Run > Run 'app'"
   - Use keyboard shortcut (Shift+F10 on Windows/Linux, Ctrl+R on macOS)
   - Run `./gradlew installDebug` in terminal

## Permissions

The app requires the following permissions:
- READ_EXTERNAL_STORAGE: To access photos
- WRITE_EXTERNAL_STORAGE: To organize photos into folders

## Building Release APK

To create a signed release APK:

1. In Android Studio, go to Build > Generate Signed Bundle/APK
2. Select APK
3. Create or choose a keystore
4. Select release build type
5. Wait for build to complete

Alternatively, use command line:
```bash
./gradlew assembleRelease
```

The APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

## Troubleshooting

Common issues and solutions:

1. Gradle sync fails
   - Update Android Studio
   - Invalidate caches/restart
   - Check internet connection

2. Build errors
   - Clean project (Build > Clean Project)
   - Rebuild project
   - Check SDK installation

3. Runtime crashes
   - Check logcat for detailed error messages
   - Ensure all permissions are granted
   - Verify device has enough storage

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details
