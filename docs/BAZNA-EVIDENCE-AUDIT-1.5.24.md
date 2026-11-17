# BAZNA 1.5.24 — dubinski audit dokazivosti bazne cijene

**Provjera propisa: 17. 11. 2026.** Ovo je implementacijska i QA evidencija, **nije pravna potvrda trgovca**. Za sva produkcijska izdanja testirajte na stvarnoj domeni, stvarnim proizvodima, dokazima i konfiguraciji POS-a. Pravna provjera prema NN 132/2026, pravilnici [1583](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html) i [1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html).

## Nezamjenjive zakonske činjenice

- Skupina `legacy_fmcg`: hrana, piće, kozmetika, sredstva za čišćenje, toaletne potrepštine i kućanstvo — referentni **2. 5. 2025.**
- Skupina `general_goods_services`: ostala roba i usluge — referentni **10. 9. 2026.**
- `reference_day`: iznos potvrđen za relevantni referentni dan.
- `first_offer`: datum **kasnije prve ponude** u izabranoj skupini; ne može biti u budućnosti.
- `discount_lowest_30d`: samo FMCG skupina, **najniža cijena u 30 dana prije početka sniženja na referentni dan**, ne automatski opći 30-dnevni popust.
- `discount_previous_retail`: ostala roba/usluge snižene na relevantni referentni dan — prethodno primijenjena maloprodajna cijena, a ne FMCG metoda.
- **Nikada** ne izvoditi povijesni iznos iz današnje maloprodajne cijene, WooCommerce sale, Shopify compareAtPrice ili pretpostavke o kategoriji.

## Što sada radi kod

| Platforma | Evidencija i pravila | Javna objava | Provjera |
| --- | --- | --- | --- |
| WordPress/WooCommerce | Privatni izvor, skupina, metoda i eksplicitna potvrda u proizvodu i varijaciji; ručni katalog ima ista polja | Nepotvrđeni iznos blokira novi kompatibilni i službeni feed; storefront i Blocks ga ne predstavljaju kao potvrđenu baznu cijenu | `tools/test_official_feed.php`, WP CI i vizualni test |
| Standalone PHP / Joomla / Drupal | Zajednički PHP Core: metoda, skupina, izvor i potvrda; odgovarajuća polja u web i Drupal uredniku | Ručna i automatska objava provjeravaju podatke; nema javnog objavljivanja nacrta | `web/tests/test.php`, `web/tests/test_editor.php`, multi-platform CI |
| Shopify | Čuva već dokumentirane podatke za identičnu varijaciju, ne preuzima `compareAtPrice` kao baznu cijenu | Koristi zajednički PHP servis i njegove blokade | Shopify integracijski CI |
| Windows | Posebna **aditivna SQLite tablica** `item_evidence`; mogućnost potvrde skupine i metode, povijest potvrda; stari iznosi nisu automatski potvrđeni | Lokalni CSV/XML i dalje su označeni kao **radni nacrt**, ne kao službena objava; provjeru stvarne javne objave obavlja poslužitelj | `Sidrena.Tests`, desktop CI |
| Android | Testirana ispravna odluka o skupini, metodi i referentnom datumu; i dalje čita **samo javno objavljene** podatke | Ne smije tvrditi da je nešto objavljeno dok nema potvrde web-poslužitelja | `LegalEvidenceRulesTest.kt`, Android CI |

## Migracija bez gubitka povijesti

1. **Backup prije nadogradnje:** pohranite WP bazu, WordPress uploads, Windows SQLite datoteku i privatne PHP `draft.json` / `published.json` / `archive.json`. Ne stavljati privatni arhiv u javni web root.
2. Postojeći WordPress `sidrena_*` meta-ključevi, slug `brendigo-sidrena-cijena` i javni API v1 ostaju zbog kompatibilnosti. **Stari povijesni iznosi ostaju, ali nemaju `_bazna_historical_verified=yes` bez provjere izvora.**
3. U svakom Woo proizvodu, varijaciji ili samostalnoj stavci unesite i provjerite **izvor, skupinu, metodu i datum**, pa potvrdite. Uvoz iz ERP-a može predložiti podatke, ne potvrditi ih umjesto trgovca. Naknadna promjena iznosa/datuma/izvora/skupine/metode poništava potvrdu.
4. Novi PHP Core zahtijeva iste podatke prije objave; stari nacrti ostaju raspoloživi za doradu. **Ako objava ne uspije, stara javna verzija može ostati dostupna** — to nije dokaz da trgovac ispunjava novi rok. Odmah otklonite nedostatke i izvršite novu objavu.
5. Windows automatski kreira tablicu `item_evidence` bez brisanja `price_items`, `price_changes` ili korisničkih zapisa. Povijesni iznos bez zasebne potvrde prikazuje se kao **zahtijeva dokaz**. Potvrda je audirana.
6. Android kompatibilnost ostaje čitanje, a ne lokalni administrator. **Debug APK** i **unsigned AAB** nisu potpisana Play Store distribucija.

## Obvezne produkcijske provjere (još nije dokazano)

- Klasifikacija i iznimke iz čl. 4. NN 132/2026, posebni popusti na referentni dan, ispravna dokumentirana najniža/prethodna cijena za svakog trgovca.
- Točnost svakog primljenog i izvezenog maloprodajnog iznosa i PDV-a, WooCommerce varijacije i Blocks u stvarnoj temi i dodatnim integracijama.
- Propisani CSV/XML raspored polja i stvarni identitet svih prodajnih mjesta, uključujući OIB/MB, vrstu/adresu/oznaku.
- Raspored objave **do 11:00 na dan promjene** prema Europe/Zagreb; automatizacija ovisi o ispravnom poslužiteljskom rasporedu i radnoj HTTPS domeni. Ne oslanjati se na promet.
- Zamijenjeni cjenici javno dostupni najmanje **15 dana od zamjene**, javni dohvat bez CAPTCHA/login/WAF blokade botova; integritet starih arhiva i višestruke promjene u istom danu.
- End-to-end 1:1 funkcionalnosti i nativni pikselni prikazi na stvarnim instalacijama WordPressa, Androida, Windowsa, Joomle, Drupala, Shopifyja: **nije certificirano**. Ujednačena poslovna pravila nisu isto što i identična administracijska funkcionalnost.
- WordPress.org SVN zahtijeva korisničke GitHub Secrets; Android Play signing i Windows Authenticode privatne ključeve. Ne deklarirati nepotpisane distribucije kao spremne za trgovine.

## CI provjere

- WP: `tools/test_official_feed.php` ima pozitivne i negativne metode, prvu ponudu, budući datum te **WP ↔ PHP Core parity**; WP plugin CI.
- PHP/Joomla/Drupal/Shopify: automatska/ručna objava, privatna pohrana, integracije i stvarno renderirane web vizualne snimke s jasno **demonstracijskim** podacima.
- Windows: SQLite migracija bez implicitne potvrde; provjera različitih metoda, zaseban audit i regresije backup/restore.
- Android: `LegalEvidenceRulesTest.kt` pokriva jednake metode, iznimku prve ponude, krivi referentni dan, buduću ponudu i nedostatak izvora/oznaku potvrde.

**Granica testa:** strukturna potvrda postojanja izvora ne dokazuje autentičnost računa. Datumi i metode moraju biti ljudski i pravno provjereni prema stvarnoj povijesti cijena.


## Dodatni dubinski sigurnosni i funkcionalni zahvati

- `Core::FIELDS` služi samo za privatne administracijske podatke. Odvojeni `Core::PUBLIC_FIELDS` zamrzava **stari Android v1** redoslijed javnih CSV/XML stupaca. `historical_source`, `reference_scope` i `historical_method` nikad ne smiju biti javni stupci — uključujući arhivu i Shopify/Joomla/Drupal proxy rute.
- Public CSV/XML imaju provjeru neutralizacije proračunskih formula u tekstu trgovca; iznos cijene ostaje stvaran numerički podatak. Provjeriti još pri izvozu na stvarnom POS-u.
- Nacrti PHP/Joomla/Drupal kataloga smiju imati nepotpunu povijesnu cijenu radi naknadnog uređivanja. **Niti ručna niti automatska javna objava** ne smiju prihvatiti nepotpun dokaz. Već potvrđeni iznos gubi potvrdu pri promjeni iznosa, datuma, izvora, skupine ili metode — uključujući puni JSON uvoz, ne samo obrasce. Novi potpuno dokumentirani zapis može biti izričito potvrđen pri prvom unosu.
- Javna kompatibilna HTML/CSV/XML projekcija ranijih privatnih snapshotova prikazuje aktualnu cijenu, ali **ne reklamira nedokumentiranu staru baznu cijenu**. Originalni privatni snapshot ostaje nepromijenjen. Za zakonski arhivski format i točnu historijsku reprodukciju obvezna je zasebna produkcijska provjera; retroaktivno prilagođavanje javne projekcije **nije** isto što i čuvanje originalne javno objavljene datoteke.
- WordPress kartica **Javni cjenik** razlikuje postojeće kompatibilne poveznice od regulatornih `official_feeds` i prikazuje službene CSV/XML URL-ove **samo kada ih stvarni public discovery potvrdi**; isto vrijedi za poslovnice. Prazan službeni status ne smije biti lažno zelen.

## Preostali kritični uvjeti za prihvat (release blocking)

1. **Popusti na referentni dan / na prvu ponudu:** trenutno se unosi metoda i izvor koji trgovac potvrđuje. Automatska provjera stvarne akcije, njezina datuma početka i najniže cijene u prethodnih 30 dana / prethodne maloprodajne cijene još nije izvediva bez potpunih povijesnih POS podataka. Posebne iznimke prema članku 4. oba pravilnika zahtijevaju stručnu klasifikaciju; ne uvoditi generički 'izuzmi sve' gumb koji bi sakrio obvezne podatke.
2. **Do 11:00 i arhiva 15 dana od zamjene:** provesti kontrolirani test promjene i dviju zamjena u istom danu na pravoj HTTPS domeni, s poslužiteljskim cron rasporedom, per-outlet URL-ovima, bot pristupom i očuvanjem izvornog arhivskog sadržaja.
3. **Vizualna usklađenost 1:1:** izvorni referentni PNG za svaki Android/WPF/WP zaslon nije dostupan kao verzionirani repozitorijski dokaz. Emulator screenshotovi i WPF snimke sami nisu dokaz `MAE=0`; referentne slike na istoj DPI rezoluciji moraju proći `tools/compare_ui_reference.py` i ručni QA interakcijskih stanja.
4. **Distribucija:** do zelenih svih relevantnih CI poslova i QA prihvata ne spajati PR niti objavljivati 1.5.24 kao produkcijski usklađeno. APK/AAB iz CI može biti nepotpisan ili samo debug, a Windows Setup/Portable bez Authenticode potpisa.

Prethodni uspješni CI ne zamjenjuje novu CI provjeru na zadnjem HEAD-u PR-a. Rezultate povezati s konkretnim commit SHA prije odobrenja.
