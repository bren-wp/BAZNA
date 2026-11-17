# BAZNA — provjera javnih implementacija i produkcijske usklađenosti

**Stanje pregleda:** 17. 11. 2026. · **Opseg:** javni HTML, poveznice i dokumentacija; nije pristup izvornom kodu, administraciji ni produkcijskim tajnama trećih strana.

## Usporedni pregled

| Javni izvor | Vidljivo ponašanje | BAZNA implementacija / kriterij |
| --- | --- | --- |
| [SPAR cjenici](https://www.spar.hr/usluge/cjenici) | Korisnik bira datum u kalendaru; datoteke su popisane po odabranom danu; CSV UTF-8 / `;`, upozorenje za tablični uvoz | Arhiva se mora moći filtrirati po datumu i poslovnici, bez skrivanja izvornog CSV/XML URL-a |
| [Hedera arhiva](https://hedera.hr/apps/cjenik/arhiva) | Odvojena aktualna objava, kronološka arhiva, datum, broj stavki i CSV/XML poveznice | Prikazati važeći feed odvojeno od starih snimaka; snapshot mora ostati dohvatljiv nakon zamjene |
| [Ponosna Hrvatska cjenik](https://ponosnahrvatska.hr/pages/cjenik) | Timestamps i redni brojevi; isti dan može imati izvorni i ispravljeni cjenik koji su oba javno dostupna | Ne prepisivati ispravljenu snimku; označiti status zamjene uz očuvanje izvornog hasha |
| [Cijene API](https://cijene.dev/docs/) | Javni strojni pristup i dokumentirani JSON REST/ZIP tokovi | Objaviti predvidljiv javni discovery endpoint bez login zidova ili CAPTCHA-e |
| [WordPress Application Passwords](https://developer.wordpress.org/rest-api/using-the-rest-api/authentication/) | WordPress ugrađena per-app vjerodajnica i Basic Auth preko HTTPS-a | Koristiti aplikacijsku (ne glavnu) lozinku, provjeriti ulogu, odvojiti javni read-only API od privatnih draft izmjena |

**Referentni javni primjeri nisu pravna potvrda:** dio navedenih stranica još opisuje staru NN 101/2026 ili objavu do 8 sati. Takav tekst ni automatiku ne treba kopirati u novi BAZNA pravni profil.

## Obvezni normativni ugovor

Izvor: [Pravilnik o načinu objave cjenika NN 132/2026-1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html), čl. 3–6.

1. Roba i usluge imaju različite zakonom nabrojane stupce, u **točnom redoslijedu iz članka 3**.
2. Na dan promjene cjenika izmijenjenu objavu treba postaviti **najkasnije do 11:00, Europe/Zagreb**.
3. Stari CSV/XML mora ostati javno dostupan **15 dana od zamjene**, ne od prve objave.
4. HTML pretraživanje, iframe ili shortcode je dodatni pregled; **ne smije zamijeniti CSV/XML**.
5. Službena datoteka nosi identifikator trgovca, prodajno mjesto/adresu/oznaku, ROBU/USLUGE, broj pohrane i lokalni timestamp `GGGGMMDD_SSMM`, osim iznimke `ZA_SVA_PRODAJNA_MJESTA`.
6. UTF-8 i `;` za CSV; bez prijave dostupno botovima i bez blokade automatskog dohvaćanja.
7. Na istoj lokaciji/URL-u novo objavljeni cjenik zamjenjuje važeći; arhivske verzije moraju ostati zasebno adresabilne.
8. Izvor bazne cijene i iznimke ne mogu se proizvesti algoritamskim nagađanjem.

## Arhitektura sinkronizacije: izvori istine

- **WordPress standalone (trenutačni PR):** vlasnik privatnog kataloga. Autentificirani `GET` + `POST` na `/wp-json/brendigo-bazna/v1/sync/draft`, izravna zaštita WordPress ovlastima i TLS-om, obvezan SHA-256 `expected_revision` te `409 Conflict` pri zastarjelom stanju. Nema automatske objave CSV/XML.
- **Windows (trenutačni PR):** ograničena dvosmjerna razmjena *jedne aktualne cijene* i čitanja privatnog WordPress nacrta; aplikacijska lozinka samo tijekom poziva. Lokalni SQLite ostaje samostalna evidencija i ne pretvara se u lažnu potvrdu sinkronizacije.
- **WooCommerce:** ne prihvaćati standalone opcijski writer. Potreban je posebni Woo adapter na izvoru `WC_Product`, transakcijski concurrency i WP hooks.
- **Android:** još uvijek čita objavljene javne podatke; mobilno privatno uređivanje nije spremno za produkciju.
- **Web PHP/Joomla/Drupal/Shopify:** Core ima vlastite zaključane privatne `draft.json` transakcije i objavu; dvosmjerna interoperabilnost s WP zahtijeva mapiranje kanonskih ID-eva, revizija i sukoba. Ne dijeliti administratorske vjerodajnice među tenantima.

**Posebno važna granica:** SHA-256 prije izmjene štiti od ponovnog slanja zastarjele verzije kroz isti sinkronizacijski endpoint. WP administrator ili drugi dodatak koji istodobno zapisuje iste opcije još zahtijeva integrirani compare-and-swap / transaction lock. Bez toga se ne može deklarirati potpuno riješen multi-writer problem.

## Produkcijski E2E audit prije pravne potvrde

| Provjera na stvarnom hostingu | Metoda / dokaz | Negativni test |
| --- | --- | --- |
| Poslovni podaci | POS cjenici i dokumentacija za 2. 5. 2025. / 10. 9. 2026., po poslovnici i iznimci | Nepostojeći povijesni izvor mora blokirati objavu |
| Usporedba vrijednosti | Stvarna stavka u POS-u = javni CSV, HTML i etiketa po odgovarajućem pravilu | Neupisani iznos nikad ne zamjenjivati nulom |
| Automatski rok | Stvarni scheduler s pravilnom TZ `Europe/Zagreb`, svjedočeni timestamp i HEAD/GET bez cookies | Ugasiti WP-Cron i web promet te potvrditi radi li sistemski scheduler |
| Ispravak istog dana | Dvije objave i dvije zasebne snimke; preuzeti oba izvora | Stari URL ili arhiva ne smiju završiti 404 nakon ispravka |
| Zamjena starije datoteke | Cjenik A objavljen ranije, zamijenjen danas: A mora ostati ≥15 dana od danas | Brisanje starije objave nakon 31 dana od izvornog datuma ne smije narušiti novo zadržavanje |
| Anonimni strojni dohvat | Neautentificirani HTTPS HEAD/GET s vlastitim crawlerom, CSV/XML i jasnim kodovima | Bez CAPTCHA, WAF blokade, prijave, HTML login redirecta ili 403 |
| Sinkronizacija | Windows/WordPress idempotency, dva konkurentna admina i fizički prekid prijenosa | Za 409 sukob nije prihvatljivo tiho prepisivanje |
| Vizualna 1:1 kontrola | Stvarni referentni PNG za svaki ekran, rezolucija, DPI, stanje, tema i jezik | Broj razlika piksela mora biti 0 samo gdje je zadana odgovarajuća izvorna referenca |

**Status:** do dostave konkretne domene, pristupa odgovarajućoj poslovnici/POS-u i referentnih dizajnerskih snimki ovo je *plan provjerljivog audita*, ne potpisana potvrda produkcijske sukladnosti. Ne kopirati tuđi sadržaj, skripte ili datoteke cjenika u BAZNA distribuciju.
