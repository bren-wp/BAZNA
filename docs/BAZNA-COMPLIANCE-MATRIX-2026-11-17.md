# BAZNA — matrica zakonodavne i produkcijske provjere
**Datum provjere:** 17. 11. 2026. | **Razvoj:** PR #241, `audit/legal-evidence-parity-1.5.24`  
**Status:** tehnički audit i djelomična implementacija; **nije potvrda pravne usklađenosti pojedinog trgovca**.

## Službeni tekstovi
- [Zakon o zaštiti potrošača, NN 19/22, 59/23, 59/26 (izmjene)](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html)
- [Pravilnik o isticanju bazne cijene proizvoda, NN 132/26, 1583](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html)
- [Pravilnik o načinu objave cjenika proizvoda, NN 132/26, 1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html)
- [Pravilnik o maloprodajnoj cijeni i cijeni za jedinicu mjere, NN 105/26, 1270](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html)

**Značenje statusa:** `KOD + CI` označava da postoji implementacija i automatizirana strukturna provjera; `DJELOMIČNO` označava nepokrivene scenarije ili samo neke platforme; `BLOKADA` označava nedokazan obvezni uvjet. CI ne provjerava istinitost povijesne dokumentacije, stvarni POS niti produkcijski hosting.

| Izvor i članak | Zahtjev | Implementacija | Provjera / test | Status |
| --- | --- | --- | --- | --- |
| NN 59/26, čl. 4. (ZZP čl. 7.) | Razdvajanje aktualne, bazne i, gdje je primjenjivo, jedinične cijene | WP `includes/class-frontend.php`, `includes/class-woo-blocks.php`; PHP Core | `tools/test_legal_price_safety.php`, WP CI; stvarne teme/Blocks nisu ovjereni | DJELOMIČNO |
| NN 132/26-1583, čl. 3. st. 1. | 2. 5. 2025. za navedene FMCG skupine, 10. 9. 2026. za ostalo i usluge; datum prve ponude | `web/src/Core.php::legalEvidenceIssue`; WP `class-official-feed.php::evidence_issue`; Android `LegalEvidenceRules.kt`; Windows `LegalEvidenceRules.cs` | `tools/test_official_feed.php`, `web/tests/test.php`, Android/Windows CI | KOD + CI; stvarni podaci BLOKADA |
| NN 132/26-1583, čl. 3. st. 2.–5. | Akcija na referentni dan: FMCG najniža cijena 30 dana prije početka akcije; ostalo prethodna maloprodajna cijena | Odvojene metode `discount_lowest_30d` i `discount_previous_retail` te izvor dokaza | Parity testovi; nemaju kompletni povijesni POS ulaz i dokaz početka akcije | DJELOMIČNO |
| NN 132/26-1583, čl. 4. | Zakonske iznimke, bez proizvoljnog isključivanja robe/usluga | Evidencija i ručna klasifikacija | Stručna klasifikacija i produkcijski uzorak nedostaju | BLOKADA |
| NN 132/26-1583, čl. 5.–6. | Vidljivo, nedvosmisleno i uz odgovarajuće cijene za robu/usluge | WP javni prikazi i Blocks; PHP javni prikaz | Responsive CI, ali bez provjere svih stvarnih tema, trgovina i zaslona | DJELOMIČNO |
| NN 132/26-1583, čl. 7.–8. | Pravila tiskanih/TV oglasa i prijelazna pravila već izrađenih tiskanih oglasa | Izvan glavnog generatora cjenika | Nema automatske provjere svih marketinških kanala | BLOKADA operativne provjere |
| NN 132/26-1584, čl. 3. st. 2.–4. | Roba i usluge, točan sadržaj i redoslijed stupaca | WP `class-official-feed.php::columns`; PHP `Core::OFFICIAL_FIELDS` | `tools/test_official_feed.php`, `web/tests/test.php`; realni uzorci nisu provjereni | KOD + CI; poslovni podaci DJELOMIČNO |
| NN 132/26-1584, čl. 3. st. 5. | Objaviti izmijenjen cjenik do 11:00 na dan promjene | WP automatizacija, PHP objava i opcionalni server cron | Nedostaje dokazan test zakazivanja, ponavljanja i oporavka na stvarnoj domeni, Europe/Zagreb | BLOKADA |
| NN 132/26-1584, čl. 3. st. 6. | Arhivska datoteka dostupna najmanje 15 dana *od zamjene* | Postoje arhivski snapshotovi i javne arhivske rute | Nedostaje provjera točnog izvornog sadržaja i dvije zamjene u istom danu | BLOKADA |
| NN 132/26-1584, čl. 3. st. 7. | Različiti cjenici po mjestima, osim identičnog cjenika za sva | Poslovnice u WP i PHP; odgovarajući filename | Test postojeće i nepoznate poslovnice; stvarni više-poslovnički E2E nedostaje | DJELOMIČNO |
| NN 132/26-1584, čl. 4. | Iznimke od objavljivanja | Predviđeno odvajanje posebnih slučajeva, bez univerzalnog izuzeća | Nema potpunog ovjerenog klasifikatora | BLOKADA |
| NN 132/26-1584, čl. 5. | Javna dostupnost automatiziranom dohvaćanju | Javni API/CSV/XML bez potrebne prijave | Stvarni WAF, CAPTCHA, robots, cache i HTTP integracija nisu provjereni | DJELOMIČNO |
| NN 132/26-1584, čl. 6. st. 1.–7. | CSV/XML, fiksni URL pri zamjeni, naziv datoteke, UTF-8, separator `;` | WP `class-official-feed.php`; PHP Core; Shopify/Joomla/Drupal preko Core-a | `tools/test_official_feed.php` i integracijski PHP testovi; produkcijski HTTP i putanje BLOKADA | DJELOMIČNO |
| NN 105/26, čl. 3. i odredbe o jedinici mjere | Ispravan prikaz maloprodajne/jedinične cijene kada je primjenjivo | Polja za neto količinu, jedinicu mjere i izračunate/unesene iznose | Test strukture, nije dokazana točnost svih jedinica i poreza | DJELOMIČNO |
| Opća sigurnost, autorizacija i integritet podataka | Ne smiju se objaviti nepotvrđeni povijesni podaci | WP `evidence_issue` dodatno traži stvarno upisan izvor i kada je flag `yes`; PHP Core zasebno traži izvor | `tools/test_official_feed.php`: prazan/kratak izvor, uvoz s lažnim `yes` i WP/PHP parity bez zaobilaženja | Implementirano u PR #241; CI na zadnjem SHA-u mjerodavan |

## Posebni međusustavni uvjeti
- **WordPress** je uređivački autoritet za svoja prodajna mjesta. Samo stvarno potvrđeni server feed smije dobiti status `published`.
- **Android** trenutačno čita javni BAZNA API; nije punopravni offline uređivač niti postoji dokazano dvosmjerno usklađivanje.
- **Windows** čuva lokalne podatke i priprema izvoze. Izraditi siguran autentificirani sync i potvrdu poslužitelja prije tvrdnje da su javni cjenici objavljeni.
- **Joomla, Drupal, Shopify, PHP** koriste zajedničku jezgru gdje je implementirana; njihove stvarne instalacije, autorizaciju i HTTP putanje treba zasebno testirati.
- **1:1 funkcionalnost i pixel-perfect** nisu verificirani bez stvarnih referentnih snimki na istoj rezoluciji/DPI, tipografije, stanja forme i odgovarajućeg screenshot-diffa.
- **Distribucija:** Android debug APK/unsigned AAB i nepotpisani Windows artefakti nisu zamjena za produkcijsko potpisivanje. Ovo nije release odobrenje.

## Kriterij dopuštenog spajanja i izdavanja
1. Svi testovi i buildovi na **posljednjem** SHA-u PR-a uspješni, bez gašenja provjera.
2. Provjera realnih povijesnih cijena, klasifikacije i popusta reprezentativnim dokaznim POS uzorkom.
3. E2E na stvarnoj javnoj domeni: izmjena, objava do 11:00, dvije zamjene istog dana, točan 15-dnevni arhiv, bot dohvat, multi-outlet i cache.
4. Potvrđena kompatibilnost postojeće baze i migracija te sigurnost sinkronizacije.
5. Pixel-diff prema stvarnim referencama za svaku podržanu nativnu konfiguraciju; integracijski test nativnih platformi.
6. Samo potpisani i provjereni produkcijski artefakti označeni kao takvi.

**Matrica je namjerno konzervativna.** Ne podrazumijeva da je novi ili postojeći trgovčev katalog zakonski sukladan.
