# BAZNA — matrica usklađenosti (17. 11. 2026.)

**Metoda:** službeni pravilnici NN 132/2026, br. 1583 i 1584, Zakon o zaštiti potrošača NN 19/22, 59/23, 59/26 i pravilnik NN 105/2026. Status **DJELOMIČNO** znači da nije potvrđena produkcijska provedba trgovca. Ovdje nije navedena potvrda sukladnosti niti tvrdnja da različite platforme imaju identičnu administraciju.

| Propis i odredba | Zahtjev | Implementacija | Dokaz/test | Status |
| --- | --- | --- | --- | --- |
| NN 132/2026, 1583 čl. 3(1) | FMCG: 2.5.2025.; ostala roba/usluge: 10.9.2026.; kasnija prva ponuda uz dokaz | `class-legal.php::reference_requirement`; Windows dokumentirani prvi datum | `tools/test_legal_contract.php`; `Sidrena.Tests` | **DJELOMIČNO** — WooCommerce razvrstavanje i provjera svih dokaza nisu automatizirani |
| NN 132/2026, 1583 čl. 3(2)–(3) | FMCG snižen na referentni dan: najmanja cijena u 30 dana **prije početka popusta** | Razlikovanje metode od iznosa, bez automatskog izmišljanja | `tools/test_legal_contract.php` | **DJELOMIČNO** — nedostaju izvorne povijesne evidencije i vezivanje uz pojedine stavke |
| NN 132/2026, 1583 čl. 3(4)–(5) | Ostala roba/usluge snižene na referentni dan: **prethodna primijenjena maloprodajna cijena** | Odvojena metoda pravnog profila; Windows čuva izvore | `tools/test_legal_contract.php` | **DJELOMIČNO** — nije potpuno provedena analitika povijesnih popusta |
| NN 132/2026, 1583 čl. 4 | Iznimke: posebni režimi, ciljane skupine/plaćanja, personalizacija, neusporediva dinamika, automati, posrednici | Bez automatskog proglašavanja iznimke | Ručni pravni pregled | **OTVORENO** — strukturirani workflow iznimaka nije dovršen |
| NN 132/2026, 1583 čl. 5–7 | Isticanje uz stvarnu cijenu i cijenu za jedinicu mjere gdje je primjenjivo, jasna odvojenost, tiskani/TV oglasi | WP frontend i javni katalog; dokumentiran kontrast | WP CI i vizualne snimke kao artefakti | **DJELOMIČNO** — potreban pregled na stvarnim temama, Blocks, oglasima i formatima |
| NN 132/2026, 1584 čl. 3(2)–(4) | Službeni redoslijed polja robe/usluge, uključujući EAN i neto količinu gdje primjenjivo | WP Official_Feed; PHP Core official CSV/XML | `tools/test_official_feed.php`, `web/tests/test.php` | **IMPLEMENTIRANO U KODU**, potrebni integracijski podaci trgovca |
| NN 132/2026, 1584 čl. 3(5) | Objava izmijenjenog cjenika do 11:00 lokalnog dana promjene | WP planiranje i PHP CLI cron; ručna objava | Simulacijski i CI testovi, ne stvarni SLA | **DJELOMIČNO** — hosting cron i nadzor točnog roka moraju se konfigurirati |
| NN 132/2026, 1584 čl. 3(6) | Arhiva dostupna najmanje 15 dana **od zamjene** | WP i PHP arhivski snapshoti | WP i PHP regresije | **DJELOMIČNO** — testirati URL i retenciju u produkciji |
| NN 132/2026, 1584 čl. 3(7), čl. 6(3)–(5) | Odvojena prodajna mjesta, identifikacija trgovca, broj pohrane, vremenska oznaka | WP i PHP filename generator i sekvence | `tools/test_official_feed.php`, `web/tests/test.php` | **DJELOMIČNO** — potvrditi fizičku poslovnicu/identitet |
| NN 132/2026, 1584 čl. 5, čl. 6(1)–(2), (6)–(8) | Javni dohvat CSV/XML, stabilna URL, UTF-8, razdjelnik ;, bez blokade botova | Javni REST i HTTP izlaz WP/PHP, posebno službeni format | CI parser i arhiva | **DJELOMIČNO** — mrežni/WAF test iz stvarne domene nije proveden |
| NN 105/2026, čl. 3–dalje | Maloprodajna i jedinična cijena tamo gdje zakon nalaže | Odvojena polja u WP i PHP | WP CI | **DJELOMIČNO** — sve kategorijske i mjerne iznimke treba testirati |
| ZZP NN 59/2026 | Najniža cijena prije sniženja ne smije se zamijeniti baznom cijenom | Različita polja/profil, bez automatskog računanja iz aktualne cijene | `tools/test_legal_contract.php` | **DJELOMIČNO** — audit evidencije i točne primjene sniženja ostaje otvoren |

## Funkcionalni status platformi

- **WordPress/WooCommerce** — najveća pokrivenost: cijene, varijacije, poslovnice, arhiva, CSV/XML, administracija. Nepotpuna verifikacija referentnih popusta i vizualna kompatibilnost tema.
- **Android** — **read-only javni pregled** i arhiva s pravnim upozorenjima; nema sigurnog dvosmjernog administratorskog uređivanja, lokalnog punog editora ni objave.
- **Windows** — lokalni katalog, povijest, uvoz/izvoz, backup; nema punu WordPress administraciju, provjerenu identičnu funkcionalnost objava ni kompletnu sinkronizaciju.
- **Joomla / Drupal / standalone PHP** — zajednički PHP Core za validaciju, javnu objavu, arhivu i nadziranu automatizaciju. Nije dokazana 1:1 administracija niti instalacija u sve produkcijske CMS verzije.
- **Shopify** — ograničeni adapter; OAuth i javna App Store distribucija nisu dovršeni.

## Sigurnosni rizici / produkcijski uvjeti

1. Nije dopušteno učitati stvarne podatke iz korisničkog računa ili računa trgovca kao dokaz referentne cijene drugog datuma; račun kupnje dokazuje samo navedeni događaj.
2. Osigurati čuvanje audit tragova, pristupne kontrole, provjeru HTTPS/WAF bot pristupa, cron monitor prije 11:00, timezone Europe/Zagreb i fail-closed objavu.
3. Android produkcijski AAB zahtijeva korisnikov upload ključ; Windows Authenticode zahtijeva odgovarajući privatni certifikat. GitHub debug APK i unsigned AAB nisu potpisana produkcijska izdanja.
4. Fiksni WordPress.org slug `brendigo-sidrena-cijena` i interni meta ključevi čuvaju se radi kompatibilnosti dok se javni tekst brendira BAZNA.

## Poveznice

- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html
