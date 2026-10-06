# EXIF → CSV

Jednoduchá aplikace pro Android, která z vybraných fotek přečte **název souboru, čas pořízení a GPS souřadnice** uložené v EXIF a uloží je do **CSV souboru**. Nic se nikam neodesílá, vše probíhá v telefonu.

[![Build APK](https://github.com/dospe/exif-csv/actions/workflows/build.yml/badge.svg)](https://github.com/dospe/exif-csv/actions/workflows/build.yml)

## Stažení

- **Nejnovější APK:** <https://github.com/dospe/exif-csv/releases/latest/download/exif-csv.apk>
- Všechny verze: <https://github.com/dospe/exif-csv/releases>

APK sestavuje GitHub Actions automaticky při každém pushi. Každý build má vyšší číslo verze, takže se nová verze nainstaluje přes starou.

### Instalace

1. Stáhněte `exif-csv.apk` v telefonu.
2. Otevřete stažený soubor. Android se zeptá, zda povolit instalaci z tohoto zdroje (prohlížeč / správce souborů). Povolte.
3. Potvrďte instalaci. Vyžaduje Android 8.0 nebo novější.

## Použití

1. Po spuštění klepněte na **Povolit přístup k fotkám**. Na Androidu 14+ můžete povolit buď všechny fotky, nebo jen vybrané.
2. V mřížce galerie klepnutím označte fotky (nebo **Vybrat vše**).
3. Klepněte na **Vytvořit CSV**. Aplikace přečte EXIF z každé fotky a zobrazí přehled.
4. Klepněte na **Uložit CSV** (otevře se systémový dialog „Uložit jako") nebo na **Sdílet** (e-mail, Disk, Zprávy…).

Alternativně lze v menu zvolit **Vybrat soubory ručně…** a vybrat fotky systémovým výběrem souborů, i bez udělení přístupu ke galerii.

## Formát CSV

Oddělovač je **středník**, kódování UTF-8 s BOM (Excel ho otevře rovnou), řádky ukončené CRLF.

```csv
nazev;datum_cas;lat;lon;alt
IMG_20260921_143210.jpg;2026-09-21 14:32:10;50.087500;14.421300;235.4
IMG_20260921_150455.jpg;2026-09-21 15:04:55;;;
```

| Sloupec     | Význam                                                            |
|-------------|-------------------------------------------------------------------|
| `nazev`     | Název souboru fotky                                               |
| `datum_cas` | Čas pořízení z EXIF (`DateTimeOriginal`), formát `RRRR-MM-DD HH:MM:SS` |
| `lat`       | Zeměpisná šířka v desetinných stupních (6 desetinných míst)       |
| `lon`       | Zeměpisná délka v desetinných stupních (6 desetinných míst)       |
| `alt`       | Nadmořská výška v metrech (pokud je v EXIF)                       |

Chybějící údaje zůstávají prázdné. V menu lze zapnout **Desetinná čárka**, pak se čísla zapíší jako `50,087500` (vhodné pro Excel v českém nastavení).

## Oprávnění a GPS

Android od verze 10 **odstraňuje GPS souřadnice** z fotek, které aplikace získá přes systémový výběr fotek (Photo Picker). Proto má aplikace vlastní mřížku galerie a žádá o tato oprávnění:

| Oprávnění                          | Proč                                                         |
|------------------------------------|--------------------------------------------------------------|
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | Zobrazení fotek v galerii (podle verze Androidu)  |
| `READ_MEDIA_VISUAL_USER_SELECTED`  | Android 14+: přístup jen k vybraným fotkám                   |
| `ACCESS_MEDIA_LOCATION`            | Čtení neořezaných GPS údajů z EXIF                           |

Aplikace nemá přístup k internetu.

## Sestavení

Požadavky: JDK 17, Android SDK (compileSdk 36). Potom:

```bash
./gradlew assembleRelease
# výstup: app/build/outputs/apk/release/app-release.apk
./gradlew testReleaseUnitTest   # jednotkové testy
```

### Podpisový klíč

Klíč `keystore/release.jks` (alias `exifcsv`, heslo `exifcsv-public`) je **záměrně uložen v repozitáři**, aby GitHub Actions mohl podepisovat APK a aktualizace šly instalovat přes starší verze. Klíč je tedy veřejný a **nehodí se pro Google Play** ani pro jiné účely. Pokud si projekt forknete, nastavte vlastní klíč přes proměnné prostředí `APP_KEYSTORE_FILE`, `APP_KEYSTORE_PASSWORD`, `APP_KEY_ALIAS`, `APP_KEY_PASSWORD`.

## Technologie

Kotlin, Jetpack Compose (Material 3), AndroidX ExifInterface, MediaStore. Min. Android 8.0 (API 26), cíl Android 16 (API 36).

## Licence

[MIT](LICENSE)
