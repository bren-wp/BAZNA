# BAZNA — kontrola svih Android i WordPress zaslona

Datum: 9. studenoga 2026.  Polazište: spojeni PR #214 na glavnoj grani.
Ovaj dokument bilježi rezultate **stvarnih GitHub Actions snimki**, izvorni kod,
ciljane popravke i preostale vizualne provjere. Nije potvrda 0% pixel-diffa.

## Kritičan regresijski propust snimanja (CI run 42706301581)

Iz izvornog Android Actions ZIP-a `SIDRENA-Android-responsive-screenshots`
pregledano je svih devet konfiguracija: small-240, small-320, phone-360,
reference-393, phone-411, tablet-600, tablet-840, landscape-1280,
large-font-320. Svaka konfiguracija sadržavala je 12 PNG datoteka te je
instrumentacijski test prijavio `OK (1 test)`.

Usporedba SHA-256 sadržaja datoteka utvrdila je:

| Konfiguracije | Duplicirani parovi |
|---|---|
| Svih devet | `04-products.png` = `05-services.png` |
| Svih devet | `08-archive.png` = `09-outlets.png` |
| Svih devet | `11-settings.png` = `12-about.png` |
| Samo reference-393 | `02-connect.png` = `03-home.png` |

Ukupno **28 parova** snimki s identičnim datotekama. To upućuje na
pogrešno označavanje ili presnimavanje prerano uhvaćenih kadrova, a ne
na dokaz da dvije implementacije zaslona stvarno imaju isti izgled.
Zato dotadašnje zelene instrumentacijske provjere **ne dokazuju
da je svih 12 namjeravanih zaslona fotografirano**.

### Popravak mjerenja

- `ResponsiveScreensInstrumentedTest.kt`: svaki zaslon ima provjerljiv
  `testTag`, čeka se završetak Compose i UI zadataka te predaja kadra;
  ponavlja se snimanje ako je prethodni kadar potpuno identičan.
  Neuspjeh razlikovanja dvaju uzastopnih zaslona ruši test.
- `run_android_responsive.sh`: nakon preuzimanja svih 12 PNG-ova po
  konfiguraciji izračunava se SHA-256 i odbijaju se **bilo koja** dva
  identična kadra s različitim imenima.
- Potrebno je ponovno pokrenuti cijelu Android testnu matricu na novoj grani
  te pregledati 108 novih snimki. Tek potom smije početi stvarno mjerenje
  odstupanja u odnosu na izvorni referentni skup.

## Android — 12 zaslona, sada pokriveno snimanjem

| ID | Zaslon | Sadašnji status |
|---|---|---|
| 01 | Splash | Poseban full-bleed put, zahtijeva usporediv izvorni PNG |
| 02 | Povezivanje | Jedinstveni ekran mora biti potvrđen stvarnom snimkom |
| 03 | Početna | Test hvatanja prethodnog kadra pojačan |
| 04 | Proizvodi | Test razlikovanja od zaslona 05 |
| 05 | Usluge | Test razlikovanja od zaslona 04 |
| 06 | Detalj stavke | Funkcionalna kartica, referentni pixel-diff tek slijedi |
| 07 | Javni cjenici | Dodirne površine povratka, izbornika i dokumenta povećane na 48dp |
| 08 | Arhiva | Test razlikovanja od zaslona 09 |
| 09 | Poslovnice | Dodana stvarna adresa poslovnice iz javnog WordPress API-ja kada postoji; bez lažnih koordinata/radnog vremena |
| 10 | Pretraga | Test nove snimke i provjera stanja prazne pretrage |
| 11 | Postavke | Test razlikovanja od zaslona 12 |
| 12 | O aplikaciji | Test razlikovanja od zaslona 11 |

## WordPress — 8 admin prikaza

| Kartica | QA fokus |
|---|---|
| Licenca | Polja za domenu i ključ moraju ostati unutar 240px+ viewporta; gumbi imaju najmanje 44px |
| Pregled | Naslov, KPI, kartice i status objave, uključujući dulje poruke |
| Proizvodi i usluge | Uređivanje i horizontalni scroll unutar tablice, bez prelijevanja cijele stranice |
| Postavke | Kontrole i datumi, mobile wrap |
| Uvoz/Izvoz | Polja za mapiranje, pregled datoteke i potvrda uvoza |
| Javni cjenik | HTML/CSV/XML poveznice, status publikacije i Android povezivanje |
| Poslovnice | Unos adrese, oznaka i više lokacija, mobilni editor |
| Upute | Dugi odlomci, linkovi, FAQ i shortcodeovi |

`tools/fixtures/admin-responsive.html` sada sadrži sve osam oznaka kartica
i reprezentativan licencni obrazac te koristi stvarni CSS dodatka.
`tools/test_responsive_browser.mjs` provjerava geometriju licencnih
polja i dodirnih gumbi na 13 širina (240–1920px). **Fixture nije dokaz
potpune podudarnosti stvarnog WordPress prikaza.** Za to treba
instalirani WordPress/WooCommerce, svi statusi licence i referentne slike.

## Interoperabilnost i sigurnost

Novi javni field `outlets[].address` objavljuje samo sanitiziranu
adresu poslovnog objekta iz već javnih podataka cjenika.
Android ga čita kao **opcionalan** string, provjerava tip, duljinu i
kontrolne znakove; nedostaje li polje, zaslon ostaje kompatibilan sa
starim WordPress dodacima.

Bez novog sustava geokodiranja/objave javnog radnog vremena ne
prikazuju se neprovjerene geografske koordinate ni radno vrijeme.

## Kriterij prihvata 1:1

Zelena kompilacija, 108 različitih snimki i odsutnost horizontalnog
prelijevanja *ne* znače 0% vizualnog odstupanja. Stvarna potvrda
zahtijeva za svaki zaslon originalnu referencu, istu rezoluciju i
sustavne postavke, obostrani overlay te izvještaj
`tools/compare_ui_reference.py` s `MAE=0` i
`0%` promijenjenih piksela. Nema tvrdnje 1:1 bez tog dokaza.
