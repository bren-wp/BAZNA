# BAZNA 1.5.24 — sigurnosni, dead-code i pravni revizijski zapis

**Datum:** 17. studenoga 2026.  
**Opseg:** WordPress/WooCommerce, PHP Core, Joomla, Drupal, Shopify adapter, Android, Windows i GitHub distribucija.  
**Status:** revizija izvora i automatizirani testovi; **nije penetracijski test, revizija živoga POS-a, niti potvrda 1:1 vizualne ili zakonske sukladnosti produkcijskih instalacija**.

## Zabilježeni i ispravljeni nalazi

| Oznaka | Nalaz | Rizik | Zahvat | Regresija |
| --- | --- | --- | --- | --- |
| ARH-01 | Službene datoteke s razmacima/HR znakovima u adresi nisu prolazile ASCII-only whitelist za 15-dnevni hold pri zamjeni | Zamijenjeni cjenik stariji od 31 dana mogao je biti odmah obrisan protivno čl. 3. st. 6. NN 132/2026-1584 | Jedna Unicode-aware provjera naziva u `class-feed-files.php`, koristi je i hold i prune | `tools/test_official_retention.php` |
| ARH-02 | Javni arhiv koristio je `str_ends_with`, PHP 8-only funkciju unatoč `Requires PHP: 7.4` | Fatal error na podržanom shared hostingu | Zamijenjeno PHP 7.4-kompatibilnom usporedbom sufiksa | PHP 7.4 CI + `tools/test_public_archive.php` |
| ARH-03 | Ulazni naziv arhive pretvarao je putanju u basename | Obrambeni sloj nije izričito odbijao `../`, kodirani `%2f` ili Windows separatore | Reject kodiranih/raw separatora prije pristupa putanji | `tools/test_public_archive.php` |
| QA-01 | Android Play preflight očekivao je 1.5.23/versionCode 26 | Svako zakonito novo izdanje rušilo audit | Validacija verzije iz Gradle-a i istog unified release YAML-a te inkrementalni versionCode | `tools/check_android_play_readiness.py` |
| QA-02 | Refleksijski test privatnog prune helpera oslanjao se na ponašanje novijeg PHP-a | Testovi na minimalnom PHP 7.4 nisu bili izvršivi | `ReflectionMethod::setAccessible(true)` uz poseban PHP 7.4 CI posao | `official-plugin-ci.yml` |
| DOC-01 | Prethodni README nije u prvom zaslonu predstavljao jasan proizvodni identitet | Nedovoljno razumljiva javna prezentacija | PNG/SVG marketinški banner, logotip, korisnička vrijednost, poveznice na izdanja, transparentne mogućnosti integracije | `audit_public_bazna.py` i `check_bazna_ui_brand.py` |

## Pravni model i testovi

Izvorni tekstovi:
- [NN 132/2026-1583 — isticanje bazne cijene](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html)
- [NN 132/2026-1584 — objava cjenika](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html)

Aktualna, bazna i najniža cijena u 30 dana nisu isto. Povijesni datum je 2. 5. 2025. za propisane skupine robe te 10. 9. 2026. za ostalo i usluge, s posebnim postupkom za prvu ponudu, akcije i iznimke. Program mora blokirati objavu nepotvrđenog povijesnog izvora; ni oznaka `historical_verified` bez stvarnog izvora nije dokaz.

**NN 132/2026-1584, čl. 3. i 6.**: službeni CSV ili XML, propisani stupci, UTF-8, separator `;`, službeni naziv i stabilan URL, na dan izmjene objava do 11:00, stara datoteka 15 dana **od trenutka zamjene**. Hrvatski znakovi i razmaci u legalnim nazivima ne smiju uzrokovati gubitak verzija. HTML i WordPress shortcodeovi služe ljudskom pregledu, ne zamjenjuju strojno čitljiv cjenik.

Za provjeru izvora, arhiviranja, filtriranja poslovnica i frontend prikaza:
`tools/test_official_feed.php`, `tools/test_official_retention.php`, `tools/test_public_archive.php`, `tools/test_public_embed.php`, `tools/test_public_api_contract.php`, `web/tests/test.php` i ostali poslovi CI-ja.

## Sigurnosna kontrolna lista

| Površina | Ugrađene kontrole / status | Što ostaje za produkciju |
| --- | --- | --- |
| Javni CSV/XML | Podaci samo iz potvrđene objave, odvojena interna evidencija dokaza, naziv i prepoznatljiv format | Stvarni CDN/cache, bot dohvat, WAF, HTTPS i povratni statusni kodovi |
| Javni arhiv | Validacija naziva; sada uključuje kodirane separatore i zamjenske holds; vremenske snimke ostaju razdvojene | Crash-recovery, sekvence pri simultanim objavama, cjelovitost nakon ponovne pohrane te stvarni 15-dnevni test na domeni |
| WordPress uredništvo | WP uloge, nonce i escaping; `tools/audit_security.py` i `tools/audit_repository.py` | Ručni test stvarnih ovlasti i zlonamjernih ulaza u svakoj kartici; audit dodataka i teme |
| Android | HTTPS, filtriranje javnih IP adresa, bez preusmjeravanja i proxyja, read-only API | Emulator test svih ekrana i varijacija DPI-a; Play release potpis, privacy disclosure |
| Windows | Lokalni SQLite, zaštita lokalnih datoteka, read-only javni pristup i UI redaction | Potvrda Windows Authenticode potpisa, recovery prijenosa i do kraja autentificirani sync |
| Shopify | HMAC app-proxy i webhook potpisi, testovi izolacije poslovnica i GDPR zahtjeva | OAuth, stvarna višetrgovinska instalacija i App Store review |
| PHP/Joomla/Drupal | Izolirana privatna pohrana i provjerena javna projekcija, CSRF/ACL i verzioniranje | Stvarni CMS integration/E2E, konfiguracija shared hostinga i rasporeda |
| Izdanje | CI provenance istog SHA-a, provjera SHA-256 paketa i izvornih verzija; 17 prepoznatih asseta | Potpisivanje i provjera instaliranja/upgradea u ciljnom sustavu |

## Dead-code i compatibility audit

Pokrenute su automatizirane statičke revizije `tools/audit_security.py`, `tools/audit_desktop.py` i `tools/audit_android.py`, uključujući heuristiku privatnih funkcija i XAML event handlera. WordPress dinamičke hookove, shortcodeove, reflection i pozive kroz nazive funkcija **nije sigurno označiti mrtvim samo prema broju tekstualnih referenci**. Nismo ih masovno uklanjali niti mijenjali javne kompatibilne slugove, WP metaključeve ili API v1.

U prvoj provjeri na prijelaznom SHA-u audit je prijavio ponovljenu deklaraciju `MainWindow` u različitim kontekstima kao `REVIEW`; nije dokaz nekorištenog koda. Obvezno se izvodi stvarni PHP lint, Android build/lint/test i Windows build/smoke, uz samostalnu provjeru minimalnog PHP 7.4. Ovaj dokument ne tvrdi da je proveden potpuni semantički tree-shaking ili ručni code review svakog pozivnog grafa.

## Ograničenja i kriterij za produkcijsku potvrdu

1. **Podaci trgovca:** potvrditi referentnu dokumentaciju/POS, akcije na referentne dane, iznimke i cijene po poslovnicama, bez automatiziranog izmišljanja povijesti.
2. **Rok objave:** izvršiti E2E na stvarnom serveru/HTTPS domeni u zoni Europe/Zagreb sa stvarnim schedulerom, promjenom prije 11:00, prekidom/retryjem i nedostupnim WAF-om.
3. **Arhiva:** testirati dvije objave istog dana, originalni hash i zamjenu nakon 31+ dana, 15 dana javne dostupnosti od zamjene, prava datoteka i neprepisivost.
4. **Sinkronizacija:** Android je javni čitač; Windows nije automatski dvosmjerni administrator WordPressa. Zajednički API format **nije** transakcijski sinkronizator. Za pisanje tražiti ovjereni HTTPS endpoint, ovlasti, revizijski trag, idempotency i conflict resolution.
5. **1:1 UI:** mjerenje razlike svih zaslona i stanja prema stvarnim referencama, DPI i tipografiji; responzivnost nije dokaz nultoga odstupanja.
6. **Potpuna sigurnost:** neovisno pregledati stvarne hostinge, ovisnosti, WordPress role escalation, mrežne granične slučajeve, DoS, rad u konkurenciji i podatkovne migracije; podvrgnuti sustav penetracijskom testu.
7. **Distribucija:** Android debug APK i unsigned AAB nisu Play-prod paketi; Windows unsigned Portable/Setup nije Authenticode-potpisan. GitHub prerelease nije sinonim potpunog produkcijskog odobrenja.

**Zaključak:** ispravci ARH-01/02/03 uklanjaju konkretne reproduktivne rizike. Automatizirana kontrola izvora se širi, ali ostaju eksplicitni produkcijski kriteriji. Puni status zakonske implementacije pojedinog članka prati [BAZNA compliance matrica](BAZNA-COMPLIANCE-MATRIX-2026-11-17.md).
