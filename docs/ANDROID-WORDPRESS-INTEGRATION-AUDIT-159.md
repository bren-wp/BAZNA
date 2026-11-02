# SIDRENA 1.5.9 — Android ↔ WordPress povezivanje, sigurnost i pravna provjera

**Datum pregleda: 3. studenoga 2026.** Ovo je tehnička kontrolna lista, ne odvjetničko mišljenje niti potvrda da su cijene određenog trgovca zakonite.

## Jedan WordPress dodatak, jedno Android povezivanje

1. Na javno dostupnu HTTPS WordPress stranicu instalirati **SIDRENA 1.5.9** (službena mapa `brendigo-sidrena-cijena`). Ne držati istodobno aktivne zastarjele izvorne dodatke koji dupliciraju prikaz cjenika. WooCommerce je opcionalan; ručno uneseni proizvodi i usluge podržani su u samostalnom načinu.
2. Aktivirati dodatak u WordPressu. U administraciji SIDRENA provjeriti *Postavke*, *Proizvodi*, *Javni cjenik*, *Poslovnice* i status objave. Objava CSV/XML datoteka može ovisiti o pozadinskom poslu (WP-Cron); **sama aktivacija ne znači da je CSV već objavljen**.
3. U pregledniku bez prijave provjeriti `https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici`. Ako hosting ne podržava pretty REST linkove, provjeriti `https://primjer.hr/?rest_route=/brendigo-sidrena/v1/cjenici`. Kada se WordPress nalazi u podmapi, koristiti njegovu stvarnu adresu, primjerice `https://primjer.hr/trgovina/`, i isti prefiks ispred endpointa.
4. Odgovor mora biti **JSON** s `api_version: 1`, `feeds.proizvodi.csv`, `feeds.usluge.csv` i `connection.mode: public_read_only`. Kod neobjavljenih CSV-ova `available_csv: false` je očekivan, a nije izmišljena objava. Zasebni CSV URL-ovi moraju postati čitljivi bez prijave nakon objave cjenika.
5. U Android aplikaciju SIDRENA unesite **HTTPS adresu WordPress instalacije**, ne adresu WP administratorske prijave ili cijelog CSV-a. Android prvo pokušava pretty REST endpoint i samo kod 404/405 ili HTML-a umjesto JSON-a koristi isti javni API preko `?rest_route=`. Ne šalje niti sprema administratorske lozinke.
6. Potvrditi učitavanje proizvoda, usluga, poslovnica, arhive, statusa CSV objave, stanja neobjavljenih promjena i pravnog profila. U slučaju 401/403, TLS pogreške, nevaljanog feeda ili blokirane mreže ne preskakati sigurnost nego ispraviti WordPress/WAF/HTTPS konfiguraciju.

## Granice sigurnosti i privatnosti

- Android prihvaća samo javne HTTPS domene, odbacuje lokalne/privatne IP adrese DNS provjerom u mrežnom klijentu te ne prati preusmjeravanja na nepouzdane vanjske domene. CDN s drugim hostom zahtijeva arhitekturnu prilagodbu i ne smije biti tiho stavljen na dopušteni popis.
- REST rute dodatka javne su **isključivo za čitanje već objavljenih cjenika**. Administrativne izmjene moraju ostati iza WordPress prava pristupa i nonce provjera; Android nema ovlasti uređivanja.
- CSV status `available_csv` i `pending_changes` nisu pravna potvrda; `compliance_certified` je **false**.
- Zakazani stari postupak koji bi iz trenutačne WooCommerce cijene pokušao zaključiti povijesnu cijenu ostaje blokiran. U 1.5.9 uklonjen je nedostižan dio njegove implementacije i neupotrebljavane private funkcije.
- Sva preuzimanja imaju vremenska/veličinska ograničenja; javna arhiva i feedovi ne otvaraju privatne WordPress podatke.
- **Otvorena ograničenja:** bez testiranja na stvarnoj WordPress instalaciji, uključujući reverse proxy/WAF, CDN, multisite, hosting cron i različite permalinke, kompatibilnost sa svakim hostingom nije dokazana. Sigurnosni skener i build nisu ekvivalent penetracijskom testu.

## Usporedba s aktualnim propisima RH

- [NN 101/2026, objava cjenika](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html), izmjena [NN 110/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html): proširene obveze objave cjenika i strojno čitljivog pristupa od **17. 11. 2026.**
- [NN 101/2026, isticanje dodatne cijene](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html) i [NN 110/2026, izmjena](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html): referentne cijene uz zasebno utvrđivanje stvarnog povijesnog iznosa.
- [NN 59/2026, izmjene Zakona o zaštiti potrošača](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html): nove odredbe o baznim cijenama i objavi cjenika; referentni povijesni podaci ne smiju se automatski izvesti iz tekuće cijene.
- [NN 105/2026, jedinična cijena](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html): jasan prikaz maloprodajne i cijene za jedinicu mjere prema vrsti proizvoda.
- Prijedlozi pravilnika o baznim cijenama i načinu objave cjenika bili su u e-Savjetovanju do **13. 11. 2026.** te se na dan ove provjere **ne smiju smatrati konačnim tekstom**. [Vlada RH, 15. 10. 2026.](https://www.vlada.gov.hr/ministar-susnjar-kroz-dijalog-s-gospodarstvenicima-do-provedivih-pravila-i-vece-zastite-potrosaca/47439)
- Sidrena/bazna cijena, najniža cijena u 30 dana prije sniženja i trenutačna cijena tri su **različita pojma**. Evidenciju i primjenjivost iznimaka provjerava odgovorna osoba trgovca.

## Automatizirane provjere i mjerila prihvaćanja

- `php tools/test_public_api_contract.php` — javna registracija ruta, status objave, poslovnice, REST v1 i bez administratorske autentifikacije.
- `php tools/test_legal_contract.php` i `php tools/test_legal_price_safety.php` — pravni metapodaci, zabrana izmišljanja povijesne cijene i blokiranje legacy cron postupka.
- `php tools/test_public_feed_columns.php`, `php tools/test_public_catalog_markup.php` — CSV/XML stupci, XSS escaping i mobilni HTML.
- `python3 tools/audit_repository.py`, `python3 tools/audit_android.py` te Gradle unit/lint — izvorni kod, verzije, mrežne restrikcije, kandidati za mrtvi kod.
- Android emulatori na 9 konfiguracija (12 zaslona) i Chrome na 13 širina za WordPress.
- **Potrebno dodatno:** potpuni end-to-end test nakon instalacije na stvarni HTTPS WordPress s WooCommerceom i bez njega, te ručni pregled sigurnosti svih administrativnih akcija. Nijedan automatizirani test ne potvrđuje sam po sebi zakonsku sukladnost konkretne trgovine.
