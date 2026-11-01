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

