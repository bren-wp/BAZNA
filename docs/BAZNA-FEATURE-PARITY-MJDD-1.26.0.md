# BAZNA — funkcionalni pregled prema referentnom dodatku 1.26.0

**Izvor pregleda:** korisnikova ZIP datoteka
`mjdd-sidrena-cijena.1.26.0.zip` — *MJDD Sidrena i bazna cijena
v1.26.0*, autor Jure Zadravec / MJDD. Licenca reference:
**GPL-2.0-or-later**. Logika je implementirana u postojećim
BAZNA komponentama; originalni MJDD vizualni identitet nije prenesen.

## Matrica funkcionalnosti

| Funkcionalnost iz reference | BAZNA WordPress 1.5.20 | Web PHP / Joomla 1.1.4 | Shopify 1.1.4 | Android 1.5.20 |
|---|---|---|---|---|
| WooCommerce proizvodi i varijacije | Da | Nije WooCommerce | Shopify adapter | Samo čitanje objavljenog CSV-a |
| Samostalni katalog proizvoda/usluga | Da | Da | Da, prema dostupnom backendu | Javni pregled |
| Bazna cijena + dokaziv referentni datum | Ručno potvrđeno | Uz provjeru povijesti | Zajednička jezgra | Samo prikaz iz CSV-a |
| SKU, marka, barkod, CJM, poseban oblik | Da | Polja standardne sheme | Ista javna shema | Prikazuje objavljena polja |
| Poslovnice i odvojeni cjenici | Da | Da | Povezani katalog | Odabir poslovnice |
| CSV/XML i HTML cjenici | Da | Da | Potpisani app-proxy | CSV pregled / HTTPS poveznica |
| Arhiva objava | Da | Da, snapshoti | Javni API po trgovini | Pregled javne arhive |
| **„Cijena na upit” uz kriterije** | **Objavljeni REST snapshot** | **Editor i HTML/REST** | **HTML/REST** | **Prikaz i filtriranje po poslovnici** |
| WooCommerce tax/HPOS, WC importer, WPML | WP-specifično | Nije primjenjivo | Nije primjenjivo | Ne uređuje WC |
| Admin unos i sinkronizacija | WordPress/WooCommerce | Lokalna autentificirana administracija | Zaseban backend | **Read-only** |

**Razlika platformi:** ujednačen je *javni ugovor podataka i značenje
prikaza*, ne automatsko dvosmjerno uređivanje. Android nema i ne smije
imati pristup WordPress administratorovoj lozinci. Shopifyov app-proxy
zahtijeva potpis; nije isto što i otvoreni public WordPress endpoint.

## Zajednički javni REST v1

Discovery dodaje opcionalnu HTTPS poveznicu `on_request` prema
`.../usluge-na-upit`. Odgovor:

```json
{"schema":1,"items":[
  {"id":"Q-1","name":"Izrada po mjeri",
   "criteria":"Dimenzije i materijal","outlet":"RI-01"}
]}
```

Nema brojčanog iznosa. Usluga se **ne** umeće u CSV/XML.
WordPress nakon uspješnog generiranja kataloga pohranjuje poseban
javno objavljeni snapshot; nikad ne čita uređivački nacrt na REST
zahtjev. PHP/Joomla/Shopify čitaju samo objavljeni katalog.
Android ograničava odgovor na 500 stavki i 2 MiB, provjerava tipove,
identifikatore, poslovnice i isti HTTPS izvor. Stari REST v1 ostaje
kompatibilan bez novog polja.

## Sigurnost, zakon i stvarna ograničenja

Reference MJDD 1.26.0 dokumentira i WP-CLI „fill” koji može kopirati
**trenutnu** redovnu cijenu u praznu povijesnu cijenu. BAZNA takvo
automatsko zaključivanje ne preuzima: današnji iznos nije dokaz cijene
na 2. 5. 2025. ili 10. 9. 2026. Najniža cijena u 30 dana prije
sniženja ostaje zaseban podatak.

Testovi pokrivaju odvajanje nacrta i objave, poslovnice, XSS
escaping u HTML-u, odsutnost izmišljenog iznosa u CSV/XML i
Android HTTPS/parser. **Nije potvrđena potpuna funkcionalna
istovjetnost svih WooCommerce i Shopify administracijskih operacija**,
stvarna usklađenost trgovca sa zakonom, Google Play Console odobrenje
niti pixel-diff 0 % na svim grafičkim referencama.

## Dodatna podudarnost u BAZNA 1.5.20

- Web PHP, Joomla i Shopify koriste zajedničku metodu
  `Core::publicCatalogHtml` za objavljene proizvode, usluge i kriterije
  cijene na upit. Sve korisničke vrijednosti HTML-escapeaju se.
  Na Webu je dostupna i pretraga te odabir poslovnice; na Shopifyju
  zahtjevi i dalje trebaju valjani app-proxy potpis.
- WordPress i svi serverski adapteri imaju aditivne, opcionalne REST
  parametre `outlet` i `q` na ruti za usluge s cijenom na upit.
  Stari klijenti bez tih parametara i dalje dobivaju dosadašnji odgovor.
- Androidova opća pretraga uključuje objavljene usluge na upit,
  filtrirane po poslovnici i kategoriji. Ne otvara lažni detalj cijene.
- PHP i Android imaju regresijske provjere objavljivanja, XSS escaping,
  pretrage, sprječavanja miješanja poslovnica i skrivenosti nacrta.
- **Granice ostaju:** nijedan automatski uvoz WooCommerce priključaka
  u Joomlu ili Shopify nije tvrdnja funkcionalne jednakosti;
  za Shopify Android pristup potreban je podržan način autentikacije
  app-proxyja. Testovi ne potvrđuju Google Play odobrenje niti
  potpuni pravni ili 0% pixel-diff certifikat.

## Provjera ponovno priloženog ZIP-a (14. 11. 2026.)

Arhiva `mjdd-sidrena-cijena.1.26.0(2).zip` sadrži **57 datoteka**,
izvornu GPL-2.0-or-later licencu i atribuciju **Jure Zadravec / MJDD**.
Referenca sadrži WooCommerce jednostavne proizvode i varijacije,
samostalni katalog, porezni prikaz, CJM, marku, barkod, dostupnost,
poslovnice, XML/CSV, shortcodeove `[sidrena-cijena]`,
`[sidrena-cjenik]`, `[sidrena-arhiva]`, dnevne objave, arhivu i HPOS.

BAZNA zadržava kompatibilne cijene, uvoz/izvoz, shortcodeove i javne
podatkovne rute za Android, uz zasebnu REST v1 integraciju. Izostaje
opasno automatsko prepisivanje trenutne u navodno povijesnu cijenu,
jer povijesni iznos mora biti dokumentiran. Službeni WordPress ZIP i
WordPress.org kandidat sada dijele potpuno otključanu GPL jezgru;
izvorni autor i uvjeti redistribucije navedeni su u `ORIGINAL-CREDITS.txt`.

**Nije potvrđena 1:1 jednakost svih zaslona, svih builder integracija ili
svih WooCommerce operacija na stvarnoj instalaciji WordPressa 7.1.4 i
WooCommercea 11.3.0.** Verzija `WC tested up to` ostaje na izdanju
koje projekt stvarno navodi kao testirano. Ovaj izvorni i CI pregled
nije WordPress.org odobrenje; službeni Plugin Check i ručni pregled
potrebni su za objavu u službenom direktoriju.
