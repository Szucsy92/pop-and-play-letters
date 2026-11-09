# Gyerek billentyűzet – mobilos kiadás

## Mi készült el?
A GitHub Actions → **Gyerek billentyűzet – APK és IPA** → **Run workflow** készíti a dátumozott kiadásokat. Androidon állandó aláírókulcsot használ; iPhone-ra aláíratlan IPA-t csomagol. A játék és a betűtípus a telepítőben van, internet nélkül is működik. A webes előnézet változatlanul használható.

## Egyszeri Android-kulcs (100 év)
Saját számítógépen, JDK-val, a projekten kívüli privát mappában futtasd:

```sh
keytool -genkeypair -keystore playful-keypad.keystore -alias playfulkeypad -keyalg RSA -keysize 4096 -validity 36525 -dname "CN=Gyerek billentyuzet" -storetype PKCS12
```
A jelszót a kérdésnél add meg, ne írd parancsba és ne küldd chatben. A kulcsot és a jelszót őrizd meg biztonságos tartalék másolatban: elvesztése esetén a meglévő alkalmazás nem frissíthető ugyanazzal az aláírással. A 100 év a tanúsítvány érvényessége, nem működési garancia.

GitHub → a projekt → Settings → Secrets and variables → Actions → New repository secret:
- `ANDROID_KEYSTORE_PASSWORD`: a választott jelszó.
- `ANDROID_KEYSTORE_BASE64`: a kulcs base64-kódolt tartalma. Linux: `base64 -w 0 playful-keypad.keystore`; macOS: `base64 < playful-keypad.keystore | tr -d '\n'`. Ezt csak a GitHub titkos mezőjébe másold, ne a projektbe vagy chatbe.

Nem generálunk minden futáskor új kulcsot. Hiányzó kulcs esetén az APK-készítés leáll.

## Frissítés az appon belül (OTA)
A szülői menü **Frissítés keresése** gombja után a **Frissítés telepítése** az appban letölti az új APK-t, majd megnyitja a rendszer telepítőjét. Első alkalommal a telefon rákérdez a „telepítés ismeretlen forrásból” engedélyre ennél az appnál. Ugyanaz a kulcs és alkalmazásazonosító kell, ekkor a frissítés a meglévő appra települ. Az Android-oldali kódot a CI a `scripts/android/` mappából másolja be.

## Frissítés (korábbi leírás)
A szülői menü **Frissítés keresése** gombja az újabb GitHub-kiadást keresi. **Új APK letöltése** megnyitja a letöltést; a telefonon engedélyezni kell a telepítést. Ez internetes APK-frissítés, nem csendes háttértelepítés és nem külön webes OTA-csomag. A játék frissítés nélkül is offline marad. iPhone-on az új IPA-t ismét telepíteni/aláírni kell.

A frissítésellenőrzéshez nyilvánosan elérhető GitHub Releases szükséges. Privát projekt esetén az APK/IPA a GitHubba bejelentkezve letölthető, de az alkalmazás frissítésellenőrzése nem működik; privát hozzáférési kulcsot nem építünk a játékba. Ha a forrásnak privátnak kell maradnia, külön nyilvános kiadási hely szükséges.

## Fontos
- Alkalmazásazonosító: `hu.playfulkeypad.app`; első telepítés után ezt és a kulcsot ne változtasd meg.
- Dátum: budapesti idő szerint. Verziószám: az Actions futás sorszáma, ezért a munkafolyamatot ne cseréld másik sorszámozásra meglévő telepítés után.
- Az iPhone aláírás érvényességét az Apple és a telepítési mód határozza meg; ingyenes Apple-fióknál rendszeres újraaláírás szükséges.
- A telefon saját rendszer-gesztusainak teljes zárolásához továbbra is Android alkalmazásrögzítés / iPhone Irányított hozzáférés kell.
- Itt valódi APK/IPA még nem készült: az első GitHub-futtatás és fizikai telefonos ellenőrzés szükséges.

## Fejlesztői parancsok
`bun install --frozen-lockfile`, majd `bun run build:app`; `bunx cap add android` / `bunx cap add ios --packagemanager CocoaPods`, utána `bunx cap sync`.
A natív projektek generáltak; privát kulcsot soha ne másolj a forrásba.
