# BAZNA — postavljanje javnog licencnog API-ja

Adresa: **https://licenca.brendigo.com/licenca/api.php**

1. Postavite javne datoteke mape `licenca` na web-poslužitelj s HTTPS-om i PHP-om 8.1+ (preporuka 8.3+), cURL i JSON podrškom.
2. **Privatno** dodajte `licenca/private/config.php` iz zasebnog privatnog paketa. Mora sadržavati **isti** `master_secret_hex` s kojim je generirano postojećih 10.000 ključeva; promjena master ključa poništava ih. Datoteka se ne smije spremati u javni GitHub.
3. HTTP mora blokirati `/licenca/private/`, `/licenca/bin/`, `/licenca/tests/` i sve osim `api.php`. Apache upotrebljava .htaccess; za Nginx koristite `nginx.conf.example` i prilagodite PHP-FPM socket. **Nemojte pretpostaviti da Nginx čita .htaccess.**
4. JSON pohrana mora biti **izvan DOCUMENT_ROOT**, a PHP korisnik mora imati pravo pisanja u nju. Preporučena zasebna mapa: `/home/hosting-user/bazna-license-private` s dozvolama 0700. Dodajte apsolutni `storage_dir` u privatni `config.php` ako zadana putanja ne odgovara.
5. Ako imate prethodno korišten `activations.json`, sačuvajte ga prije ažuriranja; **nikad ne stvarajte praznu evidenciju preko postojeće**. Sigurnosne kopije JSON datoteka čuvajte izvan web korijena.
6. `GET /licenca/api.php` sada vraća `{"ok":true,"status":"ready",...}` **samo ako je servis stvarno konfiguriran**, i HTTP 503 s kodom pogreške ako nije. Taj zahtjev ne otkriva ključeve, domene ili datume.
7. Pokrenite `php licenca/bin/preflight.php /apsolutna/putanja/do/document-roota` za provjeru konfiguracije, PHP ekstenzija i JSON pohrane **bez otkrivanja master ključa**. Zatim pokrenite `php licenca/tests/test.php`, `php licenca/bin/generate.php /private/export/BAZNA-10000.txt` (izdavanje istih postojećih 10.000 ključeva, **samo privatno**, nikada u web root), i provjerite TLS certifikat na servisu.
8. U WordPress dodatku **BAZNA → Licenca** unesite samo domenu i ključ, pa testirajte aktivaciju. Servis provjerava javni HTTPS izazov na toj domeni i trajno sprema `serijski-broj → domena` u JSON. Isti ključ ne može se koristiti na drugoj domeni, ni nakon deaktivacije.
9. Androidu se unosi samo WordPress HTTPS adresa; ne prima licencni ključ. Javni cjenici moraju ostati vidljivi kupcima i automatiziranim programima čak i kada licenca administratora istekne.

**Granice:** Ne postoji siguran javni upload koji u GitHubu sadrži privatni master secret. Za stvarno aktiviranje svih 10.000 ranije izdanih ključeva potrebno je **jednokratno dodati privatni config.php** ili koristiti odvojeno pripremljen privatni ZIP. Nema SQL-a, vanjskih SDK-ova ni hardkodiranih licencnih ključeva u dodatku.
