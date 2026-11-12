# Natív alkalmazás (Android + iOS)

Ez a könyvtár tartalmazza az eredeti, hivatalos Android- és iOS-alkalmazás forrásait. A natív forrás az egyetlen build- és release-forrás; a korábbi webes/Capacitor build workflow megszűnt.

## Android
Nyisd meg az `android/` mappát Android Studio-ban (Ladybug/Koala vagy újabb verzió).
- Kotlin + Jetpack Compose + Material 3
- Belső magyar billentyűzet és rendszerbillentyűzet mód
- Karakteranimációk, Canvas konfetti, szintetizált hangok
- Ruganyos gombnyomás, fátyolos körhullám
- Szülői menü (5 koppintás / 2 mp), teljes képernyő és opcionális képernyőrögzítés

## iOS
Nyisd meg az `ios/PlayfulKeypad.xcodeproj` projektet Xcode 16+-ban; iOS 17+ szükséges.
- SwiftUI, `@Observable`, `UserDefaults`, Canvas konfetti, AVAudioEngine hanggenerálás
- Belső billentyűzet és rendszerbillentyűzet mód
- Ruganyos gombnyomás és animált, lekerekített körhullám
- 5 koppintásos szülői menü

## Build és release
A `.github/workflows/native-build.yml` építi mindkét platform Release változatát, és csak akkor publikál közös GitHub Release-t, ha az Android- és iOS-build is sikeres. Debug build nem készül.
- Android: a `ANDROID_KEYSTORE_BASE64` és `ANDROID_KEYSTORE_PASSWORD` GitHub-secretekkel aláírt APK; ezek nélkül aláíratlan Release APK.
- iOS: aláíratlan IPA. App Store/TestFlight vagy közvetlen eszköztelepítés előtt Apple signing certificate és provisioning profile szükséges.

A rendszerfunkciók korlátozásai miatt az Android Lock Task Mode és az iOS Guided Access nem garantálható programból minden fogyasztói eszközön. iOS-en szükség lehet a Guided Access kézi bekapcsolására.
