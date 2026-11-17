# BAZNA za Drupal 10.3 / 11

Početni nativni Drupal modul. Koristi isti **BAZNA PHP Core** kao Joomla,
Web i Shopify za validaciju povijesnih cijena, javne v1 API podatke, HTML
cjenike i propisani redoslijed službenih CSV/XML polja iz NN 132/2026.

## Instalacija

Kopirajte direktorij modula `bazna/` iz ZIP-a u `web/modules/custom/`
(ili u `modules/custom/` ovisno o Drupal instalaciji). Uključite modul u
`Extend` i posjetite `/admin/config/services/bazna`.

Potrebno je zasebno postaviti okolišne varijable:

- `BAZNA_DRUPAL_PRIVATE_DIR` — apsolutna putanja privatne mape izvan
  document root direktorija, s odgovarajućim pravima pristupa.
- `BAZNA_DRUPAL_PUBLIC_URL` — javni HTTPS URL stranice (bez zadnje kose crte).
- `BAZNA_TRADER_OIB` (pravna osoba) **ili** `BAZNA_TRADER_MB` (fizička osoba),
  a ne oba, za službene izvoze.

Javne rute: `/bazna/v1/cjenici`, `/bazna/v1/cjenici/proizvodi/csv`,
`/bazna/v1/sluzbeni/proizvodi/csv`, `/bazna/v1/sluzbeni/usluge/xml`,
`/bazna/v1/arhiva`, `/bazna/v1/usluge-na-upit` i `/bazna/cjenik`.
Sve javne rute čitaju **samo objavljeni** snapshot bez privilegija pisanja.

## Sigurnosni i funkcionalni status

- Javne rute su GET i zahtijevaju samo pravo `access content`.
- Administratorski obrazac zahtijeva Drupal pravo `administer site configuration`
  i Drupal Form API token, dok se podatkovne vrijednosti provjeravaju kroz Core.
- Izvoz pravnog profila nije certifikat trgovca niti zamjena za dokumentiranu
  povijesnu cijenu; stari API stupci ostaju radi Android kompatibilnosti.
- **Ograničenje:** instalacija i administrativni UI nisu testirani na stvarnom
  Drupalu; puna vizualna i funkcionalna podudarnost s WordPressom nije potvrđena.
  Nije implementirana native Commerce uvoz/sinkronizacija niti je modul objavljen
  u službenom Drupal katalogu.

Izvorni propisi:
https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html
https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html
