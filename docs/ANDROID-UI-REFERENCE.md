# SIDRENA Android — vizualni standard

Android zasloni koriste jedinstveni SIDRENA dizajn usklađen s dostavljenih dvanaest referentnih prikaza.

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
| 1. Splash | Izvorni narančasti SVG simbol S, slogan i potpis Brendigo bez trake učitavanja |
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


## Završni vizualni audit — zaslon 01 Splash

Izvorni preglednik: `SIDRENA-Android-Kompletan-Brending.zip` → `screens_concept/01_splash.png` (1080 × 1920). Datoteka `logos/sidrena-splash-1080x1920.png` ima iste piksele. Autoritativni vektor: `logos/sidrena-splash.svg`. Implementacija je `ui/SidrenaSplashScreen.kt`, s izvornim vektorskim `Path` koordinatama umjesto lokalnog launcher simbola koji nije istovjetan izvornom znaku.

**Odstupanja otklonjena:** prethodni prikaz koristio je mali S u uokvirenoj kartici, kutne dekoracije, plavu pomoćnu boju slogana i indikator učitavanja. Referenca ima velik samostalan narančasti znak S, dvije suptilne kružnice, odgovarajući plavi dijagonalni gradijent, bijeli veliki naziv i odvojeni narančasti potpis. Korisnik nije tražio dodatne slike — implementacija crta vektorski sadržaj lokalno, bez slikovnih ili mrežnih resursa.

| Referentni element | Točna SVG koordinata / vrijednost |
|---|---|
| Radni format | 1080 × 1920 jedinica |
| Pozadinski gradijent | `#07182D` → `#123457`, gornji lijevi u donji desni kut |
| Desna gornja kružnica | centar (935, 250), radijus 540, `#FF852C` s 5,5% prozirnosti |
| Donja lijeva kružnica | centar (120, 1650), radijus 450, `#5C8EBB` s 5,5% prozirnosti |
| Glavni simbol | izvorna `Path` geometrija, translacija (16, 188), skaliranje ×4 |
| Gradijent simbola | `#FFAA55` → `#FF852C` |
| Naslov SIDRENA | središnja x=540, baseline y=1160, 119 jedinica, Bold, tracking 10 |
| Slogan – redak 1 | baseline 1248, font 42, `#A9BAD1` |
| Slogan – redak 2 | baseline 1310, font 42, `#A9BAD1` |
| Potpis by Brendigo | baseline 1720, font 36, `#FF852C` |
| Trajanje | 2200 ms, bez vizualne trake učitavanja; prethodna navigacija ostaje nepromijenjena |

`splashGeometry()` u originalnom 1080×1920 formatu daje točno navedene SVG položaje. Na drugim omjerima zaslona slovo i veličina teksta skaliraju se jednoliko, bez rastezanja; vertikalni oslonci prilagođavaju se visini. `SidrenaSplashGeometryTest.kt` pokriva izvorni format i kompaktne/izdužene ekrane.

**Ostaje za dokazati:** usporedbu snimke aplikacije u emulatoru s autoritativnom referentnom PNG datotekom, na istom viewportu, uz maskiranje eventualnih sistemskih traka i provjeru razlika zbog Android zamjenskog fonta. GitHub Actions Kotlin build, JUnit i Lint ne dokazuju konačan pixel-perfect 1:1, nego samo tehničku valjanost. Nakon stvarne usporedbe slijedi zaslon 02 Povezivanje i naredni redom.


## Završni vizualni audit — zaslon 02 Povezivanje

Izvorna referenca: `screens_concept/02_povezivanje.png` (1080 × 1920) iz dostavljenog brending paketa. Nije potrebno generirati dodatne slike.

**Odstupanja ispravljena:** prethodni ekran imao je veliki kružni simbol poveznice, drugačiji naslov, centrirani uvod, običan `OutlinedTextField` s lijevom ikonom i drugačije tekstove gumba i sigurnosne kartice. Na referenci tih elemenata nema. Novi `ui/SidrenaConnectionScreen.kt` koristi naslov **Poveži WordPress stranicu**, lijevo poravnati uvod, tamnu adresnu karticu, širok narančasti gumb **Poveži stranicu**, istaknuti naslov **Sigurna veza** i mali potpis SIDRENA u podnožju.

| Element | Izvorna geometrija / provedba |
|---|---|
| Glavna boja | `#07182D`; u podlozi inputa `#10263D`; informativni panel `#19304A` |
| Naslov | povratak narančast `#FF852C`, bijeli `Poveži WordPress stranicu`, lijevi alignment |
| Podnaslov | `Svi javni cjenici na jednom mjestu` |
| Opis | `Poveži svoju web-stranicu za pristup objavljenim proizvodima, uslugama i arhivi.` |
| Input | oznaka `HTTPS adresa web-stranice`, bold korisnički URL, bez lijeve ikone |
| Placeholder | `https://primjer-trgovina.hr` prikazuje se **samo kao primjer**, nikada nije stvarni unos |
| Primarna akcija | narančasta, min. 52 dp, `Poveži stranicu`; validacija u `SiteUrl.parse()` |
| Informacija | `Sigurna veza`, zeleni `#52D9A3` i tekst o javno dostupnom WordPress sadržaju |
| Podnožje | `SIDRENA • by Brendigo`, bez demo/koncept oznaka |
| Responsivnost | razmaci za 360×640 i 393×852 dp, vertikalni scroll i `imePadding` |

**Funkcionalnost ostaje stvarna:** `BasicTextField` prihvaća samo jednu liniju do 2048 znakova, tipkovnica za URL i potvrda pozivaju `SidrenaViewModel.connect()`; `SiteUrl.parse()` odbija nesigurne/nevaljane URL-ove. Prazna adresa stvara stvarnu validacijsku poruku umjesto povezivanja na primjer iz dizajna. Gumb i tipkovnica prikazuju stanje učitavanja i mrežnu pogrešku. Povratak ostaje aktivan prema dosadašnjoj logici.

**Testovi:** `SidrenaConnectionLayoutTest.kt` pokriva kompaktne/izdužene raspone i odbijanje multiline i predugih vrijednosti. Vizualno 1:1 još nije dokazano bez screenshot usporedbe emulatora s originalnim PNG-om; statički demo indikatori telefona iz dizajnerskog mockupa namjerno nisu replicirani u pravoj Android aplikaciji.


## Završni vizualni audit — zaslon 03 Početna

**Primarna referenca** je ranije potvrđeni treći telefon s glavnog SIDRENA marketing/brending boarda (`sidrena_ref_screen03_crop.png`); `screens_concept/03_pocetna.png` prikazuje **drugu, pojednostavljenu kompoziciju** i koristi se samo kao sekundarna provjera palete/teksta. Vizualni elementi iz ta dva različita izgleda **ne smiju se miješati**. Svi podaci u stvarnoj Android aplikaciji su iz javno povezanog WordPress kataloga, a ne iz primjerâ na referenci.

| Dio | Primarna referenca i izvedba |
|---|---|
| Gornje zaglavlje | `SIDRENA` s narančastim izvornim S logotipom i ikoncom pretrage; 62 dp |
| Lokalna ikona | `res/drawable/ic_sidrena_mark.xml` sada s točnom geometrijom i gradijentom izvornog `logos/sidrena-symbol.svg` |
| Povezana stranica | zasebna kartica s zelenom ikonom stanja, domenom i **stvarnim** datumom posljednje objave; pristup osvježavanju/promjeni weba |
| Statistike | **2×2** kartice, 132 dp svaka; lokalni piktogram gore, naziv i realna brojčana vrijednost dolje; proizvodi, usluge, cjenici i arhiva |
| Poslovnice | samostalna kartica 78 dp, stvarni broj javnih poslovnica; otvara zaseban zaslon Poslovnice |
| Nedavne objave | naslov `Nedavno ažurirano`, tri posljednja provjerena objavljena dokumenta unutar jedne kartice, 62 dp po retku |
| Donja navigacija | Početna / Cjenici / Arhiva / Postavke, aktivna narančasta, diskretna gornja linija 0,6 dp |
| Boje | `#07182D` pozadina, `#10263D` kartice, `#28455F` obrub; `#FF852C` primarni naglasak |
| Informacije koje nisu na referenci | upozorenja ili pogreške prikazuju se samo kad doista postoje; pravni profil dostupan preko postojećeg prikaza |

### Funkcionalne i sigurnosne odluke

- `HomeDashboardData.kt` definira `homeMetrics()`, `homeRecentPublications()` i `homeSiteLabel()`. Broj proizvoda/usluga dolazi iz **stvarnih učitanih CSV redaka**. Broj **Cjenici** broji samo valjane aktualne CSV feedove odabrane poslovnice, a **Arhiva** samo valjane, jedinstvene javne CSV/XML dokumente. **Nikada** se ne prikazuju primjeri 1.248 / 320 / 18 / 156 iz dizajna kao stvarni brojčani podaci.
- `homeRecentPublications()` spaja aktualne objavljene CSV dokumente i valjane arhivske CSV/XML datoteke, uklanja duplikate po URL-u, sortira po stvarno objavljenim datumima i prikazuje **najviše tri**. Dokumenti se otvaraju stvarnom sigurnom HTTPS poveznicom iste domene, kroz postojeći `openPublic()`.
- Povezana stranica prikazuje stvarni host, a ne izmišljeni naziv trgovine. Oznaka `Povezano` ne predstavlja lažnu provjeru da je web trenutačno online; tijekom učitavanja prikazuje se postojeće stanje `Osvježavanje`.
- Tražilica, promjena web stranice, osvježavanje, poslovnice i pravne informacije ostaju dostupni bez gubitka postojećih funkcija i ponašanja ostalih zaslona.
- `HomePresentationTest.kt` i `HomeReferenceLayoutTest.kt` pokrivaju stvarne datume i dokumente, brojanje odabrane poslovnice, sigurne URL-ove i izlaz bez lažnih demo brojki.

**Vizualna potvrda i dalje nedostaje:** Android emulator screenshot na rezolucijama 360×640 i 393×852 dp uz odgovarajući DPI; zatim usklađena screenshot-overlay usporedba s glavnom marketing referencom i dodatne korekcije po pikselima, uključujući sustavske trake i tipografiju. Gradle build i JUnit testovi provjeravaju funkcionalnu ispravnost, ne objektivni pixel-diff.


## Zaslon 04 — Proizvodi i zajednički WordPress/Android API ugovor

**Primarna vizualna referenca:** četvrti telefon glavne SIDRENA marketinške ploče; `screens_concept/04_proizvodi.png` dodatno opisuje tražilicu, sidrenu cijenu i stanja. U produkciji su sve fotografije, nazivi i cijene stvarni podaci WordPress/WooCommerce trgovine, nikad kopije demo artikala s referentne slike.

### Android UI

- `ui/ProductCatalogScreen.kt` koristi naslov **Proizvodi**, narančasti/brendirani navigacijski naglasak, tražilicu `Pretraži proizvode…`, filtre **Svi / Sa sidrenom cijenom / Dostupni** i `LazyColumn` kartice.
- Kartice: `#10263D` s `#28455F` obrubom, radius 14 dp, sadržaj min. 92 dp, lokalni okvir slike 64×72 dp, naziv 15 sp Bold, jedinica mjere/SKU, sadašnja cijena 17 sp ExtraBold narančasta `#FF852C` i diskretna sidrena referentna cijena.
- Filtri koriste samo postojeće javne CSV podatke: `sidrena_cijena` ili `dostupnost=dostupno`. **Akcija**, **Novo** i proizvoljne kategorije nisu lažno generirani dok API te informacije ne objavi.
- `ui/PublicProductImage.kt` podržava stvarne WordPress/WooCommerce fotografije samo ako `slika_url` postoji i pripada istoj HTTPS domeni kao spojena stranica. Mrežni pristup provjerava DNS protiv privatnih IP adresa, ne slijedi preusmjeravanja, ograničava odgovor na 1,5 MB, tip slike JPEG/PNG/WebP i dekodiranje na maksimalno 16 MP. Koristi postojeći OkHttp, ograničenu memorijsku predmemoriju i lokalnu fallback ikonu. Bez novih SDK-ova ili izmišljenih fotografija.

### WordPress dodatak

- U `includes/class-public-api.php` unutar postojećeg javnog `brendigo-sidrena/v1/cjenici` odgovora (bez promjene `api_version: 1`) svaki `feeds.proizvodi` / `feeds.usluge` i poslovnica sada imaju opcionalni `available_csv: bool`; dodatno `publication.csv_ready` prikazuje je li javni webshop objavio oba CSV feeda. Provjerava se postojanje **stvarne objavljene datoteke** `is_file` i `is_readable` bez izlaganja putanje na poslužitelju.
- Ove vrijednosti su samo informacija o spremnosti; API ostaje javan i read-only. Nema administratorske autentikacije, korisničkih podataka, lozinki ili privatnih kataloga u odgovoru.
- Android `CatalogDiscovery.kt` čita opcionalne booleane i odbija vrijednosti pogrešnog tipa. Ako polje nedostaje (stariji dodatak), nastavlja staro ponašanje učitavanja CSV-a; ako je `false`, prikazuje korisnu poruku umjesto nepotrebnog pokušaja nedostupnog endpointa.
- Zasebne poslovnice imaju zasebne readiness oznake, i njihov status nikada nije zamijenjen statusom webshopa. Nakon objave korisnik može osvježiti Android podatke.
- WooCommerce javni feed proizvoda `includes/class-feed-rows.php` i `includes/class-feed-files.php` sada sadrži **neobavezan** stupac `slika_url` iz izvorne objavljene fotografije artikla. Usluge i stare CSV datoteke ostaju kompatibilne; sidrena cijena, datum, dostupnost, SKU i jedinica mjere ostaju nepromijenjeni.
- `includes/admin/tab-javni-xml.php` sada prikazuje panel **SIDRENA Android · Povezivanje**, kopiranje javne HTTPS adrese, status pojedinačnih objavljenih CSV datoteka i upute za Android osvježavanje. UI je usklađen s Android paletom u `assets/css/admin.css`, `cjenik.css` i `archive.css`.
- Kod promjena kataloga **obavezna je objava/regeneracija CSV cjenika** u WordPress administraciji; javni REST endpoint namjerno ne regenerira katalog tijekom anonimnog zahtjeva, kako se ne bi dopuštala zlouporaba.

### Regresijski testovi i QA

- Android `PublicCatalogCompatibilityTest.kt`: čitanje novih oznaka, starije instalacije, odbijanje pogrešnih tipova, opcionalne fotografije, filtri, zaštita vanjskog HTTP/HTTPS izvora.
- `HomeReferenceLayoutTest.kt`: ne označava potpuno objavljene cjenike kada proizvodi/usluge ili odabrana poslovnica nisu spremni.
- WordPress `tools/test_public_api_contract.php`: end-to-end struktura odgovora `index()` uz izolirani PHP stub, uključujući spremnost feedova, URL-ove, poslovnice i izostanak lokalnih filesystem putanja u JSON odgovoru.
- `tools/test_public_feed_columns.php`: aktualne i sidrene cijene, datumi, valjan CSV BOM, opcionalna fotografija samo za proizvode.
- GitHub Actions: **Android** kompilacija, testovi, lint, APK/AAB; **WordPress** PHP lint, dva testna ugovora, reproducibilno ZIP pakiranje, audit cijelog repozitorija.
- Vizualni test 1:1 još zahtijeva stvarni emulator (barem 360×640 i 393×852), screenshot overlay i pregled WordPress administracije u pregledniku. Funkcionalni test s produkcijskom WordPress stranicom zahtijeva stvarno objavljeni dodatak i cjenike; lokalni stubovi ne potvrđuju hosting/DNS/WAF/HTTP konfiguraciju.



## Zaslon 05/12 — Usluge i WordPress prikaz cjenika

**Referenca:** dostavljena visoka slika \`SIDRENA-05-reference.png\` (Usluge, tamnoplava pozadina, narančasta strelica, široka tražilica i četiri kompaktne kartice). Prikaz je izrađen kao stvarni Compose UI, bez generiranja slike u chatu. Primjeri cijena/naziva na dizajnerskoj slici koriste se samo za razvojni preview i nikad se ne ubrizgavaju u produkcijski katalog.

### Android

- Kompozicija \`ServiceCatalogScreen.kt\` koristi \`#07182D\` pozadinu, naslov 23 sp, visinu zaglavlja 61 dp, tražilicu visine 40 dp (radijus 9 dp) odmah ispod zaglavlja, odmak 21 dp do prve kartice od liste i kartice \`#10263D\` visine barem 76 dp, radijusa 12 dp i obruba \`#28455F\`. Narančasta je \`#FF852C\`; siva \`#A9BAD1\`.
- Strelica i izbornik su funkcionalni. Izbornik sadrži filtre **Sve usluge / Sa sidrenom cijenom / Bez sidrene cijene**, **Osvježi usluge** i **Prikaži proizvode**. Filtri namjerno nisu stalno u dodatnom redu: tako glavni zaslon prati konkretnu dostavljenu referencu.
- Pretraga izravno filtrira stvarne javno objavljene usluge preko \`CatalogSearch.items\`. \`LazyColumn\` prikazuje stvarna imena i podatke; odabir kartice otvara detalj stavke.
- Jedna objavljena cijena **nije automatski „cijena od”**: prikazuje se **Cijena** i izvorna vrijednost. Sidrena cijena prikazuje se samo kad postoji, uz **objavljeni datum**. Nema lažnih promocija, nasumičnih cijena, slika ili izvora.
- Ako WordPress v1 prijavi \`available_csv=false\`, korisnik dobiva jasnu poruku da cjenik usluga nije objavljen. Kada API nema to novo polje (starija verzija), aplikacija ostaje kompatibilna i pokušava standardno CSV učitavanje.
- Stanje objave odabrane poslovnice provjerava se **isključivo na njezinu feedu**. Nema tihog prelaska na webshop kad poslovnica nije pronađena.

### WordPress

- U \`SIDRENA → Javni cjenik → SIDRENA Android · Povezivanje\` prikazani su objavljeni CSV proizvoda/usluga za glavni cjenik i zasebno prvih 12 poslovnica. Sve se poslovnice mogu provjeriti kroz postojeći javni \`GET /wp-json/brendigo-sidrena/v1/cjenici\`. Oznaka statusa računa se isključivo iz objavljenih čitljivih datoteka; **prazna, ali ispravno objavljena CSV datoteka** smatra se objavljenom.
- \`class-feed-files.php\` u javni HTML dodaje stabilne, HTML-escapeane \`data-kind\` (\`usluga\`/\`proizvod\`) i \`data-field\` atribute uz postojeće lokalizirane oznake. Javne mobilne kartice u \`assets/css/cjenik.css\` preuzimaju Android paletu, prikazuju stvarnu trenutačnu cijenu narančasto i sidrenu cijenu tamnije, bez skrivanja datuma ili izvornog sadržaja.
- Ništa se ne mijenja u WooCommerce cijeni, datumima, korisničkim računima, administratorskim lozinkama ili privatnom API-ju. Android je **read-only**, koristi HTTPS i podatke javne objave.

### Provjere prije spajanja

- Android: \`ServiceCatalogPresentationTest.kt\` uključuje pretraživanje, filtar, točnu trenutnu cijenu, nedostajuću sidrenu cijenu, sidreni datum i zaseban status poslovnice. CI mora proći compile, lint, unit tests te APK/AAB.
- WordPress: \`tools/test_public_catalog_markup.php\` poziva stvarni PHP builder s izoliranim WordPress stubovima; provjerava \`data-kind\`, \`data-field\`, escapeanje naziva, izvornu cijenu, sidrenu cijenu i datum. Uz to se izvode ostali REST/CSV testovi i reproducibilno pakiranje.
- Za stvarnu **pixel-po-pixel potvrdu** još treba usporediti emulator screenshot i referencu na jednakom DP/DPI okruženju, Android 360/393/411 dp širine te WordPress 360/768/desktop prikaz. Nije moguće tvrditi potvrđen 1:1 bez te usporedbe.
- Za stvarnu provjeru povezivanja: na testnom WordPressu objaviti cjenik, preko preglednika otvoriti \`/wp-json/brendigo-sidrena/v1/cjenici\`, provjeriti \`feeds.usluge.available_csv=true\`, otvoriti \`feeds.usluge.csv\`, provjeriti 302 odredište iste HTTPS domene, povezati Android, odabrati poslovnicu, osvježiti i otvoriti uslugu. Hosting/CDN/WAF mogu uzrokovati greške koje lokalni testovi ne oponašaju.


## Referentni audit — zaslon 06/12 Detalj artikla i usluge (2. 11. 2026.)

Nativni zaslon `ui/CatalogScreens.kt` povezan je s javnim WordPress CSV poljima. Za detalj se koristi **izvorni konceptni PNG 06_detalj_proizvoda.png (1080×1920)**: zaglavlje, kartica identiteta s naslovom, SKU-om i dostupnošću, kartica trenutačne i sidrene cijene te treća kartica datuma i izvora. U stvarnoj aplikaciji ne ispisuju se demo oznaka, lažni status-bar elementi, promotivni podaci ni izmišljene fotografije.

- Uklonjen je prethodni veliki generički hero/piktogram koji se ne pojavljuje na konceptnom PNG-u. Autentične fotografije proizvoda i dalje su podržane u zaslonu popisa Proizvodi (04), ali detalj (06) prvenstveno odgovara podatkovnim karticama i ne dodaje sliku koja nije na referenci.
- `PriceItem` i `CsvParser` osim stvarne trenutne i sidrene cijene sada čuvaju `cijena_jedinice_mjere`, `naziv_posebnog_oblika`, `sidrena_datum`, `barkod` i `marka`. Nijedno polje nije generirano iz dizajnerske slike.
- Detalj prikazuje izvornu jediničnu cijenu, naziv posebnog oblika prodaje, marku i barkod kada postoje. **Bazna cijena nije poistovjećena sa sidrenom**, a najniža cijena tijekom 30 dana prije sniženja mora imati neovisni i potpuni dokaz.
- Ako sidrena cijena postoji, ali datum **nije javno objavljen**, zaslon eksplicitno upozorava na neprovjeren povijesni datum. Ako cijene uopće nema, ona se ne računa iz današnje cijene.
- Spremanje odabira preko rotacije ekrana sada prenosi 14 public polja, ali vraća i stare zapise od 9/10 polja, bez administratorskih podataka.
- `LegalContract` i `AboutPage` dodatno čitaju informativni pravni profil provjeren 2. 11. 2026. i označavaju pravilnik o baznoj cijeni kao nacrt u savjetovanju. Ne prikazuje se potvrda da je konkretni trgovac zakonski usklađen.
- `CatalogRepository` i Početna iz WordPress API-ja čitaju `publication.pending_changes`. Stara objavljena CSV datoteka uz kasnije promjene proizvoda prikazuje se kao potencijalno zastarjela, čak i kad su datoteke fizički dostupne.

**Kontrole prije produkcije:** usporedba s izvornim PNG-om u emulatoru (isti DPI/inseti/font), vizualni pregled WordPress mobilnog i desktop kataloga, stvarna objava i preuzimanje JSON/CSV/XML na trgovčevoj domeni, test različitih poslovnica, WP-Cron/poslužiteljski cron do 08:00, pravna provjera dokaza za cijene iz 2025./2026., iznimaka i jediničnih cijena te konačnih pravilnika nakon 13. 11. Ovaj razvojni prolaz i uspješan CI ne znače automatsko zakonsko certificiranje ili izmjereno pixel-perfect 1:1.



## Reprodukcija obnovljenog kataloga i responzivni Android QA (PR #202)

- Odabrana poslovnica sprema se uz HTTPS stranicu tek nakon uspješnog učitavanja. Nakon gašenja procesa aplikacija ponovno učitava isti katalog; ako je poslovnica u međuvremenu uklonjena, prikazuje se glavni webshop katalog, a odabir se briše. Odspajanje briše i stranicu i poslovnicu.
- `DetailSelection.kt` pri obnovi detalja provjerava **domenu, poslovnicu, vrstu, ID i po potrebi SKU** te podatke ponovno pronalazi u svježe učitanom javnom CSV-u. Prethodno spremljena cijena nikada nije autoritativna nakon obnove.
- `DetailSelectionTest.kt` pokriva različite poslovnice, domene, tip proizvoda/usluge, promjenu cijene, uklonjene stavke i SKU. Povećani font i uski zasloni ne smiju trajno odrezati statističke kartice.
- `.github/workflows/android-ci.yml` pokreće `tools/run_android_responsive.sh`: devet stvarnih konfiguracija emulatora od 240 dp do 1280 dp, uključujući 393×852, tablet, landscape i povećani font. Instrumentacijski test `ResponsiveScreensInstrumentedTest.kt` prikazuje svih 12 Compose zaslona i snima ukupno **108 PNG datoteka** u GitHub Actions artefakt `SIDRENA-Android-responsive-screenshots`.
- To su testovi stvarnog renderiranja i veličine snimke, **ne** dokaz nepostojanja svakog overlapa niti 1:1 odstupanja prema izvornom dizajnu. Pixel-perfect odobrenje zahtijeva usporedbu svakog PNG-a s izvornom referencom pri istom DPI-ju, font scaleu, status/navigacijskim trakama i slijed provjere klikova.
- WordPress plugin ima CSS prilagodbe za 320/375/600/782 px: povezivanje Androida, URL, statusi objave, popis poslovnica i pomoć uz cijenu ostaju dostupni na uskim zaslonima. Za prihvat je potreban još stvarni pregled na WordPress mobilnoj administraciji i webshopu (uključujući 200% zum i tipkovnicu), ne samo PHP i CSS statičke provjere.

## Ekran 07/12 — Javni cjenici i provjera responzivnosti (2. 11. 2026.)

**Referenca:** originalni `screens_concept/07_cjenici.png`, 1080 × 1920, bez izmišljenih sustavskih oznaka, stvarnih PDF dokumenata ili DEMO značke u produkciji. Implementacija ostaje u postojećem PR-u #202.

### Android raspored i ponašanje

- Naslov `Javni cjenici` s narančastom strelicom, potpis `Objavljeni dokumenti` te vertikalno složene tamnoplave kartice s velikom narančastom oznakom stvarnog formata CSV/XML/WEB. Raspored uključuje preciznije razmake i kartice visine najmanje 76 dp.
- Dodir na karticu ili ikonu otvara **provjerenu HTTPS poveznicu iste WordPress domene**. Nije dodan lažni PDF jer WordPress API takve dokumente ne jamči. Datumi dolaze isključivo iz stvarnih metapodataka objave.
- Pretraživanje, filtri, osvježavanje i arhiva funkcionalni su u izborniku; dodatni stalni redovi nisu prisutni na osnovnoj referenci. `available_csv=false` skriva neobjavljen dokument, a `null` čuva kompatibilnost sa starijim pluginom. Odabrana poslovnica nikada ne preuzima status webshopa.
- `AppNavigation` na zaslonima užim od 360 dp daje četiri dostupačna ikonografska odredišta s govornim opisima; 360 dp i više zadržava izvorne oznake. Na tabletima/panoramskim zaslonima sadržaj je centriran u maksimalno 840 dp, obrazac povezivanja u 560 dp.
- **Proizvodi 04/12:** na karticama užim od 310 dp proizvodna fotografija, puni naslov i jedinica mjere ostaju u gornjem retku, a aktualna i sidrena cijena prelaze ispod. Na klasičnim 360–411 dp uređajima originalna vodoravna referentna kartica ostaje nepromijenjena; referentne cijene se ne izmišljaju.

### Automatizirani Android testovi

- JUnit: `AdaptiveSizingTest.kt` mjeri 13 karakterističnih širina **240 / 280 / 320 / 360 / 393 / 411 / 480 / 600 / 720 / 840 / 1024 / 1280 / 1920 dp**, uključujući granične uvjete za navigaciju.
- Stvarni emulator: `ResponsiveScreensInstrumentedTest.kt` i `tools/run_android_responsive.sh` pokušavaju prikazati **svih 12 Compose zaslona** pri devet konfiguracija: **240×480, 320×568, 360×780, 393×852, 411×891, 600×960, 840×1120, 1280×800, 320×640 s fontom 1.4×** (160 dpi, Android API 35). Uz provjeru granica dodirnih elemenata prikuplja se 108 snimki ako test prođe.
- GitHub Actions objavljuje rezultat i screenshot artefakt `SIDRENA-Android-responsive-screenshots`. Samo uspješan izvršeni workflow dokazuje da je pojedina konfiguracija stvarno testirana. Dodatna vizualna screenshot razlika u odnosu na 1080×1920 referencu i stvarni uređaji i dalje su nužni za potvrđeno 1:1.

### WordPress responzivnost

- Public plugin `cjenik.css`: mobilne tablične ćelije prelaze u tamnoplave kartice do 720 px, dugi nazivi/URL-ovi se prelamaju bez vodoravnog pomaka cijele stranice; cijene i sidreni datumi ostaju vidljivi.
- Admin `admin.css`: široki uređivač s 15 stupaca ima **vlastiti vodoravni scroll**, dok navigacija, kartice, WooCommerce filtri, statusi poslovnica i gumbi ostaju unutar mobilnog zaslona. Zastarjeli gumbi za masovno kopiranje trenutačnih cijena/datumâ uklonjeni su zbog prethodno utvrđene pravne opasnosti.
- Browser test `tools/test_responsive_browser.mjs` pokreće stvarni Chrome na **13 širina 240–1920 px** za javni PHP renderirani cjenik i reprezentativni WordPress admin: **26 snimki + JSON mjerenja** u artefaktu `SIDRENA-WordPress-responsive-Chrome`. Tablični scroll dopušten je unutar tablice, ne preko cijelog dokumenta.
- WordPress je klijentov javni **read-only** izvor; Android ne preuzima administratorske lozinke i ne mijenja katalog. Niti automatizirana responsivnost niti zeleni CI sami po sebi ne predstavljaju pravni certifikat.

**Ograničenje:** "sve moguće" kombinacije rezolucija, DPI-ja, omjera stranica, pristupačnosti, verzija Androida i tema nisu konačan skup. Matrica iznad pokriva definiran skup graničnih i tipičnih profila, a ne sve uređaje. Za produkcijsko označavanje 1:1 preporučuje se screenshot-diff referenci po svakoj klasi uređaja te barem jedan stvarni fizički telefon i tablet.
