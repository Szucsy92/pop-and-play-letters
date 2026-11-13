# Alkalmazásikon

Az ikon kezelése mindkét platformon be van kötve, a végleges rajzzal (béka + egyszarvú + színes billentyűzet).

## Android — `android/app/src/main/res/`
Adaptív ikon (minSdk 26, ezért nincs szükség külön PNG-kre):
- `drawable-nodpi/ic_launcher_foreground.png` – előtér, 432×432 px (108×108 dp @4x), átlátszó háttéren; a rajz a középső ~66–72 dp-ben van (biztonsági zóna)
- `values/ic_launcher_background.xml` – háttérszín (`#1B1346`), ez látszik a csempe körül
- `drawable-nodpi/ic_launcher_monochrome.png` – egyszínű változat az Android 13+ témázott ikonhoz
- `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` – összeállítás
- A `AndroidManifest.xml` az `@mipmap/ic_launcher` / `ic_launcher_round` ikonra hivatkozik.
- `../ic_launcher-playstore.png` (512×512) – a Google Play listázáshoz.

Rajz cseréje: Android Studio → *New → Image Asset*, vagy cseréld a két PNG-t (432×432, átlátszó háttér).

## iOS — `ios/PlayfulKeypad/Assets.xcassets/AppIcon.appiconset/`
- `AppIcon.png` – 1024×1024, **átlátszóság nélkül**, lekerekítés nélkül (az iOS maszkolja)
- Az Xcode-projekt `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon` beállítása és az asset catalog a Resources fázisban be van kötve.

Rajz cseréje: cseréld le az `AppIcon.png` fájlt (azonos név, 1024×1024).
