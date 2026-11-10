# BAZNA

### Bazne cijene

![BAZNA — Bazne cijene by Brendigo](wordpress/brendigo-sidrena-cijena/assets/banner-1544x500.png)

<div align="center">

**Jedan sustav cijena. Više prodajnih kanala. Potpuna kontrola nad javnim cjenicima.**

BAZNA povezuje proizvode, usluge, poslovnice, povijesne bazne cijene, javne objave i arhivu — od WordPressa i samostalnih PHP stranica do Joomle, Shopifyja i Androida.

[**Preuzmi izdanje**](https://github.com/bren-wp/BAZNA/releases) · [**Instalacija**](#instalacija-po-platformama) · [**Povezivanje s Androidom**](#jedinstveni-podaci-na-svim-platformama) · [**Dokumentacija**](#dokumentacija)

![Android CI](https://github.com/bren-wp/BAZNA/actions/workflows/android-ci.yml/badge.svg)
![WordPress CI](https://github.com/bren-wp/BAZNA/actions/workflows/official-plugin-ci.yml/badge.svg)
![Multiplatform CI](https://github.com/bren-wp/BAZNA/actions/workflows/multiplatform-ci.yml/badge.svg)

</div>

---

## Sve cijene na jednom mjestu

**BAZNA** pomaže poslovanju javno prikazivati cijene i jednostavno upravljati njihovim promjenama: od jedne poslovnice do kataloga s tisućama proizvoda i usluga. Cjenik nastaje iz provjerene evidencije, objavljuje se izričitom radnjom i zadržava povijest prethodnih objava.

<img src="wordpress/brendigo-sidrena-cijena/assets/icon-256x256.png" width="80" alt="BAZNA — ikona dodatka" />

- **Proizvodi i usluge** — aktualne cijene, jedinice mjere, SKU, barkod i dostupnost.
- **Bazne cijene** — povijesni iznos i datum povezani s izvornom evidencijom.
- **Poslovnice** — zasebni katalozi, javne objave i povijesni cjenici po lokaciji.
- **Javni cjenici** — pregledne HTML stranice te strojno čitljivi CSV/XML.
- **Arhiva** — dostupne nepromjenjive verzije već objavljenih cjenika.
- **Licence** — zaštita administrativnih funkcija domenskim licencnim sustavom.

## Jedan BAZNA ekosustav

| Platforma | Namjena | Verzija | Status |
|---|---|---|---|
| **WordPress + WooCommerce** | Upravljanje cjenicima, postojećim WordPress podacima, javnim objavama i arhivom | **1.5.14** | CI provjeren; kandidat WordPress.org paketa |
| **Web PHP** | Samostalni sustav bez WordPressa, grafički editor, lokalni JSON katalog, objava i arhiva | **1.1.0** | Funkcionalni paket; potreban test na stvarnom hostingu |
| **Joomla** | System plugin sa zajedničkim editorom, ACL/CSRF kontrolama, cjenicima i licencama | **1.1.0** | Kandidat; potreban instalacijski test u Joomli |
| **Shopify** | Zaseban PHP adapter s potpisanim app-proxyjem, uvozom EUR varijanti, theme blokom i privacy webhookovima | **1.1.0** | Custom-store kandidat; javni OAuth/App Store postupak nije dovršen |
| **Android** | Siguran javni pregled proizvoda, usluga, poslovnica i arhive s podržanih BAZNA stranica | **1.5.14** | Testni APK; AAB bez produkcijskog potpisa |
| **Windows** | Lokalna obrada kataloga, povijesti cijena i izvoz s Portable/Setup paketima | **0.10.6** | Windows CI i stvarne provjere sučelja |
| **Licencni servis** | Provjera licence vezane uz javnu HTTPS domenu, odvojeno od podataka kataloga | **1.5.14** | Za instalaciju potrebna vlastita privatna konfiguracija |

## Jedinstveni podaci na svim platformama

Android koristi **javni BAZNA API v1** i ne traži administratorsku lozinku ni licencni ključ. Proizvodi, usluge i poslovnice prikazuju se iz *posljednje objavljenog* cjenika — ne iz neprovjerenog nacrta.

~~~text
WordPress / WooCommerce ─┐
BAZNA Web PHP ────────────┤
Joomla ───────────────────┼──> BAZNA javni API v1 ──> Android (samo čitanje)
Shopify app-proxy ────────┘             │
                                       ├──> HTML / CSV / XML
                                       └──> javna arhiva po poslovnici

BAZNA licencni servis ──> provjera instalacije i domene (zasebno)
~~~

| Platforma | Otkriće objavljenih cijena |
|---|---|
| Web PHP i Joomla | `https://primjer.hr/bazna/v1/cjenici` |
| Shopify | `https://trgovina.myshopify.com/apps/bazna/v1/cjenici` |
| WordPress | Postojeća BAZNA REST v1 putanja, podržana i u pretty i u plain-permalink načinu |

**Važno:** zajednički javni format i domenska licenca ne znače da svaki CMS ima identične administracijske mogućnosti. Shopify trenutačno treba zaseban server i autoriziranu instalaciju, a Android je namjerno **samo za čitanje**. Svaki kanal objavljuje vlastiti katalog; automatska dvosmjerna sinkronizacija svih trgovina nije implementirana bez dodatne autentificirane integracije.

## Instalacija po platformama

### WordPress / WooCommerce

1. Na [GitHub izdanjima](https://github.com/bren-wp/BAZNA/releases) preuzmite BAZNA WordPress ZIP; poseban paket za WordPress.org izdan je kao kandidat.
2. Instalirajte dodatak u WordPressu, otvorite **BAZNA**, konfigurirajte katalog i, ako koristite licenciranu distribuciju, aktivirajte licencu.
3. Provjerite cijene, referentne datume i objavite javne CSV/XML cjenike.
4. Android povežite HTTPS adresom svoje stranice.

### Samostalni PHP web — `/web`

1. Preuzmite **BAZNA-Web-1.1.0.zip**. Ne treba WordPress.
2. Potrebni su PHP 8.1+, HTTPS, pravila preusmjeravanja na `public/index.php` i **privatna mapa izvan webroota**.
3. Na serveru konfigurirajte `BAZNA_PUBLIC_URL`, `BAZNA_PRIVATE_DIR`, `BAZNA_ADMIN_PASSWORD_HASH` i `BAZNA_APP_KEY`. Ključeve nemojte unositi u JavaScript ni u Git.
4. Otvorite `/bazna/admin`, aktivirajte licencu i preko kartica **Proizvodi**, **Usluge** i **Poslovnice** unesite podatke. Objavite ih kroz **Javni cjenici**.

[**Potpune PHP upute**](web/README.md)

### Joomla — `/joomla`

1. Preuzmite **BAZNA-Joomla-Plugin-1.1.0.zip** i instalirajte ga kroz **System → Install → Extensions**.
2. Omogućite *System – BAZNA*, konfigurirajte privatnu pohranu i licencni servis.
3. Otvorite BAZNA administratorski prikaz i koristite iste osnovne forme kao u samostalnom PHP sustavu.
4. Objavite katalog i povežite Android javnom HTTPS domenom.

[**Potpune Joomla upute**](joomla/README.md)

### Shopify — `/shopify`

Shopify **ne izvršava PHP u temi**. BAZNA koristi zaseban PHP backend povezan s autoriziranom trgovinom preko **potpisanog app-proxyja**. Dostupan je Shopify theme app extension za javni cjenik te serverski uvoz varijanti preko Shopify Admin API-ja.

Za objavu su potrebni registrirana trgovina, Shopify vjerodajnice, privatna pohrana i valjana BAZNA licenca. Shopify usporedna cijena nije dokaz povijesne bazne cijene. **Javna Shopify App Store distribucija još nije spremna:** potreban je potpuni OAuth postupak, trgovinski UI i preostale provjere aplikacije.

[**Potpune Shopify upute**](shopify/README.md)

### Android i Windows

Android se povezuje sa stvarnim javnim katalogom preko HTTPS-a. APK služi za instalaciju i testiranje; **nepotpisani AAB nije spreman za Google Play** bez odgovarajućeg produkcijskog potpisa. Windows izdanje sadrži Portable.exe i Setup.exe iz stvarnog Windows builda.

[Android upute](android/README.md) · [Windows upute](desktop/README.md)

## Stvarni ekrani i provjere sučelja

Ne koristimo izmišljene „screenshotove aplikacije”. Sljedeći GitHub Actions postupci spremaju **stvarne slike provedenih testova**, dostupne u dijelu *Artifacts* dok traje GitHubovo razdoblje zadržavanja:

| Platforma | Stvarni dokaz |
|---|---|
| [Android — 12 ekrana × 9 konfiguracija](https://github.com/bren-wp/BAZNA/actions/workflows/android-ci.yml) | Compose emulator prikazi i provjere različitosti zaslona |
| [WordPress — responzivnost](https://github.com/bren-wp/BAZNA/actions/workflows/official-plugin-ci.yml) | Javni cjenik i administratorske kartice na 13 širina |
| [Web i Joomla — 182 prikaza](https://github.com/bren-wp/BAZNA/actions/workflows/multiplatform-ci.yml) | 14 prikaza × 13 širina; stvarni predlošci i zajednički editor |
| [Windows — 10 prikaza](https://github.com/bren-wp/BAZNA/actions/workflows/desktop-ci.yml) | Stvarno WPF sučelje i provjere navigacije |

Prolazak responzivnih i funkcionalnih testova **nije dokaz 0 % vizualnog odstupanja** od referenci. Za pixel-perfect potvrdu trebaju odgovarajući izvorni PNG-ovi, ista razlučivost i automatizirana usporedba.

## Hrvatski propisi, povijesne cijene i sigurnost

BAZNA čuva razliku između **aktualne cijene**, **povijesne bazne cijene** i **najniže cijene u 30 dana prije sniženja**. Ne izvodi baznu cijenu iz današnjeg iznosa ni Shopifyjeve usporedne cijene. Povijesni iznos zahtijeva stvarni datum i potvrdu dokaza.

Ugrađeni pravni profil priprema objavu za hrvatski regulatorni okvir i predviđeni početak proširene primjene **17. studenoga 2026.** Konačne propise i evidenciju treba zasebno provjeriti; softver ne predstavlja pravni certifikat trgovčevih cijena.

Zaštite uključuju HTTPS javno čitanje, privatno čuvanje licenci, administratorske CSRF/ACL kontrole, atomsku pohranu, zaštitu od zastarjelih izmjena i provjeru Shopify potpisanih zahtjeva. Sigurnosni audit u CI-ju nije zamjena za produkcijski penetracijski test.

## Dokumentacija

[Arhitektura i kompatibilnost](docs/BAZNA-MULTIPLATFORM-ARCHITECTURE.md) · [Pravni okvir](docs/LEGAL-2026-INTEROPERABILITY.md) · [Android i WordPress](docs/ANDROID-WORDPRESS-INTEGRATION-AUDIT-159.md) · [Vizualni standard](docs/PIXEL-PERFECT-QA.md) · [Sigurnosni audit](docs/SECURITY-DEADCODE-AUDIT-2026-10-24.md) · [Licence](licenca/README.md)

---

<div align="center">

**BAZNA by Brendigo**  
*Cijene pod kontrolom. Poslovanje s povjerenjem.*

[brendigo.com](https://brendigo.com/) · [GitHub izdanja](https://github.com/bren-wp/BAZNA/releases)

</div>
