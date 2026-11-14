# BAZNA 1.5.18 — pravna i Google Play provjera (14. 11. 2026.)

## Normativna granica

**Važeći objavljeni izvori:**
- NN 101/2026, odluka 1212: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html
- NN 101/2026, odluka 1213: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html
- NN 110/2026, odluke 1309 i 1310: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html i https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html
- NN 59/2026, Zakon o izmjenama i dopunama Zakona o zaštiti potrošača: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html
- NN 105/2026, maloprodajna cijena i cijena za jedinicu mjere: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html

Prema NN 101/2026 izmijenjenoj NN 110/2026, datum početka proširenih
obveza je **17. 11. 2026.** Za ranije obuhvaćene proizvode referentni
datum je **2. 5. 2025.**, a za ostale proizvode i usluge **10. 9. 2026.**
Prodajni objekt / pružatelj usluge mora potvrditi točnu kategoriju i
povijesnu cijenu vlastitom evidencijom; današnja cijena ili najniža cijena
u prethodnih 30 dana ne smiju se koristiti kao zamjena.

Prema objavljenoj odluci, javni strojno čitljivi CSV/XML cjenici proizvoda
objavljuju se najmanje jednom dnevno do **08:00** hrvatskoga vremena
za tekući radni dan; usluge se objavljuju pri promjeni prema propisanom
roku; arhiva je dostupna najmanje **30 dana**. Ovo je opis propisane
obveze, a **nije dokaz da neki WordPress poslužitelj ostvaruje rok**:
WP-Cron sam po sebi nije jamstvo pravovremenog izvršavanja.

Dva posebna pravilnika o baznoj cijeni i načinu objave cjenika bila su
u e-Savjetovanju zaključno s **13. 11. 2026.** Na dan ove provjere
nisu potvrđeni objavljeni konačni tekstovi u Narodnim novinama. Zato
`base_price_regulation_status=consultation_closed_pending_final_text`
**nije** oznaka njihove pravne snage. Prije produkcijskog pravnog
certificiranja ponovno provjeriti NN, iznimke i prijelazne odredbe.

## Android ↔ WordPress ugovor

WordPress ostaje **izvor istine**: isključivo javno objavljeni
`GET /wp-json/brendigo-bazna/v1/cjenici`, CSV datoteke i arhiva.
Kompatibilni `brendigo-sidrena/v1` REST alias ostaje radi starijih klijenata.
U API-ju v1 ostaju `feeds.proizvodi/usluge.available_csv`,
`outlets[].feeds.*.available_csv` i `publication.pending_changes`.
Novo, neobvezno polje `publication.csv_ready_all_outlets` označava
**jesu li sve definirane CSV datoteke objavljene**; ne potvrđuje da je
trgovac zakonski usklađen niti implicira da se bavi svim vrstama roba/usluga.

Android 1.5.18 koristi per-outlet status, ne prikazuje URL bez
stvarnog objavljenog CSV-a i neće izmišljati arhivske podatke.
Stariji REST v1 koji ne sadrži polja spremnosti ostaje podržan uz
**nepoznat** status (nije automatski označen neobjavljenim).
Android pristupa samo istom HTTPS izvoru, bez korisničkih računa,
WordPress administratorove lozinke ili API-ja za izmjene.

## Google Play pregled

- Identifikator: `com.brendigo.bazna`; `versionCode=21`;
  `versionName=1.5.18`; `minSdk=26`, `targetSdk=36`.
- Tražena Android dozvola: samo `INTERNET`. Potvrditi svaki stvarni
  dependency / SDK i prijenose podataka prije označavanja Data safety.
- Politika privatnosti mora biti javna HTML stranica s HTTPS-om,
  bez prijave i ograničenja, te sadržajno pokrivati baš BAZNA Android.
  https://brendigo.com/politika-privatnosti/ postoji, ali opći tekst
  treba dopuniti BAZNA-specifičnom obradom i mogućim mrežnim zapisima.
- URL za privatnost dostupan je u Android odjeljku O aplikaciji.
  Android prikazuje i obavijest da odabrani javni poslužitelj može
  evidentirati IP adresu i vrijeme HTTPS zahtjeva.
- Popuniti obveznu Data safety izjavu prema stvarnom ponašanju,
  izjave o sadržaju/publiki, privacy link i identitet razvojnog računa.
- Potpisati AAB ispravnim upload ključem, bez dodavanja tajni u Git.
  Debug APK i unsigned AAB iz GitHub izdanja nisu Play-signed.
- Testirati Google Play pre-launch izvještaj, Android 26–37,
  promjene fontScale, rotaciju, talkback, offline i nepouzdan API.

**Ni uspješan CI ni ovaj dokument ne znače pravno odobrenje ili
odobrenje Google Playa.**

Službene smjernice:
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/9859455
- https://support.google.com/googleplay/android-developer/answer/10787469
