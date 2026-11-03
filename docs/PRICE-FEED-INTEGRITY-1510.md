# SIDRENA 1.5.10 — Provjera integriteta javnih cijena

**Opseg:** Android klijent i WordPress dodatak, 3. studenoga 2026.

## Potvrđene slabosti iz pregleda izvornog koda

1. Android je prihvaćao ponovljene CSV stupce, uključujući dva stupca `maloprodajna_cijena`, pa je uzimao samo prvi. Time se iz nepouzdanog ili oštećenog CSV-a mogla prikazati pogrešna cijena. Sada su duplicirani/prazni nazivi stupaca i retci s pogrešnim brojem polja nevaljani. Aplikacija ne prikazuje izmišljenu alternativnu cijenu, nego upozorava da izvor nije moguće učitati.
2. JSON REST discovery (objava, spremnost CSV-a, neobjavljene promjene), arhiva i 302 preusmjeravanja na javne cjenike nisu imali izričitu `no-store` politiku. U 1.5.10 za te odgovore koristi se `Cache-Control: no-store, max-age=0, must-revalidate`. Android uz to traži `Cache-Control: no-cache, max-age=0`. Statijski CSV server, CDN ili hosting i dalje mogu imati vlastitu predmemoriju; to se mora provjeriti na stvarnom webu.
3. Android je ignorirao dodatni sigurnosni `connection` objekt iz WordPress API-ja. Sada odbija izričito zahtijevanu autentifikaciju, bilo koji način osim `public_read_only`, nijekanje obveze objave CSV-a te off-origin kanonski REST URL. Stari WordPress REST v1 API-ji bez tog opcionalnog objekta ostaju podržani.
4. HTTP 204/206 više nije prihvatljiv za potpuni CSV/JSON cjenik. Samo HTTP 200 i dopuštena, kontrolirana preusmjeravanja mogu se obraditi; mrežna ograničenja (javne HTTPS adrese, DNS, veličina tijela, timeout) ostaju na snazi.

## Testovi i zaštita podataka

- Android `CsvParserTest`: duplicirana/prazna zaglavlja, različito napisani duplikati, premalo/previše stupaca i normalni CSV s referentnom cijenom.
- Android `CatalogDiscoveryTest`: public read-only, sigurni kanonski endpoint, bez administratorske prijave i bez izmišljenog certifikata.
- WordPress `test_public_api_contract.php`: prave GET rute, sadržaj objave, pretty/plain permalinks i cache politika na discovery, arhivi i CSV preusmjeravanju.
- GitHub CI: PHP lint, public API/legal/price tests, browser test 13 širina, Android Gradle/JUnit/Lint, 12 zaslona × 9 emulatora, cross-platform audit.
- Ovaj prolaz **ne** mijenja stvarne cijene, datume, CSV/XML stupce, WordPress korisnike niti zaštićene administrativne rute. Ne izvodi automatsko zaključivanje povijesne cijene.
- Testirani slučajevi nisu dokaz kompatibilnosti sa svakim CDN-om ili hostingom, niti potpun penetracijski test ili pravno mišljenje. Prije produkcije provjeriti povratne zaglavlja na stvarnoj HTTPS instalaciji i objavu arhive nakon promjene cijene.

## Koraci na stvarnoj instalaciji

1. Instalirati SIDRENA 1.5.10 i objaviti cjenike; u Androidu povezati HTTPS adresu weba.
2. Otvoriti `/wp-json/brendigo-sidrena/v1/cjenici` (ili `/?rest_route=/brendigo-sidrena/v1/cjenici`) i provjeriti `Cache-Control` zaglavlja, `available_csv` i `pending_changes`.
3. Promijeniti javni podatak i objaviti novi CSV; potvrditi da Android nakon osvježavanja prikazuje novu **objavljenu** vrijednost i ne preuzima zastarjeli REST redirect.
4. Učitati neispravan/dupliciran CSV na testnoj instalaciji; aplikacija mora prijaviti problem izvora i ne smije zamijeniti cijene između stupaca.
5. Odvojeno pravno potvrditi stvarne referentne i najniže 30-dnevne cijene odgovarajućim izvornim evidencijama.

**Povezano:** [Upute povezivanja, sigurnosti i propisa](ANDROID-WORDPRESS-INTEGRATION-AUDIT-159.md).
