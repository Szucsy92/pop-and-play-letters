# Playful Keypad

Gyerekeknek készült, teljes képernyős, natív billentyűzetes játék.

## Hivatalos alkalmazás

Az Android és iOS kiadások kizárólag a natív forrásokból készülnek:
- Android: `native/android` — Kotlin + Jetpack Compose
- iOS: `native/ios` — SwiftUI

A `.github/workflows/native-build.yml` az egyetlen hivatalos build- és release-folyamat. Minden kiadás Release konfigurációval készül; debug APK nem készül. Androidon a GitHub Actions `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` és opcionálisan `ANDROID_KEY_ALIAS` titkokkal aláírt APK-t állít elő. Kulcs nélkül aláíratlan Release APK készül. Az iOS IPA jelenleg aláíratlan; telepítéshez Apple fejlesztői aláírás és provisioning szükséges.

## Fejlesztés

- Android Studio: nyisd meg a `native/android` mappát.
- Xcode 16+: nyisd meg a `native/ios/PlayfulKeypad.xcodeproj` projektet.
- Minimum iOS verzió: iOS 17.

A rendszer teljes képernyős módja és a gyermekzár platformonként OS-szintű korlátozásoktól függ. iOS-en a Guided Access kézi bekapcsolása szükséges lehet; Androidon a képernyőrögzítés / menedzselt kiosk mód eszközfüggő.
