# BAZNA — licencni servis (PHP + JSON, bez SQL-a)

Repozitorij sadrži **kod** za servis. Privatni master ključ i 10.000 aktivacijskih ključeva NISU dio javnog GitHub repozitorija. Čuvajte ih odvojeno i nikada ih ne izlažite HTTP-om.

## Postavljanje na licenca.brendigo.com

1. Preuzmite direktorij **/licenca** iz repozitorija. Kopirajte ga u korijenski direktorij poddomene tako da je URL **https://licenca.brendigo.com/licenca/api.php**. Zahtijevaju se PHP 8.1+, PHP cURL, valjan TLS certifikat i pravo pisanja u privatnu mapu izvan web korijena.
2. Iz privatnog paketa dobivenog od vlasnika uvezite **licenca/private/config.php**. Datoteka sadrži jedan nasumični 256-bitni master secret; bez tog ključa servis ne može provjeravati 10.000 unaprijed izdanih licenci. GitHub sadrži samo `private/config.example.php` za pregled strukture.
3. Provjerite može li PHP pisati u **privatnu direktorijsku putanju izvan web korijena**. Zadana putanja je `dirname(__DIR__, 2) . '/bazna-license-private'` relativno od `licenca/api.php`. Možete je po potrebi zadati apsolutnim `storage_dir` unutar privatnog `config.php`.
4. Omogućite izlazni HTTPS/cURL s licencnog poslužitelja prema korisničkim WordPress domenama, radi **dokaza vlasništva nad domenom**. TLS mora potvrditi odgovarajući certifikat; servis ne prati preusmjeravanja i odbija privatne/rezerverirane DNS adrese.
5. Ako koristite Nginx, **ručno blokirajte pristup `/licenca/private/` i `/licenca/bin/`**, jer Nginx ne čita `.htaccess`. Na Apacheu su dodana lokalna `.htaccess` pravila. Ne postavljajte `activations.json` ni `rate.json` u javno dohvatljivu mapu.
6. Testirajte `GET https://licenca.brendigo.com/licenca/api.php` i očekujte JSON s nazivom servisa. Zatim instalirajte WordPress dodatak BAZNA te u **BAZNA → Licenca** obvezno upišite **domenu web-stranice** (npr. `primjer.hr`, bez `https://`) i **licencni ključ**. Domena mora odgovarati stvarnoj WordPress domeni. Nakon uspješne provjere svi administracijski moduli postaju dostupni. Android se povezuje samo unosom WordPress HTTPS adrese i čita već objavljene podatke; **Android ne dobiva licencni ključ**.

## Dizajn i ograničenja

- **Točno 10.000 licenci**: serijski brojevi 00001–10000, svaki s 128-bitnim autentikacijskim dijelom izvedenim iz master secreta preko HMAC-SHA256. Javna serija ne omogućuje izračun ključa bez tajne.
- **Jedna licenca = jedna trajno zaključana DNS domena**: `example.hr` i `www.example.hr` tretiraju se kao ista domena. Različite poddomene su odvojene domene. Podržane su WordPress instalacije u potdirektoriju preko `site_path`.
- **Domena se mora dokazati**: WordPress privremeno objavljuje nasumični izazov u `/wp-json/brendigo-sidrena/v1/license-proof`; servis ga dohvaća isključivo s potvrđene javne HTTPS domene. Za obične WordPress poveznice podržan je `?rest_route=...` fallback. Server pinna prethodno provjerenu javnu IPv4 adresu i odbija redirecte (zaštita od SSRF/DNS rebindinga).
- **Podaci o aktivacijama**: samo serijski broj, kanonska domena, status (aktivna/neaktivna) i datumi, zapisani kao JSON u privatnoj mapi, s atomskom zamjenom datoteka i `flock` zaključavanjem. Za serijski broj ne pohranjuje se sirovi ključ.
- **Ograničenje poziva**: do 60 zahtjeva/minuti po IP adresi s hashiranim IP identifikatorom. Za javnu uslugu dodatno se preporučuje rate limiting na web-poslužitelju/WAF-u, dostupni logovi i redovite privatne sigurnosne kopije.
- **WordPress**: ključ je šifriran lokalno AES-256-GCM uporabom postojećeg WP salta. Provjera najmanje dnevno; ograničen offline grace od 72 sata. Administracija se zaključava kada licencija više nije valjana, ali javno objavljeni cjenici/povijest ne brišu se zbog gubitka licence.
- **Nema SQL-a, udaljenih SDK-ova ni ovisnosti o trećim stranama**: međusobna HTTPS komunikacija s vlastitom poddomenom nužna je za provjeru jedinstvene domene.
- GPL kod može se tehnički modificirati. Server provodi jedinstvenost za službena izdanja, ali nije DRM koji se ne može zaobići.
- **Trajna veza s domenom:** nakon prve aktivacije, serijski broj zauvijek ostaje vezan uz kanonsku domenu upisanu na korisnikovoj WordPress stranici. Deaktivacija samo mijenja status u `inactive`, a ne briše poslužiteljski zapis niti dopušta prijenos na drugu domenu. Ključ s jedne domene **ne vrijedi** na drugoj ni nakon deaktivacije; iznimke nisu dostupne putem javnog API-ja.
- **Zahtjev za kompatibilnošću**: stari WP slug `brendigo-sidrena-cijena`, REST ruta `brendigo-sidrena/v1`, Android `applicationId` i Windows putanja SQLite baze ostaju nepromijenjeni dok se ne provede zasebna migracija.
- **Pravni okvir:** licenciranje ne dokazuje ispravnost bazne, aktualne, jedinične ili najniže 30-dnevne cijene. Nema automatskog certificiranja sukladnosti.

## Izdavanje i testovi

```bash
php licenca/tests/test.php
php -l licenca/lib.php
php -l licenca/api.php
php -l wordpress/brendigo-sidrena-cijena/includes/class-license.php
php tools/test_bazna_brand.php
```

Na privatnom poslužitelju ponovna izrada iste serije iz postojećeg secreta:

```bash
php licenca/bin/generate.php /private/path/BAZNA-10000.txt
```

**Nikada ne mijenjajte master secret nakon početka prodaje** bez migracijske strategije: svi dotadašnji ključevi prestali bi vrijediti. Kopirajte i čuvajte `config.php` i JSON aktivacije izvan javnog HTML korijena u redovitim, šifriranim sigurnosnim kopijama.

## Treba dodatno provjeriti prije produkcije

Nginx/Apache konfiguraciju, TLS reverse proxy, Cloudflare WAF, hostinge s isključenim outbound cURL-om, IPv6-only domene (trenutačno zahtijeva javni IPv4 A zapis), tipične multisite instalacije, performanse i sigurnost pod opterećenjem, te uvjete i privatnost obrade domenskih podataka. **Automatizirani test nije zamjena za penetracijski test.**
