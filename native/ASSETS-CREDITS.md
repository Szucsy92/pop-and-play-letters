# Felhasznált anyagok (betűk, számok, betűtípus)

A szülői menü „betűtípus” választójának három stílusa van (az ABC123 minták a menüben látszanak):

| Stílus | Forrás | Licenc / megjegyzés |
|---|---|---|
| Alap | rendszer lekerekített betű | – |
| Modak | Modak betűtípus (Indian Type Foundry), `Modak-Regular.ttf` | SIL Open Font License 1.1, lásd `third_party/Modak-OFL.txt` |
| Gumicukor betűk + szörnyes számok | Freepik: „Hand drawn colorful alphabet” (270) és „Number collection with animals” (291205) | Freepik licenc: **szerzői megjelölés (attribution) szükséges** – „Designed by Freepik”. Tedd ki az áruházi leírásba / névjegyoldalra. A kép-karakterek a mellékelt JPG-kből lettek kivágva (átlátszó háttér). |

## Hol vannak a fájlok
- Android: `android/app/src/main/res/drawable-nodpi/g_a.png … g_z.png, g_0.png … g_9.png`, betűtípus: `res/font/modak_regular.ttf`, kód: `GlyphStyles.kt`
- iOS: `Assets.xcassets/g_*.imageset`, betűtípus: `Assets.xcassets/ModakFont.dataset` (futásidőben töltődik be, nem kell Info.plist-ba írni), kód: `KeypadView.swift`

## Hogyan működik a „nem vágódik el” védelem
- Képeknél minden karakter mérete a doboz arányában van kiszámolva (a legnagyobb is a doboz 92%-a), és a doboz sosem vágja le.
- Modak és a tartalék (pl. ékezetes) betűknél a betű valódi körvonalát mérjük meg, állandó betűmérettel a dobozba illesztjük, középre igazítjuk, és ha bármelyik (pl. Ő, Ű) kilógna, kisebbre vesszük.
- A karakterek sorbeli távolsága a valódi szélességből jön, a sor pedig – mint eddig – a képernyő szélétől 8 dp-re (glow-val együtt) mindig elfér.
- Ha egy karakterhez nincs rajz (pl. emoji, ♥, ★), az eredeti szöveges rajz marad.
