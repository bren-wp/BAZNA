# BAZNA — kompletni Google Play brending (Android)

Službeni Android identifikator: `com.brendigo.bazna`. Dizajn: tamnoplava `#10243A`, narančasta `#FF852C`, bijela tipografija, postojeći vektorski znak B iz `android/app/src/main/res/drawable/ic_launcher_foreground.xml`.

## Gotovi formati

| Stavka | Format | Sadržaj |
|---|---|---|
| Google Play ikona | PNG 512 × 512, RGBA, maksimalno 1024 KB | `icon/BAZNA-icon-512.png` |
| Feature graphic | PNG RGB 1024 × 500 (bez alpha kanala) | `feature/BAZNA-feature-1024x500.png` |
| Telefonske snimke | 8 PNG RGB, 1080 × 1920, 9:16 | `phone-upload/` |
| Svi Android zasloni | 12 PNG RGB, 1080 × 1920, samo za odabir/dokumentaciju | `all-screens/` |
| Tablet 7 inča | 4 PNG RGB, 1080 × 1920 | `tablet-7inch/` |
| Tablet 10 inča | 4 PNG RGB, 1920 × 1080 | `tablet-10inch/` |
| Adaptive launcher | foreground i monochrome RGBA 432 × 432, background RGB 432 × 432 | `launcher/` |

Google Play dopušta **najviše 8 snimki po kategoriji uređaja**. Za telefone se preporučuju najmanje četiri kvalitetne snimke visoke razlučivosti. Prije prijenosa provjeriti i stvarna ograničenja veličine datoteka prikazana u Play Consoleu.

Snimke **nisu izmišljene slike Android sučelja**: izvor su stvarni Jetpack Compose emulator capture-i iz `BAZNA-Android-responsive-screenshots` artefakta, uključujući profile `reference-393`, `tablet-600` i `landscape-1280`. Marketinške pozadine i naslovi dodaju se oko stvarne snimke. Snimke s testnim podacima jasno su označene kao primjer; ne prikazivati da je testni katalog zadani sadržaj svakog korisnika.

## Kako preuzeti novi paket nakon idućeg uspješnog Android CI-a

1. Otvoriti **GitHub → Actions → BAZNA Google Play branding assets**.
2. Nakon što `BAZNA Android build, tests and audited artifacts` prođe na `main`, automatski se izrađuje novi Play branding artefakt za isti commit.
3. Preuzeti `BAZNA-Google-Play-Branded-Assets` i u njemu ZIP `BAZNA-Google-Play-<commit>.zip` i SHA-256 kontrolne zbrojeve.
4. Po potrebi kroz **Run workflow** ručno obnoviti paket, ali samo ako postoji uspješna Android emulator snimka za trenutačni `main` commit. CI odbija zastarjele prikaze.
5. Lokalna reprodukcija: `python -m pip install pillow==11.3.0 cairosvg==2.8.2` i zatim `python tools/build_play_store_assets.py --screenshots-dir artifacts/android-responsive --output artifacts/google-play/BAZNA-Google-Play`.

## Redoslijed osam telefonskih snimki

1. Početna — svi javni cjenici na jednom mjestu.
2. Proizvodi — aktualne objavljene cijene.
3. Detalji proizvoda — bazna cijena i referentni datum.
4. Usluge — objavljeni katalog usluga.
5. Javni cjenici — datoteke i preuzimanja.
6. Arhiva — prethodno objavljeni cjenici.
7. Poslovnice — odabir poslovnice/webshopa.
8. Povezivanje — HTTPS adresa korisnikove stranice.

Puni skup dodatno uključuje Splash, Pretragu, Postavke i O aplikaciji. Tablet ilustracije izrađene su iz **stvarnog različitog emulator viewporta**, a nisu samo iste telefonske slike preimenovane u tablet.

## Unos u Play Console

- **Store presence → Main store listing → Graphics**: postaviti ikonu, feature graphic, osam telefonskih te četiri 7-inčna i četiri 10-inčna prikaza.
- **Store listing details**: koristiti `docs/BAZNA-GOOGLE-PLAY-STORE-LISTING-HR.txt` za hrvatski naslov, kratki i puni opis. Tekst je namjerno usklađen sa stvarnim **read-only** mogućnostima.
- **App content**: provjeriti Data safety, Content rating, ciljnu publiku, politike privatnosti, Data deletion primjenjivost, kontakt i potrebne deklaracije.
- **App signing**: potpisati release Android App Bundle valjanim Google Play upload ključem; CI-jev **unsigned AAB** nije gotov Play upload.
- **Privacy policy**: provjeriti da je stvarno javno dostupna, točna i **BAZNA-specifična**, ne samo generička politika stranice. Aplikacija lokalno pamti URL odabrane stranice; odabrani HTTPS poslužitelj može bilježiti IP/vrijeme zahtjeva.
- Ako je objavljena nova verzija sučelja, ponovno snimiti stvarne zaslone za nju i izraditi nove marketing materijale.

## Izvori zahtjeva

- [Google Play — Add preview assets](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Google Play — Best practices for store listing](https://support.google.com/googleplay/android-developer/answer/13393723)
- [Google Play — Metadata policies](https://support.google.com/googleplay/android-developer/answer/9898842)

Ovaj paket izrađuje grafiku i tekst, **ne šalje automatski aplikaciju na Google Play i ne potvrđuje odobrenje Google Playa**. Zadnji pregled zahtjeva: 14. studenoga 2026.
