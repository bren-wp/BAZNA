# BAZNA za Shopify — aplikacijski adapter

**Autor:** Brendigo · https://brendigo.com/

Shopify **ne izvodi PHP u temi/trgovini**. Ovdje je zaseban PHP backend s Shopify HMAC provjerenim **app-proxy** zahtjevima, izoliranim podacima po trgovini, server-side GraphQL Admin sinkronizacijom i theme app extension blokom za javni cjenik. Ovo nije gotova javno distribuirana Shopify App Store aplikacija.

## Shopify konfiguracija

1. U Shopify Dev Dashboard stvorite i instalirajte vlastitu BAZNA aplikaciju. Koristite najmanji potreban API pristup: \`read_products\`; \`write_app_proxy\` samo ako konfiguracija to zahtijeva. Za trgovine nastale nakon 2026. pristupni token nije trajni token koji se kopira iz klasičnog Shopify admina: koristite dokumentirani Dev Dashboard token flow / klijentske vjerodajnice.
2. Prilagodite \`shopify.app.toml.example\`, registrirajte app proxy na
   \`https://SHOP.myshopify.com/apps/bazna\` → \`https://YOUR-APP-HOST/shopify/proxy\`.
3. Deployajte \`shopify/server/index.php\` na HTTPS aplikacijski poslužitelj. PHP ruta na ovom poslužitelju mora očuvati \`QUERY_STRING\` koji Shopify potpisuje, a \`BAZNA_SHOPIFY_BACKEND_PATH\` mora odgovarati stvarnoj backend putanji.
4. Postavite SERVER-ONLY environment:
   - \`BAZNA_SHOPIFY_CLIENT_SECRET\` — Shopify Client Secret za HMAC;
   - \`BAZNA_SHOPIFY_ALLOWED_SHOPS=shop.myshopify.com\` — samo autorizirano instalirane trgovine (ne prihvaćati slobodan unos od anonimnog kupca);
   - \`BAZNA_SHOPIFY_PRIVATE_DIR=/srv/bazna-shopify-private\` izvan webroota;
   - \`BAZNA_SHOPIFY_APP_KEY\` — zaseban 64-znamenkasti hex za AES-GCM;
   - \`BAZNA_SHOPIFY_ACCESS_TOKEN\` — samo tijekom autoriziranog sync CLI poziva; ne smije se pojaviti u logu, Liquid kodu ili HTTP odgovoru.
5. Server-side komande (CLI, ne otvoreni web):
   \`BAZNA_LICENSE_KEY=... php shopify/bin/manage.php shop.myshopify.com activate\`
   \`php shopify/bin/manage.php shop.myshopify.com sync\`
   \`php shopify/bin/manage.php shop.myshopify.com publish\`
6. Dodajte Shopify Theme App Extension blok \`BAZNA javni cjenik\`. Javni Android API je \`https://shop.myshopify.com/apps/bazna/v1/cjenici\`.

## Ista pravila kao WordPress / PHP / Joomla

- HMAC provjera **svakog** proxy poziva, kontrola vremena, whitelist instaliranih trgovina, segregacija po vlastitom shop identitetu.
- Licenca se veže za \`shop.myshopify.com\` domenu preko potvrđene Shopify proxy putanje \`/apps/bazna/v1/license-proof\` i postojećeg centralnog BAZNA servisa.
- GraphQL varijante u EUR se uvoze kao **nacrt**. Shopify \`compareAtPrice\` nije povijesna bazna cijena; ne uvozi se automatski. Prije objave treba unijeti i dokazima potvrditi povijesnu baznu cijenu.
- Android prima isti \`api_version: 1\`, HTTPS CSV feed, arhivu i pravni profil.

## Granice / produkcijska provjera

Ovo je **custom-store integracijski kostur**, ne gotov Shopify embedded/OAuth marketplace app. Nisu implementirani kompletna autorizacija za više proizvoljnih trgovaca, automatsko sigurno spremanje i rotacija tokena, obvezni Shopify privacy webhooks, fakturiranje preko Shopify Billing API-ja, puni embedded merchant UI, sinkronizacija u stvarnom vremenu ili pregled Apps listinga. Ne distribuirati u Shopify App Store bez toga i odgovarajuće autorizacije.

Shopify može omogućiti trgovcu promjenu URL-a app proxyja; u tom slučaju mora se uskladiti javni URL u konfiguraciji i Androidu. Podržana tvornička putanja je \`/apps/bazna\`. Ne otkrivajte \`BAZNA_SHOPIFY_CLIENT_SECRET\` niti ključ licence frontend skriptama.

Test: \`php shopify/tests/test_proxy.php\`.
