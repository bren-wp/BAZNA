# BAZNA Web — samostalni PHP sustav

**Autor:** Brendigo · https://brendigo.com/  
**Status:** prva funkcionalna inačica; nije objavljena niti testirana na stvarnom produkcijskom hostingu.

BAZNA Web 1.1.4 pruža administratorsku prijavu i **grafičko uređivanje proizvoda, usluga i poslovnica**, pojedinačno spremanje i uklanjanje s provjerom konkurentnih izmjena, zasebnu domensku licencu, javne HTML/CSV/XML cjenike i nepromjenjivu arhivu. Devet administratorskih kartica koristi stvarne obrasce; JSON je zadržan samo za napredni uvoz/izvoz. Android čita isti **BAZNA public v1** oblik kao kod WordPressa.

## Instalacija (Apache / PHP 8.1+)

1. Raspakirajte \`BAZNA-Web-1.1.4.zip\` tako da imate \`src/\` i \`public/\`.
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

Objave su eksplicitne, stare objave ostaju u arhivi bez automatskog brisanja. Zajedničko zaključavanje između procesa sprečava gubitak arhivskih verzija pri istodobnim objavama; obrazac odbija zastarjelu izmjenu retka pomoću SHA-256 identiteta prethodnog zapisa; lozinke/ključevi nisu u javnom API-ju. CSRF, sigurne sesije, ograničenja veličine i domene su u kodu. Prije produkcije provesti instalacijske, penetracijske, pravne i backup/restore testove.

## Brzi test

\`php web/tests/test.php\` iz korijena repozitorija. Pokrenite i \`php web/tests/test_editor.php\`. Testovi koriste lokalnu privremenu mapu i nemaju mrežne pozive. Chrome CI snima 14 Web/Joomla prikaza na 13 veličina (182 snimke), a to nije dokaz podudaranja piksela s izvornim dizajnom.

## Usluge s cijenom na upit (v1.1.4)

Nova javna REST v1 ruta `/bazna/v1/usluge-na-upit` vraća samo
objavljene `id`, `name`, `criteria` i `outlet`, bez iznosa i
bez administratorskih identiteta. Izmjene u nacrtu nisu javne dok
se katalog ne objavi. Zahtjevi su odvojeni od brojčanih CSV/XML
cjenika. Na javnoj HTML stranici prikazuju se naziv, kriteriji
i oznaka „Cijena na upit”. Shopify koristi isključivo potpisani
app-proxy: zahtjev upućen izvan valjanog proxy potpisa nije podržan.

## Javna funkcionalna podudarnost — Web PHP 1.1.4

Prikaz sada uključuje stvarno objavljene proizvode i usluge u
responzivnoj tablici sa SKU-om, trenutnom cijenom, baznom cijenom,
referentnim datumom, mjernom jedinicom i dostupnošću. Usluge
**„Cijena na upit”** pojavljuju se odvojeno s kriterijima,
bez izmišljenog broja. HTML se generira iz istog javnog snapshota
kao REST i CSV, ne iz nacrta; korisnički sadržaj se HTML-escapea.

REST `/bazna/v1/usluge-na-upit` opcionalno podržava `outlet`
i `q` za filtriranje po poslovnici i višerječnim kriterijima.
API ostaje samo za čitanje i ne sadrži administratorske podatke.
Na samostalnom Web PHP-u dodatno su dostupni pretraga i odabir poslovnice u javnom HTML-u.
