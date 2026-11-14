# BAZNA 1.5.18 — izdanja i Google Play

## Izdanje
| Platforma | Verzija |
|---|---|
| Android | **1.5.18**, `versionCode 21`, `com.brendigo.bazna` |
| WordPress | **1.5.18**, bez promjena baze i javnog REST API-ja |
| Windows | **0.10.6** |
| Web / Joomla / Shopify | **1.1.2** |

BAZNA 1.5.18 donosi moderniziran Android build, provjere sigurnosti i
responzivnosti te izdvajanje navigacijske logike u zasebni modul.
Android je i dalje nativna aplikacija samo za čitanje javnih HTTPS cjenika.

**Važno:** Android paket `com.brendigo.bazna` je NOVI identifikator.
Google Play NE može ažurirati prethodnu aplikaciju `com.brendigo.sidrena`
ovim AAB-om. Potreban je zaseban Play Console unos za novu aplikaciju.
Obje aplikacije mogu koegzistirati, ali se postavke lokalno ne prenose
automatski. Korisnik može ponovno unijeti postojeću javnu HTTPS domenu.

## Izgradnja, provjere i isporuka

GitHub Actions kompilira Android aplikaciju, izvodi Kotlin/JUnit i lint,
izrađuje debug APK i nepotpisani AAB te računa SHA-256. Instrumentacijski
test provjerava 12 zaslona na 9 emulator konfiguracija. Ova mjerenja
nisu dokaz 1:1 podudarnosti bez izvornih PNG dizajnerskih referenci.

Objedinjeni GitHub release objavljuje:
- `BAZNA-Android-debug.apk` — samo za instalaciju i testiranje
- `BAZNA-Android-unsigned.aab` — potrebno potpisivanje prije Play Consolea
- Windows Portable i Setup, WordPress ZIP, dodatne adapterske ZIP pakete
- SHA-256 i informacije o podrijetlu builda

**Niti debug APK niti nepotpisani AAB nisu Play-ready potpisani proizvodni artefakti.**
Za Google Play potpišite AAB ispravnim novim upload ključem, objavite
odgovarajuću politiku privatnosti, popunite Data safety i sve izjave u
Play Consoleu. Ne unosite keystore ili lozinke u Git.

[Detaljna provjera Google Playa](../docs/ANDROID-GOOGLE-PLAY-CHECKLIST.md)
 · [Migracija identifikatora](../docs/BAZNA-ANDROID-PACKAGE-MIGRATION.md)
 · [GitHub izdanja](https://github.com/bren-wp/BAZNA/releases)
