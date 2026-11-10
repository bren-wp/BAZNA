# BAZNA — jedinstveni sustav za WordPress, PHP, Joomla, Shopify i Android

## Arhitektura

- \`wordpress/\`: postojeći WooCommerce + standalone WP dodatak, privatno licenciranje, produkcijski testirani WP API v1. **Ne mijenja se baza korisničkih cijena niti javni legacy REST namespace.**
- \`web/\`: BAZNA Core PHP 8.1+ bez frameworka, lokalni JSON, login, licence, eksplicitno objavljivanje, CSV/XML/HTML i nepromjenjiva arhiva.
- \`joomla/\`: Joomla 5/6 system plugin koji uz sigurnosne Joomla ACL/CSRF kontrole ugrađuje isti Web Core i nudi javni API + admin kartice.
- \`shopify/\`: izolirani autorizirani shop app-proxy, HMAC, GraphQL varijante, CLI publikacija i Shopify Theme App Extension. Shopify trgovina ne izvršava PHP; PHP radi na BAZNA app serveru.
- \`android/\`: read-only aplikacija, automatski pokušava postojeći WordPress pretty/plain endpoint, zatim novu PHP/Joomla rutu i Shopify app-proxy rutu. Nikad se ne prijavljuje na CMS i ne šalje licencne ključeve.
- \`licenca/\`: jedan centralni BAZNA servis s kanonskim domenskim vezivanjem, HTTPS provjerom izazova, DNS-pin/tls SSRF zaštitom, lokalnim AES-GCM klijentom i grace periodom od 72 sata.

## Javna podatkovna shema i kompatibilnost

\`GET /bazna/v1/cjenici\`: \`api_version=1\`, \`platform\`, \`connection.mode=public_read_only\`, \`legal\`, \`feeds.proizvodi/usluge.csv\`, \`outlets[].feeds\`, \`type_archive\`, \`publication\`.

\`GET /bazna/v1/cjenici/{proizvodi|usluge}/csv\`: UTF-8 BOM, semicolon CSV, odvojeni redovi. Tehnički stupci \`sidrena_cijena\`/\`sidrena_datum\` zadržani su radi Android/WordPress kompatibilnosti; nisu vidljivo javno brendiranje. Na ekranima je samo **BAZNA/Bazna cijena**.

\`GET /bazna/v1/arhiva\`: \`date, kind, outlet, format, url\`, samo javni HTTPS same-origin. Neobjavljene izmjene nikad nisu objavljene automatski.

| Svojstvo | WordPress | Web PHP | Joomla | Shopify | Android |
|---|---|---|---|---|---|
| Javni API v1 | Da | Da | Da | App proxy | Čita |
| Proizvodi i usluge | Da | JSON urednik | JSON urednik | Shopify varijante + ručne usluge | Čita |
| Poslovnice | Da | Da | Da | Ručno | Čita |
| CSV/XML | Da | Da | Da | Da | CSV čita |
| Arhiva | Da | Da | Da | Da | Čita |
| Domenski licencni ključ | Da | Da | Da | Proxy + CLI | Ne treba ključ |
| Izvorne cijene u CMS-u | WooCommerce | Lokalni podaci | Lokalni podaci | GraphQL EUR | Ne uređuje |
| Produkcijski E2E verificiran | WP CI | NE | NE | NE | Android CI |

## Zahtjevi za dovršetak produkcijske paritete

1. Provjeriti sve tri platforme na stvarnoj instalaciji/hostingu i testirati HTTPS, licencne izazove, deinstalaciju, migraciju i oporavak podataka.
2. Joomla: puni komponentni grafički editor bez JSON textarea i native integracije iz stvarnih prodajnih sustava.
3. Shopify: Shopify CLI OAuth/App Bridge token lifecycle, GDPR/CCPA webhooks, marketplace billing/instalacija, pravi embedded UI i multiple shops.
4. Regresijska pixel-diff provjera svih WP/Android/Joomla/Shopify zaslona u odnosu na originalne reference. **CI layout/smoke test nije dokaz 0% vizualnog odstupanja.**
5. Pravni review službenih propisa i svih budućih konačnih pravilnika; necertificirane cijene neće automatski postati zakonski usklađene.

## Sigurnost

Svi javni endpointi su read-only, svih pet kanala koriste HTTPS i objavljene snimke, a write sučelja su autentificirana i CSRF zaštićena. Ključevi i korisnički podaci nisu dio javnog REST JSON-a. Shopify App Proxy potpis **ne zamjenjuje** OAuth dokaz instalacije za proizvoljnu trgovinu — zato backend zahtijeva i lokalnu eksplicitnu listu dopuštenih trgovina.
