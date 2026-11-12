# Native migration (Android + iOS)

This folder contains native application implementations alongside the original Capacitor/Web app. The original web project is intentionally preserved.

## Android
Open `android/` in Android Studio (Ladybug/Koala or newer), sync Gradle, then run on a device.
- Kotlin + Jetpack Compose + Material 3
- Feature-first folders, ViewModel/StateFlow, Preferences DataStore
- Internal Hungarian keypad, system keyboard mode, character animations, Canvas confetti, synthesized low-latency tones, parent menu (5 taps / 2 sec), fullscreen and optional Android screen pinning.

## iOS
Open `ios/PlayfulKeypad.xcodeproj` in Xcode 16+ and run on iOS 17+.
- SwiftUI, `@Observable`, `UserDefaults`, Canvas confetti, AVAudioEngine tone generation, 5-tap parent menu.
- Guided Access is **not** programmatically enabled by an ordinary app. The app displays its status and directs the parent to enable it in iOS Settings / Accessibility.

## Important production notes
- This is a native migration implementation, not a signed release. Device testing, accessibility testing, audio tuning and store signing remain required.
- Android `startLockTask()` is not guaranteed to lock down a consumer device: screen pinning may show a system confirmation. Full managed kiosk mode requires Device Owner / enterprise provisioning. The parent menu's “screen pinning” action is explicit (not silently activated on launch).
- Neither platform can reliably suppress all system gestures from an ordinary app. Use Android screen pinning/Lock Task and iOS Guided Access at OS level.
- The existing app's original sound engine synthesizes sound in Web Audio; native versions synthesize tones too, so no copyrighted/external sound assets are needed.
