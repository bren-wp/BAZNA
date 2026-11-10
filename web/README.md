# BAZNA Web — samostalni PHP sustav

**Autor:** Brendigo · https://brendigo.com/  
**Status:** prva funkcionalna inačica; nije objavljena niti testirana na stvarnom produkcijskom hostingu.

BAZNA Web pruža administratorsku prijavu, uređivanje kataloga i poslovnica, zasebnu provjeru/licenciranje, objavu javnih HTML/CSV/XML cjenika i nepromjenjivu arhivu. Android čita isti **BAZNA public v1** oblik kao kod WordPressa.

## Instalacija (Apache / PHP 8.1+)

1. Raspakirajte \`BAZNA-Web-1.1.0.zip\` tako da imate \`src/\` i \`public/\`.
2. Postavite virtualni host DocumentRoot na **samo** \`/your-path/bazna-web/public\` (nikada na korijen paketa). Potrebni su PHP \`openssl\`, \`curl\`, \`dom\`, \`json\` i \`mbstring\` po potrebi.
3. Postavite server-side environment variables (nginx fastcgi_param / PHP-FPM environment / Apache SetEnv, ne klijentski JavaScript):

   \`BAZNA_PRIVATE_DIR=/srv/bazna-private/your-site\` — izvan webroota; mora biti zapisiva.

   \`BAZNA_PUBLIC_URL=https://www.primjer.hr\` — kanonska HTTPS adresa javne web-stranice; nikada ne čitati nekontrolirani \`Host\` header.

   \`BAZNA_ADMIN_PASSWORD_HASH=<password_hash(PASSWORD_DEFAULT) output>\` — lozinka nikad u Git.

   \`BAZNA_APP_KEY=<64 hexadecimal characters from random_bytes(32)>\` — služi za AES-256-GCM lokalnu zaštitu licence; čuvati sigurno kroz nadogradnje.

4. Generirajte vrijednosti na poslužitelju:
   \`php -r 'echo password_hash(readline("Password: "), PASSWORD_DEFAULT), PHP_EOL;'\`
   \`php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'\`
5. Omogućite Apache rewrite ili ekvivalentno usmjerite \`/bazna/*\` na \`web/public/index.php\`. Poslužitelj mora odbijati HTTP ili preusmjeravati na HTTPS **prije PHP-a**.
6. Otvorite \`https://www.primjer.hr/bazna/admin\`, aktivirajte domenski vezanu BAZNA licencu i unesite katalog. Objavite eksplicitno preko kartice **Javni cjenici**.

## Zajednički Android API

\`GET /bazna/v1/cjenici\` — discovery, pravni profil, poslovnice, RSS-like status objave.

\`GET /bazna/v1/cjenici/proizvodi/csv\` i \`/usluge/csv\` — stvarno objavljeni javni CSV.

\`GET /bazna/v1/cjenici/proizvodi/xml\` i \`/usluge/xml\` — javni XML.

\`GET /bazna/v1/arhiva\` — nepromjenjive arhivske poveznice; \`GET /bazna/v1/license-proof\` — kratkotrajan izazov bez ključa.

\`GET /bazna/cjenik\`, \`GET /bazna/arhiva\` — javni responzivni HTML.

Android prihvaća **samo isti HTTPS host**, ne treba administratorsku prijavu ni licencni ključ. REST identitet, datum objave i privatni administrativni podaci ostaju odvojeni. Stari naziv stupca \`sidrena_cijena\` ostaje isključivo tehnička kompatibilnost sa starim Androidom i WP CSV-om; posjetitelji vide **Bazna cijena**.

## Sigurnost i zakonska točnost

Povijesna bazna cijena zahtijeva \`historical_verified: true\` i stvarni datum. Nikad se ne izvodi iz današnje redovne ili Shopify usporedne cijene. Pravila iz NN 101/2026 i 110/2026 imaju budući datum primjene 17. 11. 2026.; pravni profil ne certificira trgovca.

Objave su eksplicitne, stare objave ostaju u arhivi bez automatskog brisanja; lozinke/ključevi nisu u javnom API-ju. CSRF, sigurne sesije, ograničenja veličine i domene su u kodu. Prije produkcije provesti instalacijske, penetracijske, pravne i backup/restore testove.

## Brzi test

\`php web/tests/test.php\` iz korijena repozitorija. Test koristi lokalnu privremenu mapu i nema mrežne pozive.
