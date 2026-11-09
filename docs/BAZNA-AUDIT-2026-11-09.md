# BAZNA — izvorni kod, sigurnost, dead code i UI revizija
**Datum:** 9. studenoga 2026.  
**Referentna grana:** `main` na commitu `3631a2eba99e6cd3946fa9d29ccc244dcf829197`  
**Opseg:** službeni WordPress dodatak, Android aplikacija, Windows sloj u zajedničkim sigurnosnim provjerama, PHP/JSON licencni servis, GitHub Actions i postojeći vizualni QA alati.

## Opseg dokaza i ograničenja

Izravno su pregledani datotečni popis, odabrani implementacijski moduli, postojeći izvještaji, registracije ruta/hookova te posljednji rezultati GitHub Actions. Nije obavljen vanjski penetracijski test, instrumentacija pokrenutih produkcijskih poslužitelja, reprodukcija aplikacije na fizičkom uređaju, niti mjerenje pikselskih odstupanja jer izvorni referentni PNG-ovi i odgovarajući novi screenshotovi nisu dostupni u ovome ciklusu. Stoga ne postoji metodološka osnova za postotke odstupanja ili tvrdnju „0 %”.

Posljednji potvrđeni zeleni CI na početnoj reviziji `main` od 3. 11. 2026. obuhvaća Android, WordPress, desktop, source audit i objavu izdanja. To nije dokaz da izmjene ove grane imaju zelen CI dok njihovi workflowi zasebno ne završe.

## Android — inventura svih 12 referentnih zaslona

| # | Zaslon | Datoteka | Potvrđeno pregledom izvornika / ključna provjera |
|---|---|---|---|
| 01 | Splash | `ui/SidrenaSplashScreen.kt` | BAZNA simbol i referentna geometrija u Compose; stvarni screenshot-diff nije proveden |
| 02 | Povezivanje | `ui/SidrenaConnectionScreen.kt` | HTTPS unos i spajanje s javnim WordPress katalogom; testirati rubne URL-ove i mali ekran |
| 03 | Početna | `ui/HomeScreens.kt` | BAZNA naslov, povezivanje, navigacija i status objave; provjeriti responsive raspored |
| 04 | Proizvodi | `ui/ProductCatalogScreen.kt` | Pretraga, filtri i cijene; ovisno o WordPress katalogu slika može nedostajati |
| 05 | Usluge | `ui/ServiceCatalogScreen.kt` | Popis i filtri; popravljena zastarjela poruka s nazivom SIDRENA |
| 06 | Detalj stavke | `ui/CatalogScreens.kt` | Javne cijene i datumi; ne prikazivati neprovjerene povijesne vrijednosti |
| 07 | Javni cjenici | `ui/PublicPriceListsScreen.kt` | Javni dokumenti i otvaranje; provjeriti učitavanje i duge nazive |
| 08 | Arhiva | `ui/ArchiveScreen.kt` | Objavljeni CSV/XML dokumenti i filtriranje; popravljeno ime previewa |
| 09 | Poslovnice | `ui/LocationsScreen.kt` | **Potvrđen funkcionalni jaz:** javni API ne daje adresu, radno vrijeme ni koordinate; bez novog ugovora API-ja to se ne može istinito prikazivati |
| 10 | Pretraživanje | `ui/SearchScreen.kt` | Ujedinjena javna pretraga; popravljeni nazivi previewa |
| 11 | Postavke | `ui/SettingsScreen.kt` | Povezana stranica, odabir i opcije; provjeriti stvarne promjene stanja |
| 12 | O aplikaciji | `ui/AboutScreen.kt` | Brend, verzija i upute; provjeriti poveznice i skaliranje tipografije |

**Prioritet vizualnog QA:** najprije 09 Poslovnice (nedostaje podatkovni model), 04 Proizvodi i 06 Detalj (prisutnost fotografija i razlike sadržaja), 03 Početna i 02 Povezivanje (layout), potom ostalih sedam zaslona. Ovo je prioritet po poznatim funkcionalnim prazninama, **ne** rang prema izmjerenom pixel-diffu.

Za objektivni QA koristiti `tools/run_android_responsive.sh`, usporedne snimke emulatora i `tools/compare_ui_reference.py` na identičnoj fizičkoj rezoluciji, DPI-u i sistemskim trakama. `docs/PIXEL-PERFECT-QA.md` opisuje postupak. Ne koristiti generirane zamjenske reference.

## WordPress — svih 8 administratorskih prikaza + javni prikazi

| Prikaz | Implementacija | Fokus revizije |
|---|---|---|
| Pregled | `includes/admin/tab-pregled.php` | KPI, statusi objave, prazna stanja i istinitost upozorenja |
| Proizvodi i usluge | `includes/admin/tab-proizvodi.php` | Duga imena, metapolja, WooCommerce varijacije, provjere ovlasti |
| Poslovnice | `includes/admin/tab-poslovnice.php` | Izmjene lokacija, jedinstvene oznake i usklađivanje s Android API-jem |
| Javni cjenik | `includes/admin/tab-javni-xml.php` | HTML/CSV/XML objava, statusi, javne poveznice i arhiva |
| Uvoz/Izvoz | `includes/admin/tab-uvoz-izvoz.php` | Mapiranje, predprovjera, CSV/XML/XLSX upload i rukovanje pogreškama |
| Postavke | `includes/admin/tab-postavke.php` | Izolacija podešavanja, cron i čitljivost na uskim zaslonima |
| Upute | `includes/admin/tab-upute.php` | Ispravljeni zastarjeli nazivi proizvoda, ZIP-a, izbornika i opis primarnih shortcodeova |
| Licenca | `includes/class-admin.php` | Forma i status aktivacije, domena, nenavođenje tajnog ključa u HTML-u |

Dosadašnji `docs/PIXEL-PERFECT-QA.md` popisuje sedam prikaza; aktualni `class-admin.php` sadrži i zaseban **Licenca** tab. U vizualnu QA matricu obvezno uključiti i licencu, kao i javni cjenik, shortcode blokove i mobilni prikaz. Test `tools/test_responsive_browser.mjs` koristi referentni HTML fixture i ne dokazuje pixel-perfect na stvarnoj konfiguraciji svake WordPress teme.

## Sigurnost — provjereni zaštitni slojevi i preostali testovi

- **Android transport:** `AndroidManifest.xml` zabranjuje cleartext i backup; `SiteUrl.kt` normalizira javne HTTPS domene; `CatalogRepository.kt` koristi OkHttp uz DNS filtriranje javnih IP adresa, bez automatskih preusmjeravanja i proxyja, uz ograničenje učitanih CSV datoteka. Osobito testirati DNS rebinding, redirect lance, TLS pogreške i napadačke CSV formate na odvojenom testnom poslužitelju.
- **Slike:** `PublicProductImage.kt` dopušta slike samo sa sigurne javne domene, MIME allowlist i ograničenu veličinu/rezoluciju. Testirati neispravan MIME, velike JPEG/PNG datoteke i prekide učitavanja.
- **WordPress REST:** `class-public-api.php` registrira javne read-only GET rute za objavljene cjenike/arhivu. Ne smiju se izlagati privatni metapodaci ili uvoditi write callback bez autentifikacije i granularnih ovlasti.
- **Uvoz:** `class-bulk-import.php` sadrži administrativne ovlasti, nonce, ograničenje veličine i predprovjeru; `class-xlsx.php` provjerava ZIP/DTD, broj članova i ukupnu raspakiranu veličinu. Preostaju fuzz testovi XML/CSV/XLSX, opterećenje memorije i transakcijska provjera djelomičnih uvoza.
- **Licenciranje:** `licenca/api.php` zahtijeva HTTPS i privatnu konfiguraciju/pohranu; `licenca/lib.php` provjerava domenu preko javne DNS adrese i TLS-a te zaključava JSON promjene. `includes/class-license.php` koristi WordPress ovlasti/nonce i kratku vremensku predmemoriju. Za produkcijski audit potrebna je zasebna provjera konfiguracije hostinga, tajnih vrijednosti, HTTP cachea, rate limita i opoziva licence.
- **CI i izdavanje:** ZIP SHA-256 provjere i postojeći testovi ne zamjenjuju potpisivanje Android AAB-a, malware pregled i ručne prijelazne testove nadogradnje.

**Zaključak sigurnosti:** u pregledanim putanjama postoje zaštitni mehanizmi; nije potvrđena iskorištiva kritična ranjivost. To **ne** znači da je kompletan sustav dokazano siguran ili da je odrađen puni penetracijski audit.

## Dead code i regresijska zaštita

- **Potvrđeno:** `Brendigo_Sidrena_Cijena_Product_Meta::maybe_autofill` je namjerno no-op, ali se svejedno registrirao za svaki WooCommerce spremanje proizvoda. Uklonjena je suvišna registracija hooka; metoda ostaje kompatibilna s eventualnim starim vanjskim pozivateljima. `tools/audit_security.py` dobio je regresijski test za ponovno uvođenje tog hooka.
- **Oprezno zadržano:** `snapshot_if_empty()` vraća `false` kako pravno neutemeljene cijene ne bi nastale iz današnjih iznosa. Legacy `process_batch` i drugi povezani ulazi nisu nasumce brisani zbog historijske kompatibilnosti i osjetljivih podataka.
- **Samo kandidati:** neiskorištene UI pomoćne funkcije, stare nazivne oznake `sidrena`, duplicirane deklaracije koje skripte upozoravaju i nepokriveni kodovi mogu biti aktivni preko WordPress hookova, reflectiona, XAML-a ili eksternih shortcodeova. Za njih su potrebni call graph i integracijski testovi prije uklanjanja.
- **Regresija brendiranja:** novi `tools/check_bazna_ui_brand.py` te GitHub workflow provjeravaju naziv BAZNA na odabranim vidljivim mjestima i ispravne poveznice na izdanje. Proširen je i WordPress CI za postojeći `tools/test_bazna_brand.php` test kompatibilnosti shortcodeova.

## Prijavljene korekcije ove grane

Popravljeni su vidljivi ostaci starog brenda na nekoliko Android zaslona, Android HTTP `User-Agent` za slike, WordPress administratorske upute, zastarjeli linkovi i verzije u README dokumentaciji te su dodane dvije CI provjere i uklonjen jedan nepotreban WooCommerce hook. Interni PHP klasni nazivi, pohranjeni WP meta ključevi, REST v1 rute, aplikacijski ID i stari shortcodeovi namjerno nisu preimenovani zbog kompatibilnosti. Nazivi pojedinih Windows/Android izgradnih artefakata još su povijesni i zahtijevaju zaseban koordinirani zahvat na izdavačkim workflowima.

## Obavezne završne provjere prije spajanja

1. Zeleni Android build + lint + unit test + instrumentirani screenshotovi na barem dvjema gustoćama i više omjera stranica.
2. WordPress PHP lint + WP/WooCommerce funkcionalni test + negativni slučajevi licence i import procesa, uključujući aktivnu licencu i isteklu licencu.
3. JSON licencni servis: dokaz vlasništva domene, pohrana izvan web-roota, SSRF test i rate limit.
4. Usporedba stvarnih referentnih PNG-ova s reproducibilnim screenshotovima uz izmjerene MAE i promijenjene piksele; nema tvrdnje 1:1 bez ovoga.
5. Pregled objavljenih i starih permalinkova, WordPress shortcodeova i stvarne kompatibilnosti pri nadogradnji s verzije 1.5.13.

**Granica zadatka:** ova grana isporučuje dokazive ciljane popravke i provjerljiv tehnički izvještaj, ne potvrđuje potpun formalni security audit niti postotak vizualne jednakosti.
