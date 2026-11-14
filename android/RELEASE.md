# BAZNA 1.5.21 — službeni izvorni paketi i Google Play provjere

| Komponenta | Verzija | Napomena |
|---|---|---|
| Android | **1.5.21**, code **24** | `com.brendigo.bazna` · targetSdk 36 · minSdk 26 |
| WordPress / WooCommerce | **1.5.21** | Zadržani svi postojeći podaci i REST v1 |
| Web PHP, Joomla, Shopify | **1.1.4** | Zajednički Core, javni HTML i filtrirani REST |
| Windows | **0.10.7** | Isporučuje se u istom releaseu nakon smoke testova |

## Novo: ažuriranja postojećeg WordPress.org dodatka

GitHub izdanje priprema provjereni BAZNA WordPress ZIP s nepromijenjenim identitetom `brendigo-sidrena-cijena`. WordPress.org SVN nadogradnja zahtijeva valjane committer vjerodajnice i GitHub Actions objavu, zasebno od GitHub Release paketa. Postojeće postavke, cjenici, arhiva i REST v1 ostaju netaknuti.

[Upute za objavu na WordPress.org](../docs/WORDPRESS-ORG-AZURIRANJE.md)

## Novo: isti javni prikaz na svim platformama

WordPress zadržava javni HTML cjenik i proširuje REST v1 pregled
usluga „Cijena na upit” filtrima `outlet` i `q`.
Web, Joomla i Shopify koriste istu PHP funkciju za HTML prikaz
**posljednje objavljenih** proizvoda, numeričkih usluga i usluga
s cijenom na upit. PHP/Web omogućuje odabir poslovnice i pretragu
po nazivu, šifri, marki i barkodu, uz kriterije za dogovorne cijene.
Shopify i dalje zahtijeva valjani potpisani proxy zahtjev.

Androidova opća pretraga sada prikazuje i usluge na upit kao
zasebne kartice s kriterijima. Nikad im ne dodaje lažnu cijenu.
Filtri „Sve / Proizvodi / Usluge” i poslovnica primjenjuju se na
stvarno objavljene podatke.

## Stabilnost i kompatibilnost

Ostaju netaknuti administratorski računi i hashovi, WooCommerce
podaci, povijesni iznosi, arhive i sve prethodne REST v1 rute.
Ova verzija ne tvrdi da Joomla ili Shopify izvode WP/WooCommerce
funkcije: usklađena je javna semantika, a administracija ostaje
autentificirana i platformno specifična.

## Android potpisivanje i Google Play

GitHub izdanje isporučuje **nepotpisani release AAB** i **debug APK**.
Potrebno je potpisati AAB privatnim upload ključem, popuniti stvarnu
Data safety izjavu, objaviti aplikaciji specifičnu politiku
privatnosti i proći Play Console reviziju. CI testovi nisu
zamjena za Googleovo odobrenje, pravno mišljenje ili pixel-perfect
certifikat.

[Podudarnost s MJDD 1.26.0](../docs/BAZNA-FEATURE-PARITY-MJDD-1.26.0.md)
 · [Play Checklist](../docs/ANDROID-GOOGLE-PLAY-CHECKLIST.md)
 · [Sva izdanja](https://github.com/bren-wp/BAZNA/releases)

## Bez licenciranja i bez pozivanja aktivacijskog servera

BAZNA WordPress, Web PHP, Joomla i Shopify sada ne zahtijevaju BAZNA licencni ključ. Android ne učitava ni prikazuje status licence; stare REST v1 objave s poljem `licensing` sigurno ignorira. Windows Setup/Portable 0.10.7 ne traže aktivaciju. Ovlasti administratora, CSRF, Shopify HMAC i Android HTTPS ostaju sigurnosne kontrole. Distribucijska GPL licenca izvornog koda ostaje valjana.
