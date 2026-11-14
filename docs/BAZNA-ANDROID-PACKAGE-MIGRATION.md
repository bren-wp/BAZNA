# BAZNA Android 1.5.17 — promjena identifikatora

## Nova Android aplikacija
- `applicationId` i `namespace`: **`com.brendigo.bazna`**
- Kotlin izvorni kod, testovi i instrumentalizirani testovi nalaze se u `com/brendigo/bazna`.
- Novi Android paket ima `versionCode=20`, `versionName=1.5.17`, `targetSdk=36`, `minSdk=26`.
- Prethodni `com.brendigo.sidrena` ostaje **druga** aplikacija. Ne postoji automatsko ažuriranje ni automatska migracija postavki iz nje.
- Ako je prethodna verzija objavljena na Google Playu, za BAZNA je potrebno otvoriti novi unos aplikacije. Staro izdanje nemojte brisati prije odluke o prijelazu korisnika.
- Stari i novi APK mogu biti istodobno instalirani. Ponovno unesite istu HTTPS adresu i izaberite poslovnicu; svi objavljeni podaci ostaju na poslužitelju.
- Bez administratorske autentifikacije, javni BAZNA API ostaje **v1 samo za čitanje**, a PHP/WordPress konfiguracija i povijesne cijene se ne migriraju niti mijenjaju.
- WordPress plugin zadržava internu mapu i javne REST alias putanje radi kompatibilnosti i bez lomljenja postojećih stranica.

## Test i objava
1. Provjeriti Android Gradle build, R8, lint, JUnit i instrumentacijske testove na 9 emulator konfiguracija.
2. Potvrditi da `aapt dump badging` za APK prikazuje paket `com.brendigo.bazna`, verziju **1.5.17** i kod **20**.
3. Testirati povezivanje s pravim WordPress BAZNA javnim API-jem i starim REST podacima.
4. Dostaviti stvarne snimke Google Playu, ikonu, grafiku i novu politiku privatnosti.
5. Potpisati AAB ispravnim upload ključem za novi Play unos. Keystore i lozinke ostaju izvan repozitorija.
6. Ne predstavljati debug APK ni nepotpisani AAB kao produkcijski potpisan paket.
7. Upozoriti postojeće korisnike da nova aplikacija ne može zamijeniti staru kao update.

Referenca: https://developer.android.com/build/configure-app-module
