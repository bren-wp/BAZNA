# BAZNA 1.5.19 — Android, WordPress i adapteri

- **Android 1.5.19**: `com.brendigo.bazna`, `versionCode=22`,
  `targetSdk=36`, `minSdk=26`, `compileSdk=37`.
- **WordPress 1.5.19**: WooCommerce / standalone katalog, poslovnice,
  HTML, CSV/XML, arhiva, usluge s cijenom na upit.
- **Web PHP / Joomla / Shopify 1.1.3**: zajednička javna shema i
  objavljivanje opisnih usluga bez unaprijed utvrđenog iznosa.
- **Windows**: postojeća desktop verzija uključena u zajedničko izdanje.

## Ujednačen javni REST v1

Opcionalna `on_request` poveznica u katalogu vodi na javni popis
usluga koje imaju `id`, `name`, `criteria`, `outlet`. Usluge
ne nose brojčani iznos niti se pretvaraju u numerički CSV/XML.
WordPress stvara neautoloadani snapshot nakon uspješne javne objave;
PHP/Joomla/Shopify čitaju objavljeni, ne uređivački katalog.
Android provjerava JSON tipove, poslovnice i HTTPS istog izvora.
Stariji poslužitelji bez nove poveznice i dalje rade.

## Google Play i ograničenja

Unsigned AAB se mora potpisati ispravnim upload ključem.
Potrebno je provjeriti Play Console Data safety izjave, stvarne SDK-ove,
privatnost i dostupnost BAZNA-specifične politike privatnosti.
CI nije Google Play odobrenje ni potvrda pravne usklađenosti.
Android ostaje isključivo javni klijent; ne dijeli WooCommerce
administratorske ovlasti ni korisničke lozinke s WP dodatkom.
WooCommerce porezni obračun je specifičan za WordPress; ostali
adapteri razmjenjuju samo objavljene konačne iznose.

[Usporedba značajki](../docs/BAZNA-FEATURE-PARITY-MJDD-1.26.0.md)
 · [Play provjere](../docs/ANDROID-GOOGLE-PLAY-CHECKLIST.md)
 · [Izdanja](https://github.com/bren-wp/BAZNA/releases)
