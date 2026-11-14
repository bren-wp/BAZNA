# BAZNA — jedinstveni sustav za WordPress, PHP, Joomla, Shopify i Android

## Arhitektura

- \`wordpress/\`: postojeći WooCommerce + standalone WP dodatak, produkcijski testirani WP API v1. **Ne mijenja se baza korisničkih cijena niti javni legacy REST namespace.**
- \`web/\`: BAZNA Core PHP 8.1+ bez frameworka, lokalni JSON, login, eksplicitno objavljivanje, CSV/XML/HTML i nepromjenjiva arhiva.
- \`joomla/\`: Joomla 5/6 system plugin koji uz sigurnosne Joomla ACL/CSRF kontrole ugrađuje isti Web Core i nudi javni API + admin kartice.
- \`shopify/\`: izolirani autorizirani shop app-proxy, HMAC, GraphQL varijante, CLI publikacija i Shopify Theme App Extension. Shopify trgovina ne izvršava PHP; PHP radi na BAZNA app serveru.
- \`android/\`: read-only aplikacija, automatski pokušava postojeći WordPress pretty/plain endpoint, zatim novu PHP/Joomla rutu i Shopify app-proxy rutu. Nikad se ne prijavljuje na CMS i ne traži licencne ključeve.
- **Bez licencnog servisa:** prethodna mapa `licenca/`, domensko vezivanje, aktivacijski ključevi i periodična provjera uklonjeni su u 1.5.20. Sigurnosne kontrole za pristup uređivanju i potpisani Shopify zahtjevi nisu uklonjeni.

## Javna podatkovna shema i kompatibilnost

\`GET /bazna/v1/cjenici\`: \`api_version=1\`, \`platform\`, \`connection.mode=public_read_only\`, \`legal\`, \`feeds.proizvodi/usluge.csv\`, \`outlets[].feeds\`, \`type_archive\`, \`publication\`.

\`GET /bazna/v1/cjenici/{proizvodi|usluge}/csv\`: UTF-8 BOM, semicolon CSV, odvojeni redovi. Tehnički stupci \`sidrena_cijena\`/\`sidrena_datum\` zadržani su radi Android/WordPress kompatibilnosti; nisu vidljivo javno brendiranje. Na ekranima je samo **BAZNA/Bazna cijena**.

\`GET /bazna/v1/arhiva\`: \`date, kind, outlet, format, url\`, samo javni HTTPS same-origin. Neobjavljene izmjene nikad nisu objavljene automatski.

| Svojstvo | WordPress | Web PHP | Joomla | Shopify | Android |
|---|---|---|---|---|---|
| Javni API v1 | Da | Da | Da | App proxy | Čita |
| Proizvodi i usluge | Da | Grafički editor + JSON uvoz | Grafički editor + JSON uvoz | Shopify varijante + ručne usluge | Čita |
| Poslovnice | Da | Da | Da | Ručno | Čita |
| CSV/XML | Da | Da | Da | Da | CSV čita |
| Arhiva | Da | Da | Da | Da | Čita |
| Izvorne cijene u CMS-u | WooCommerce | Lokalni podaci | Lokalni podaci | GraphQL EUR | Ne uređuje |
| Produkcijski E2E verificiran | WP CI | NE | NE | NE | Android CI |

## Zahtjevi za dovršetak produkcijske paritete

1. Provjeriti sve tri platforme na stvarnoj instalaciji/hostingu i testirati HTTPS, administratorsku prijavu, deinstalaciju, migraciju i oporavak podataka.
2. Joomla: dovršiti nativnu Joomla MVC komponentu i integracije stvarnih prodajnih sustava; dijeljeni grafički editor već radi za proizvode, usluge i poslovnice.
3. Shopify: dovršiti Shopify CLI OAuth/App Bridge token lifecycle, marketplace billing/instalaciju, pravi embedded UI i više autoriziranih trgovina. HMAC GDPR/CCPA webhook obrada i provjere brisanja već su implementirane.
4. Regresijska pixel-diff provjera svih WP/Android/Joomla/Shopify zaslona u odnosu na originalne reference. Web/Joomla CSS i zajednički editor već prolaze 195 stvarnih Chrome snimki u 13 veličina. **CI layout/smoke test nije dokaz 0% vizualnog odstupanja.**
5. Pravni review službenih propisa i svih budućih konačnih pravilnika; necertificirane cijene neće automatski postati zakonski usklađene.

## Sigurnost

Svi javni endpointi su read-only, svih pet kanala koriste HTTPS i objavljene snimke, a write sučelja su autentificirana i CSRF zaštićena. Ključevi i korisnički podaci nisu dio javnog REST JSON-a. Shopify App Proxy potpis **ne zamjenjuje** OAuth dokaz instalacije za proizvoljnu trgovinu — zato backend zahtijeva i lokalnu eksplicitnu listu dopuštenih trgovina.

## Bez aktivacijskog ključa (1.5.20)

WordPress, Web, Joomla, Shopify, Android i Windows rade bez BAZNA aktivacije, licencnog ključa ili vanjskog licencnog servera. Za uređivanje ostaju WordPress/Joomla ACL, PHP administratorska lozinka i CSRF; Shopify zahtijeva HMAC i autoriziranu trgovinu, a Android HTTPS. Windows installer ne traži komercijalni EULA pristanak. Izvorne GPL/autorske obveze ostaju valjane.
