# SIDRENA Android — vizualni standard

Android zasloni koriste jedinstveni SIDRENA dizajn usklađen s dostavljenih deset referentnih prikaza.

## Dizajnerski sustav

- **Pozadina:** `#07182D` — tamnoplava.
- **Kartice:** `#10263D`, povišene površine `#19304A`.
- **Obrubi:** `#28455F`, suptilni i ujednačeni.
- **Primarna akcija:** `#FF852C` — Brendigo narančasta.
- **Tekst:** bijela; pomoćni tekst `#A9BAD1`.
- **Status:** `#52D9A3` za uspješnu povezanost, narančasta za upozorenja.
- Zaobljeni paneli (14–18 dp), razmak 8–16 dp, čitljivi hrvatski natpisi, uredni statusni prikazi i velika dodirna područja.

## Navigacija i zasloni

| Referenca | Android prikaz |
|---|---|
| 1. Splash | SIDRENA logo, slogan i animirana traka |
| 2. Povezivanje | HTTPS unos i povezivanje s WordPressom |
| 3. Početna | Povezana stranica, statistike, objave i brze kartice |
| 4. Proizvodi | Pretraga, filtri sidrenih cijena i popis javnih artikala |
| 5. Usluge | Popis usluga, pretraga, cijene |
| 6. Detalj stavke | Naziv, SKU, cijene, datum i dostupnost |
| 7. Cjenici | Javni cjenici |
| 8. Arhiva | CSV/XML filter i objavljene datoteke |
| 9. Poslovnice | Odabir webshopa i javnih poslovnica |
| 10. Pretraživanje | Proizvodi i usluge s pretragom |
| 11. Postavke | HTTPS stranica, osvježavanje i prikaz |
| 12. O aplikaciji | Verzija, upute i podrška |


Podnožje s četiri glavne kartice: **Početna, Cjenici, Arhiva, Postavke**. Trenutačno aktivna kartica naglašena je narančastom.

Interakcije ovise o stvarnoj dostupnosti server-side funkcionalnosti. Vizualne reference prikazuju i uređivanje, obavijesti, slike proizvoda i povijest promjena; javni WordPress REST API trenutno ne daje sve potrebne autorizirane podatke za te mogućnosti, pa se one ne smiju lažno simulirati. Dizajnerski cilj je što bliže podudaranje, uz stvarne izvore podataka i funkcionalne radnje.

## Zaslon 04 — Proizvodi (PR #201)

Referentni četvrti telefon: zaglavlje s povratkom i izbornikom, polje pretrage, jedan red kratkih filtera, tamne kartice s lijevom svijetlom pločicom, nazivom i jedinicom te narančastom aktualnom cijenom i sivom referentnom cijenom. Implementacija: `ui/ProductCatalogScreen.kt`.

| Element | Cilj |
|---|---|
| Zaglavlje | 61 dp; naslov Proizvodi 22 sp, povratak i izbornik min. 48 dp |
| Tražilica | 52 dp, `#19304A`, zaobljenje 12 dp |
| Filtri | 43 dp, razmak 8 dp, narančasti aktivni čip |
| Proizvod | min. 92 dp, `#10263D`, radius 14 dp |
| Ikona proizvoda | 64×72 dp, lokalna neutralna ilustracija bez vanjske usluge |
| Naziv | 15 sp bold, najviše dva retka |
| Cijena | 17 sp bold, `#FF852C`; referenca 11 sp `#A9BAD1` |
| Podaci | Stvarni `CatalogSnapshot.products`, `CatalogSearch.items()`, `PriceDisplay.euro()` |

**API ograničenja:** Javne WordPress rute ne pružaju slike proizvoda, kategorije, oznake `Novo` niti promocije. Zato prikaz koristi neutralan lokalni piktogram, a filteri su **Svi**, **Sidrena** i **Bez sidrene**; gumb **Usluge** otvara postojeći zaslon usluga. Ne smije se označiti sidrena cijena kao popust ili stvoriti lažni podatak.

**QA:** Android CI (assembleDebug, bundleRelease, testDebugUnitTest, lintDebug, sigurnosni audit), test otvaranja detalja, povratka, učitavanja, tipkovnice i rubnih podataka (prazna lista, dugačka imena, bez cijene). Vizualna tvrdnja pixel-perfect 1:1 zahtijeva screenshot s emulatora i overlay usporedbu na istoj rezoluciji kao referenca, uz zasebnu provjeru pristupačnosti.


## Zaslon 05 — Usluge (PR #201)

**Glavna referenca:** peti telefon na korisnički potvrđenom marketing/branding prikazu SIDRENA. Rezervna referenca `05_usluge.png` iz paketa brendinga prikazuje jednostavniji popis i služi samo za usporedbu boja, proporcija i elemenata. Oznake `DEMO` i `KONCEPT` nisu dio stvarnog sučelja.

Implementirano u `ui/ServiceCatalogScreen.kt`, integrirano kroz `SidrenaApp.kt`, s testovima u `ui/ServiceCatalogPresentationTest.kt`.

| Element | Smjernica |
|---|---|
| Zaglavlje | 61 dp, bijeli naslov Usluge 22 sp, povratak i izbornik 48 dp |
| Pretraga | 52 dp, tamnoplavo izdignuto polje, zaobljenje 12 dp |
| Filteri | 43 dp, razmak 8 dp, aktivni filtar narančast, ostali tamnoplavi |
| Redak usluge | min. visina 101 dp, kartica `#10263D`, 1 dp border `#28455F`, radius 14 dp |
| Piktogram | 76 × 82 dp, lokalni `Build` simbol u `#FF852C` |
| Naziv | 15 sp bold, dva retka, uredno skraćivanje |
| Aktualna cijena | 17 sp, narančasta, format na hrvatskom |
| Referentna cijena | 12 sp u `#A9BAD1`, samo ako je javno objavljena |
| Scroll | LazyColumn i pomični filteri, prilagodba tipkovnici |

**Povezani podaci:** prikaz koristi `CatalogSnapshot.services`, `CatalogSearch.items()` i `PriceDisplay.euro()` (pravi cjenici WordPressa). Nema lažnih primjera u produkcijskom prikazu, a pregledni testni podaci nalaze se isključivo u `@Preview`.

**API ograničenja:** WordPress javni CSV nema fotografije, kategorije ni podatke o popularnosti usluga. Umjesto lažnih foto-thumbnaila koristi se lokalni simbol; filteri **Svi / Sidrena / Bez sidrene** rade na stvarnim referentnim cijenama, a **Proizvodi** je funkcionalni navigacijski prečac. Cijene se ne označavaju kao „od X €” bez server-side podatka da je to početna/minimalna cijena.

**Funkcionalni testovi:** pretraga hrvatskih znakova i SKU-a, kombiniranje filtara, prazna lista, neočekivano prazna cijena, ulazak u detalje i povratak na usluge, promjena na proizvode, osvježavanje, prikaz mrežne greške, test velikog fonta i različitih širina. Gradle CI provjerava Android build, unit testove, lint te APK/AAB.

**Vizualna potvrda:** preciznost 1:1 potrebno je objektivno provjeriti usporedbom screenshotova emulatora i izvornog boarda na istim dimenzijama te dodatnom provjerom za različite DPI gustoće. Ova promjena sama po sebi ne predstavlja potvrđen screenshot pixel-diff.


## Zaslon 06 — Detalj artikla (Compose)

**Vizualna referenca:** šesti telefon izvornog SIDRENA boarda. Redoslijed: zaglavlje s povratkom, veliki hero prostor za artikl, naziv i jedinica, istaknuta narančasta trenutačna cijena, bijela sidrena cijena i datum, informacije o artiklu te javni cjenik.

Implementacija: `ui/CatalogScreens.kt` (`DetailPage`), integracija `SidrenaApp.kt`, testovi `ui/ItemDetailPresentationTest.kt`.

| Dio | Mjere i ponašanje |
|---|---|
| Zaglavlje | 57 dp, naslov 21 sp, povratak 48 dp; dodatni izbornik samo kad postoji javni cjenik |
| Hero | 213 dp, navy gradient, radius 17 dp, narančasti lokalni simbol (nema izmišljenih fotografija) |
| Naziv | 23 sp ExtraBold, razmak 4 dp prema jedinici |
| Trenutačna cijena | 29 sp ExtraBold, #FF852C, samo ako je dostupna |
| Sidrena cijena | 18 sp SemiBold, bijela; datum prikazati samo ako postoji sidrena cijena |
| Sekundarni tekst | 12–13 sp, #A9BAD1 |
| Detalji | tamnoplava kartica #10263D, border #28455F, radius 15 dp |
| Raspored | vertikalni LazyColumn, radi na kompaktnim zaslonima i pri promjeni veličine fonta |

**Važno za ispravnost podataka:** `CatalogSnapshot` i `PriceItem` trenutno **ne sadrže** fotografiju proizvoda, kategoriju, opis niti datum kraja razdoblja primjene. Nijedan od tih podataka ne smije se izmišljati ili ispisivati kao da je iz javnog API-ja. Umjesto fotografije koristi se lokalni piktogram s natpisom „Fotografija nije objavljena”. Podatak o dostupnosti ispisuje se točno kako je objavljen: zelena se koristi samo za izričitu vrijednost `Dostupno`, a ne za npr. `Nedostupno`.

**Interakcije:** povratak na stvarni prethodni popis, otvaranje javnog HTTPS cjenika kroz postojeći `SiteUrl.publicLink()` sloj, pohrana devet javnih tekstualnih polja odabrane stavke u `rememberSaveable` kako rotacija ne bi izgubila detalj.

**QA:** `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `bundleRelease`, audit izvornog koda; JUnit testovi za opise cijena, datume, nedostupne podatke i vraćanje odabira. Nakon builda slijedi ručni prikaz na emulatoru 393×852 i 360×640 dp, 100% i 150% povećanje fonta te screenshot overlay prema originalnoj slici. Sama kompilacija ne potvrđuje pixel-perfect 1:1 rezultat.


## Zaslon 07 — Cjenici (PR #202)

**Vizualna referenca:** sedmi telefon korisnikove SIDRENA ploče. Posebno zaglavlje s povratkom i izbornikom, tražilica, filtri `Svi / Proizvodi / Usluge / Ostalo`, kompaktni redovi dokumenata s formatom i radnjom otvaranja. Zadržane su tamnoplava i narančasta paleta, bijeli naslovi i suptilni obrubi.

| Element | Vizualni cilj |
|---|---|
| Zaglavlje | 61 dp, naslov 22 sp, povratak i izbornik 48 dp |
| Tražilica | 52 dp, `#19304A`, radius 12 dp |
| Filtar | 43 dp, razmak 8 dp; narančast samo odabrani |
| Redak dokumenta | najmanje 80 dp, `#10263D`, radius 14 dp |
| Ikona dokumenta | 44×44 dp, lokalna: CSV zelena, XML narančasta, WEB plava |
| Naziv/metadata | naziv 14 sp Bold, format i status 12 sp, datum 11 sp |
| Akcija | `OpenInNew` (48 dp dodirna površina); otvara URL, ne nameće preuzimanje |

**Stvarni izvori:** `index.productsCsv` / `index.servicesCsv` (ili CSV cjenici odabrane poslovnice), `index.publicPage` kao WEB dokument i stvarni `archive` CSV/XML zapisi. Arhivski zapisi izričito su označeni **Arhiva**; ne prikazuju se lažni PDF-ovi, veličine datoteka, nazivi ili datumi. Deduplikacija ide po provjerenoj URL adresi. Filtar `Ostalo` prikazuje javnu web stranicu cjenika ili neklasificiranu arhivu; ne zaključuje vrstu iz datotečnog imena.

**Sigurnost:** svi linkovi prije prikaza prolaze `SiteUrl.publicLink()` (ista HTTPS domena), a prilikom otvaranja još jednom prolaze zaštitu u `SidrenaApp.openPublic()`. Odabir poslovnice određuje izvore CSV cjenika; nepoznata poslovnica ne uzrokuje tihu zamjenu webshopom.

**Navigacija:** `Početna → Cjenici`, donja stavka `Cjenici`, unutar zaslona `Natrag`, `Arhiva cjenika` i `Osvježi cjenike`. Druga i treća funkcija koriste postojeću logiku, bez posebnih HTTP klijenata.

**Provjera:** `PublicPriceListsScreenTest.kt` (javni dokumenti, pretraga, filtri, poslovnice, duplikati, same-origin zaštita). CI: Kotlin compile, JUnit, Android Lint, sigurnosni audit, APK/AAB. Pixel-perfect potvrda zahtijeva snimku emulatora i overlay s referentnim dizajnom na istim dimenzijama, nakon čega se mjere preostala odstupanja. **Bez snimke još ne postoji potvrda vizualnog rezultata 1:1.**


## Zaslon 08 — Arhiva (isti PR #202)

**Glavna referenca:** osmi telefon izvornog SIDRENA brendinga. Raspored u Kotlin/Jetpack Composeu: zaglavlje s povratkom i kontekstnim izbornikom, pretraživanje, čipovi godina, status filtra te lista arhivskih dokumenata sa zasebnim zelenim CSV i narančastim XML ikonama.

| Element | Mjera / pravilo |
|---|---|
| Zaglavlje | 61 dp, naslov 22 sp Bold, povratak i izbornik 48 dp |
| Tražilica | 52 dp, `#19304A`, zaobljenje 12 dp; koristi postojeću komponentu |
| Godine | 43 dp čip, razmak 8 dp, odabrani `#FF852C`; godine su samo one stvarno objavljene |
| Popis | kartice najmanje 79 dp, `#10263D`, radius 14 dp, obrub `#28455F` |
| Ikona dokumenta | 43 × 43 dp, CSV zeleno `#52D9A3`, XML narančasto `#FF852C` |
| Sadržaj | naslov 14 sp, format 12 sp, datum 11 sp; bez izmišljenih veličina datoteka |
| Akcija | `OpenInNew` 48 dp za otvaranje dokumenta, bez prisilnog preuzimanja |
| Scroll | godine horizontalno, stavke `LazyColumn`; prilagodba kraćim zaslonima |

### Stvarna povezanost sa WordPressom

- `CatalogSnapshot.archive` već je filtriran za odabranu poslovnicu na backend/sync sloju; ekran ne dodaje fiktivne datoteke ili druge poslovnice.
- Prikazuje isključivo stvarne, objavljene **CSV/XML** poveznice s iste HTTPS domene (`SiteUrl.publicLink()`) i uklanja duplikate URL-ova.
- Godine nastaju iz provjerljivih datuma u ISO ili hrvatskom zapisu; nepoznati datumi ostaju u filtru **Sve**, bez izmišljene godine ili mjeseca.
- Naziv `Cjenik proizvoda/usluga — mjesec godina` izvodi se iz stvarno objavljenih `kind` i `date`. Nepoznati tip se prikazuje neutralno kao `Cjenik`.
- Pretraga radi s više riječi i kombinira se s godinom i CSV/XML filtrom iz izbornika. Moguće je otvoriti aktualne cjenike i osvježiti podatke.
- Otvaranje se provodi postojećim `SidrenaApp.openPublic()` (ponovna same-origin HTTPS provjera). Bez prisilnog preuzimanja CSV/XML.
- Arhiva se ne prikazuje kao kompletna ako je server-sko čitanje neuspješno: prikazuje se postojeće upozorenje `Arhiva trenutačno nije dostupna`.

### Integracija i provjera

- `ui/ArchiveScreen.kt` — novi nativni zaslon, Preview 393 × 852 dp.
- `ui/ArchiveAndSearch.kt` — uklonjen samo zastarjeli `ArchivePage`, Poslovnice i Pretraga ostaju sačuvani.
- `ui/SidrenaApp.kt` — povezana donja navigacija, povratak na početnu, izbornik aktualnih cjenika i osvježavanje, bez dvostrukog zaglavlja.
- `ui/ArchiveScreenPresentationTest.kt` — sedam JVM testova datuma, godina, filtriranja, formata, URL sigurnosti, duplikata i prazne arhive.
- CI: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `bundleRelease`, sigurnosni i dead-code audit. Potrebna je i ručna pixel-diff usporedba stvarnog emulatora s korisničkom referencom; prolazak Gradlea nije dokaz preciznosti 1:1.


## Zaslon 09 — Poslovnice (isti PR #202)

**Referenca:** deveti telefon SIDRENA branding boarda. Zaslon slijedi kompoziciju: zaglavlje 61 dp, pretraga 52 dp, tamnoplavi informativni blok 168 dp u prostoru karte, lista poslovnica i jasna akcija za pregled pripadajućih cjenika.

| Dio | Pravilo |
|---|---|
| Pozadina | `#07182D`, tamne kartice `#10263D`, izdignuta površina `#19304A` |
| Zaglavlje | 61 dp, bijeli naslov 22 sp, akcije povratak i izbornik po 48 dp |
| Pretraživanje | 52 dp, radius 12 dp, pretraga stvarnih naziva/šifri poslovnica |
| Područje karte | 168 dp, statički geometrijski uzorak s oznakom **Karta nije dostupna** |
| Poslovnice | min. 92 dp, radius 14 dp, obrub 1 dp `#28455F`, ikona 48 dp |
| Odabir | `Odaberi` učitava cijene poslovnice; aktivna kartica `Cjenici` otvara njezine cjenike |
| Scroll | `LazyColumn` sa stabilnim kodovima, prilagodba tipkovnici putem `imePadding` |

**Ograničenje javnog API-ja:** `Outlet` sadrži samo `name`, `code`, `productCsv` i `serviceCsv`. Ne sadrži GPS koordinate, adresu, radno vrijeme niti telefonski broj. Stoga zaslon ne crta izmišljene karte s pinovima i ne prikazuje fiktivne adrese ili udaljenosti. Uzorak u panelu je dekorativan, ne geografski prikaz. Kartografsku funkciju moguće je implementirati tek nakon dokumentiranog proširenja javnog WordPress API-ja stvarnim lokacijskim podacima i zasebne validacije.

**Stvarna funkcionalnost:** `publishedLocations()` prikazuje webshop kao zaseban kanal, uz stvarne poslovnice iz `CatalogIndex.outlets`; ukupan broj fizičkih poslovnica ne uključuje webshop. `filterPublishedLocations()` podržava više pojmova, velika/mala slova, hrvatske znakove i pretragu po šifri. `SidrenaViewModel.selectOutlet()` ostaje jedini put za promjenu izvora; spinner i greška vidljivi su na istom zaslonu. Uspješan odabir omogućuje gumb **Cjenici** koji otvara javne cjenike odabrane poslovnice. Prije otvaranja stranice sve URL adrese prolaze postojeću `SiteUrl.publicLink()` zaštitu.

**Integracija:** `ui/LocationsScreen.kt` je nova implementacija, stari `LocationsPage/OutletCard` uklonjeni su iz `ArchiveAndSearch.kt`; `SearchPage` je ostao funkcionalno nepromijenjen. U `SidrenaApp.kt` uklonjeno je dvostruko zaglavlje i povezani su povratak, osvježavanje, odabir poslovnice, otvaranje cjenika. `LocationsScreenPresentationTest.kt` sadrži šest JVM testova.

**Obavezna provjera prije 1:1 potvrde:** Kotlin build, testovi, Android Lint, audit, APK/AAB; emulator 393×852 i 360×640 dp, skaliranje teksta 100% i 150%, pretraga dugog naziva, prazna lista, spora i neuspjela mreža, odabir različitih poslovnica, screenshot overlay s referencom. Bez izmjerene usporedbe ne tvrditi da je zaslon pixel-perfect 1:1.


## Zaslon 10 — Pretraživanje (isti PR #202)

Nativni `ui/SearchScreen.kt` zamjenjuje zastarjeli `ArchiveAndSearch.kt`. Koristi iste zajedničke UI komponente kao Proizvodi, Usluge i Cjenici: tamnoplava SIDRENA podloga, vlastiti toolbar, pretraga, narančasti aktivni filtar, kartice stavki i donja navigacija.

| Element | Mjera i ponašanje |
|---|---|
| Zaglavlje | 61 dp, bijeli naslov 22 sp Bold, povratak i izbornik po 48 dp |
| Tražilica | 52 dp, `#19304A`, radius 12 dp, tipkovnica `ImeAction.Search` |
| Filtri | 43 dp, razmak 8 dp: Sve / Proizvodi / Usluge; Arhiva je zaseban navigacijski prečac |
| Rezultati | najmanje 91 dp, kartica `#10263D`, radius 14 dp, obrub `#28455F` |
| Ikona | 59 dp, narančasti proizvod ili plava usluga; bez izmišljene fotografije |
| Cijena | 16 sp ExtraBold `#FF852C`; sidrena 11 sp, samo ako je objavljena |
| Layout | `LazyColumn`, `imePadding`, skraćivanje dugih naziva, prazna stanja |
| Navigacija | detalj proizvoda/usluge i povratak na prethodni zaslon, sa sačuvanim upitom i filtrom |

### Integracija i izvor podataka

- `searchCatalogEntries(products, services, query, kind)` filtrira **oba stvarna javna CSV kataloga**, bez dvojnog preuzimanja, server-side pretraživanja ili dodatnih vanjskih biblioteka. Zadržava objavljeni redoslijed unutar svakog izvora.
- Filtar `Sve` pretražuje proizvode i usluge; zasebni filtri biraju vrstu prema stvarnoj listi izvora, a ne nepouzdanim tekstualnim kategorijama.
- Postojeći `CatalogSearch.items()` omogućuje više pojmova te pretragu naziva, SKU-a, ID-a, dostupnosti i vrste bez pretvaranja sidrene cijene u popust.
- U rezultatima se prikazuju isključivo stvarne trenutačne i sidrene cijene; prazna cijena jasno je označena. Nema demo podatka u produkciji.
- `SidrenaApp.kt` upravlja `rememberSaveable` upitom, vrstom filtra i prethodnom stranicom. Nakon otvaranja detalja i povratka pretraga ostaje očuvana. Android sustavna i gornja tipka za povratak vode na isti prethodni zaslon.
- `SearchPage` prikazuje stvarno učitavanje i API grešku; kontekstni izbornik omogućuje osvježavanje i odlazak u Arhivu.
- `SearchScreenPresentationTest.kt` sadrži šest JVM testova za kombiniranu pretragu, vrstu, hrvatske znakove, više pojmova, prazne rezultate i cijene.

### Provjera prihvatljivosti

Provjeriti `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `bundleRelease`, audit i APK/AAB artefakte. Ručno provjeriti zaslone 393×852 dp i 360×640 dp, tipkovnicu, male uređaje, 150% veličinu fonta, mnoštvo rezultata, pretragu s hrvatskim znakovima, povratak iz detalja i `Arhiva` prečac. **Pixel-perfect 1:1 nije potvrđen** bez usporedbe screenshota emulatora i izvornog brending prikaza te korekcije izmjerenih odstupanja.


## Zaslon 11 — Postavke (isti PR #202)

**Dizajnerska referenca:** SIDRENA zaslon postavki, s tamnoplavom pozadinom, narančastim akcijama, grupiranim karticama i donjom navigacijom. Implementacija `ui/SettingsScreen.kt`; zadržan `AboutPage` u `ui/SettingsScreens.kt`.

| Dio | Vizualni cilj |
|---|---|
| Zaglavlje | 61 dp, bijeli naslov 22 sp Bold, ikona povratka s dodirnom površinom 48 dp |
| Pozadina | `#07182D`, kartice `#10263D`, izdignute ikonice `#19304A` |
| Kartice | radius 16 dp, obrub 1 dp `#28455F`, unutarnje margine 14–16 dp |
| Povezana stranica | URL sa skraćivanjem dugih domena, status `#52D9A3`, datum zadnje objave |
| Primarna akcija | narančasti gumb Osvježi, 48 dp; promjena URL-a obrisani sekundarni gumb |
| Sekcije | POSLOVNICA I CJENICI, PRIKAZ, INFORMACIJE I PODRŠKA |
| Stavke | visina 76 dp, ikona 43 dp, naslov 14 sp, opis 12 sp, desna strelica samo za stvarne akcije |
| Odspajanje | zasebna akcija 48 dp; postojeći potvrdni dijalog prije brisanja lokalne HTTPS adrese |
| Raspored | `verticalScroll`, prilagodba kraćim ekranima i povećanom fontu |

### Integracija i sigurnost

- `SettingsSummary` prikazuje samo podatke iz `CatalogSnapshot`: HTTPS adresu, zadnju objavu, naziv stvarne odabrane poslovnice, verziju dodatka i reviziju pravnog profila. Nedostajući metapodaci označeni su kao nedostupni, bez izmišljanja datuma ili verzije.
- Osvježi, Promijeni URL, Odspoji stranicu, Poslovnica, Pomoć i O aplikaciji povezani su sa stvarnim `SidrenaViewModel` funkcijama i postojećom navigacijom. Dok traje osvježavanje, radnje koje mijenjaju vezu onemogućene su.
- `Tema aplikacije: Tamna tema` i `Jezik aplikacije: Hrvatski` su **informativne** (aplikacija trenutačno nema sustav promjene teme ili jezika). Ne prikazuju se lažni prekidači ni klikabilni redovi.
- `Odspoji stranicu` koristi već postojeći potvrdni `AlertDialog`; lokalna postavka se uklanja bez brisanja sadržaja WordPress stranice.
- Povezana poslovnica prepoznaje se prema točnom javnom `Outlet.code`; nepoznat kod ne smije se prikazati kao webshop.
- `ui/SettingsScreens.kt` nakon izdvajanja sadrži samo `AboutPage`, bez duplicirane implementacije postavki i nepotrebnih uvoza.
- `SettingsScreenPresentationTest.kt` provjerava šest scenarija uključujući prazne i nedostupne metapodatke.

### QA i pixel-perfect kriteriji

Android CI mora proći Kotlin kompilaciju, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `bundleRelease`, source security/dead-code audit i verifikaciju artefakata. Na emulatoru dodatno usporediti screenshot pri 393×852 dp i 360×640 dp, provjeriti 150% font scaling, duge nazive domena, grešku mreže, ponovno povezivanje i odspajanje. **Dok se ne napravi overlay sa stvarnim emulatorom i referencom, 1:1 pixel-perfect nije potvrđen.**


## Zaslon 12 — O aplikaciji (isti PR #202)

**Stanje:** posljednji zaslon planiranog 12-zaslonskog Android prolaza; nativni `ui/AboutScreen.kt` s postojećim lokalnim drawable logotipom. Novi slikovni resursi nisu generirani. `SettingsScreens.kt` uklonjen je jer je jedini preostali `AboutPage` izdvojen u novi zaslon.

### Vizualna kompozicija

| Element | Mjera i dizajn |
|---|---|
| Pozadina | tamna `#07182D` uz `#10263D` kartice i `#28455F` obrub |
| Zaglavlje | 61 dp, naslov 22 sp Bold, povratak 48 dp |
| Brendirani hero | radius 18 dp, lokalni SIDRENA znak 67 dp na navy pločici 83 dp, naziv 27 sp ExtraBold |
| Slogan | `Cijene pod kontrolom. Poslovanje s povjerenjem.`, 12 sp, centrirano |
| Verzije | dvije fleksibilne kartice min. 107 dp, radius 15 dp, Android nasuprot WordPress dodatku |
| Upute | redovi najmanje 76 dp, lokalna ikona 43 dp, tekst 14/12 sp, strelica samo na radnji |
| Podaci | URL povezane stranice i datum posljednje objave s odgovarajućom oznakom kada nedostaju |
| Podrška | funkcionalno otvaranje sistemske aplikacije za e-poštu, bez vanjskih SDK-ova |
| Podnožje | neutralno objašnjenje read-only podataka i `SIDRENA by Brendigo` |
| Responsive | `verticalScroll`, flexible `Row.weight`, prelomi i skraćivanje dugačkih URL-ova |

### Podaci i funkcije

- `aboutMetadata()` razdvaja **verziju Android aplikacije iz `BuildConfig.VERSION_NAME`** od **verzije WordPress dodatka iz `CatalogIndex.pluginVersion`**. Nijedna se ne smije izmišljati niti jedna zamjenjivati drugu.
- Datum posljednje objave i revizija pravnog profila prikazuju se samo kada ih javni WordPress API doista objavi; nedostajući profil se ne predstavlja kao pravna potvrda.
- `aboutPublicPriceListUrl()` prihvaća samo `SiteUrl.publicLink()` (ista javna HTTPS domena); gumb se prikazuje **samo kada postoji stvarna sigurna poveznica**. `SidrenaApp.openPublic()` provjerava URL još jednom pri otvaranju.
- `Kako povezati stranicu` i `Što su sidrene cijene?` otvaraju stvarne informativne dijaloge; `Kontaktiraj podršku` koristi postojeći `ACTION_SENDTO` za e-poštu; gumb natrag vodi u Postavke.
- Donja navigacija aktivno označava Postavke i dok je otvoreno O aplikaciji. Duplirano generičko zaglavlje je skriveno.
- Statična stranica ne zahtijeva dodatne mrežne pozive ili dopuštenja.

### QA

- `ui/AboutScreen.kt`: dva `@Preview` prikaza (393 × 852 i 360 × 640 dp).
- `ui/AboutScreenPresentationTest.kt`: šest testova verzija, praznih datuma i metapodataka, URL validacije, nemogućih HTTP/external URL-ova te pravnog profila.
- U CI-ju obvezni Kotlin build, unit tests, Lint, sigurnosni/dead-code audit i APK/AAB validacija.
- Ručni test: Android Back i gornji povratak, pomoćni dijalozi, e-pošta s/bez instalirane aplikacije, veliki font, kratka visina i duga domena. **Tvrdnja 1:1 pixel-perfect nije dokazana dok emulator screenshot nije poravnan s korisnikovim referentnim prikazom.**

