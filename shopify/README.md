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
   - \`BAZNA_SHOPIFY_ACCESS_TOKEN\` — samo tijekom autoriziranog sync CLI poziva; ne smije se pojaviti u logu, Liquid kodu ili HTTP odgovoru.
5. Server-side komande (CLI, ne otvoreni web):
   \`php shopify/bin/manage.php shop.myshopify.com sync\`
   \`php shopify/bin/manage.php shop.myshopify.com publish\`
6. Dodajte Shopify Theme App Extension blok \`BAZNA javni cjenik\`. Javni Android API je \`https://shop.myshopify.com/apps/bazna/v1/cjenici\`.

## Ista pravila kao WordPress / PHP / Joomla

- HMAC provjera **svakog** proxy poziva, kontrola vremena, whitelist instaliranih trgovina, segregacija po vlastitom shop identitetu.
- Licenca se veže za \`shop.myshopify.com\` domenu preko potvrđene Shopify proxy putanje \`/apps/bazna/v1/license-proof\` i postojećeg centralnog BAZNA servisa.
- GraphQL varijante u EUR se uvoze kao **nacrt**. Shopify \`compareAtPrice\` nije povijesna bazna cijena; ne uvozi se automatski. Prije objave treba unijeti i dokazima potvrditi povijesnu baznu cijenu.
- Android prima isti \`api_version: 1\`, HTTPS CSV feed, arhivu i pravni profil.

## Granice / produkcijska provjera

Ovo je **custom-store adapter kandidat 1.1.4**, ne gotov Shopify embedded/OAuth marketplace app. Obvezni privacy webhook endpointi i pretplate sada su implementirani, s provjerom HMAC potpisa, izolacijom trgovina, revokacijom nakon deinstalacije i brisanjem podataka trgovine na potpisani zahtjev. **Još nisu implementirani** puni OAuth za više trgovaca, automatska sigurna pohrana i rotacija tokena, Shopify Billing API, puni embedded merchant UI, sinkronizacija u stvarnom vremenu ili App Store odobrenje. Ne distribuirati u Shopify App Store bez toga i odgovarajuće autorizacije.

Shopify može omogućiti trgovcu promjenu URL-a app proxyja; u tom slučaju mora se uskladiti javni URL u konfiguraciji i Androidu. Podržana tvornička putanja je \`/apps/bazna\`. Ne otkrivajte \`BAZNA_SHOPIFY_CLIENT_SECRET\` niti ključ licence frontend skriptama.

Testovi: \`php shopify/tests/test_proxy.php\` i \`php shopify/tests/test_webhooks.php\`.

**Privacy webhooks:** Shopify primjer TOML sada registrira \`customers/data_request\`, \`customers/redact\`, \`shop/redact\` i \`app/uninstalled\`. Sve HTTPS putanje \`/shopify/webhooks/*\` moraju na vašem poslužitelju voditi na \`shopify/server/webhooks.php\`; nemojte ih usmjeriti na app proxy. BAZNA ne pohranjuje korisničke Shopify račune. Nakon opoziva trgovina ne smije nastaviti javno čitati podatke preko starog popisa dopuštenih instalacija. Na potvrđen \`shop/redact\` uklanja se privatna mapa samo te trgovine. Trgovac mora zasebno riješiti zakonsku obvezu čuvanja cjenika prije brisanja, ako je primjenjiva.

## Usluge s cijenom na upit (v1.1.4)

Nova javna REST v1 ruta `/bazna/v1/usluge-na-upit` vraća samo
objavljene `id`, `name`, `criteria` i `outlet`, bez iznosa i
bez administratorskih identiteta. Izmjene u nacrtu nisu javne dok
se katalog ne objavi. Zahtjevi su odvojeni od brojčanih CSV/XML
cjenika. Na javnoj HTML stranici prikazuju se naziv, kriteriji
i oznaka „Cijena na upit”. Shopify koristi isključivo potpisani
app-proxy: zahtjev upućen izvan valjanog proxy potpisa nije podržan.

## Javna funkcionalna podudarnost — Shopify 1.1.4

Prikaz sada uključuje stvarno objavljene proizvode i usluge u
responzivnoj tablici sa SKU-om, trenutnom cijenom, baznom cijenom,
referentnim datumom, mjernom jedinicom i dostupnošću. Usluge
**„Cijena na upit”** pojavljuju se odvojeno s kriterijima,
bez izmišljenog broja. HTML se generira iz istog javnog snapshota
kao REST i CSV, ne iz nacrta; korisnički sadržaj se HTML-escapea.

REST `/bazna/v1/usluge-na-upit` opcionalno podržava `outlet`
i `q` za filtriranje po poslovnici i višerječnim kriterijima.
API ostaje samo za čitanje i ne sadrži administratorske podatke.
Shopifyov app proxy i dalje zahtijeva valjan HMAC potpis; nije otvoreni, anonimni API dostupan proizvoljnom Android klijentu.

## Bez BAZNA aktivacije

Komande `status`, `sync` i `publish` rade bez BAZNA licencnog ključa ili servisa. Shopify HMAC, odobrena instalacija i API pristupni token za remote sinkronizaciju ostaju sigurnosne obveze, a ne licenciranje.
