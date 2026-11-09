# BAZNA — priprema propisa, Android/WordPress ugovor i distribucija dodatka

Provjera: 9. studenoga 2026. — razvojna grana `fix/bazna-legal-wordpress-review-brand-parity-20261109`.

## 1. Objavljeno pravo i prijelazni režim

Službene Odluke NN 101/2026 (o dodatnoj cijeni i o objavi cjenika),
izmijenjene NN 110/2026, imaju određen početak primjene **17. 11. 2026.**
Izmjene NN 110/2026 stupile su na snagu 30. 9. 2026. i pomaknule
datum primjene obveza. Do početka novih obveza treba uzeti u obzir
postojeći režim NN 75/2025 za kategorije koje on već obuhvaća.

- Referentna cijena **2. 5. 2025.**: ranije obuhvaćene vrste robe
  (hrana, piće, kozmetika, čišćenje, toaletne i kućanske potrepštine).
- Referentna cijena **10. 9. 2026.**: ostala roba i usluge na koje se
  odnose objavljene odluke.
- Uvijek je potreban **stvaran dokaz** o cijeni na dan koji odgovara
  pravilnoj kategoriji. Današnja redovna cijena nije vjerodostojna
  zamjena za povijesnu.
- **Najniža cijena u 30 dana prije sniženja** ostaje zasebna od bazne
  cijene i pripadajućeg datuma. Ni WP ni Android ih ne smiju automatski
  poistovjetiti.
- Za objavu CSV/XML, naziv datoteke s poslovnicom, vremenom i brojem
  pohrane, automatizirano prikupljanje javno dostupnih cijena te arhivu
  treba provjeriti svaku odgovarajuću obvezu prema vrsti prodaje.
- Posebna pravila za proizvode i usluge iz *prijedloga pravilnika*
  (savjetovanje 14. 10. – 13. 11. 2026.) **nisu u ovom izdanju
  proglašena konačno objavljenim propisom**.

Ovim razdvajanjem plugin može pripremiti podatke **prije** 17. 11.,
te od tog datuma jasno označava da je početak primjene objavljenih
odluka nastupio. To ne pretpostavlja da u međuvremenu nije objavljen
zamjenski pravilnik: potreban je zaseban pregled Narodnih novina.

Izvori (primarni):
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html
- https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html
- https://www.vlada.gov.hr/ministar-susnjar-kroz-dijalog-s-gospodarstvenicima-do-provedivih-pravila-i-vece-zastite-potrosaca/47439

## 2. Implementacija i kompatibilnost

WordPress `Brendigo_Sidrena_Cijena_Legal` izlaže javni REST profil
(`schema=1`) s datumom 17. 11., klasifikacijom i pravilima iz
službenih Odluka, a novi opcionalni objekt `implementation` deklarira
da se povijesne cijene unose uz dokaz i da nacrti nisu automatski
aktivirani.

Android `LegalContract` zasebno provjerava datum početka primjene
i da bazna cijena nije automatski izračunata iz trenutne ili najniže
cijene u 30 dana. Stariji REST odgovor bez novog objekta i dalje radi.

WordPress prikazuje razdoblje pripreme ili nastup datuma primjene
prema lokalnom WP datumu; Android rabi `Europe/Zagreb`.
Povezivanje proizvoda, usluga, poslovnica i javnih CSV-ova zadržava
postojeće kompatibilne ključeve i rute — **bez gubitka podataka**.

## 3. Dva različita distribucijska kanala WordPress dodatka

WordPress.org smjernice zabranjuju zaključane lokalne značajke
licencnim ključem i servis koji samo provjerava licence
(Guidelines 5 i 6). Zato postoje dva odvojena načina pakiranja:

1. **Izvorni privatni paket:** ostaje postojeća funkcionalnost
   aktivacije; nije prikladno nazivati ga WordPress.org-odobrenim.
2. **WordPress.org kandidat:** `tools/package_wporg.py` gradi
   zaseban `bazna-cjenici` GPL ZIP: uklanja provjeru licencnog
   servera, administratorski ekran licence i zaključavanje lokalnih
   funkcija. Plugin radi samostalno s WooCommerceom ili bez njega,
   a javni REST API ostaje čitljiv Androidu.

Skripta ne mijenja izvorne datoteke niti postojeće pohranjene
vrijednosti. **Pakete ne treba instalirati istodobno** jer dijele
interni kompatibilni kod i podatke.

Službene WordPress smjernice:
- https://developer.wordpress.org/plugins/wordpress-org/detailed-plugin-guidelines/
- https://developer.wordpress.org/plugins/internationalization/how-to-internationalize-your-plugin/
- https://wordpress.org/plugins/plugin-check/

CI provjerava reproducibilan ZIP i SHA-256, PHP sintaksu, uparivanje
naziva i Text Domaina, odsutnost poziva licencnom servisu u kandidatu,
WordPress i Android testove. To **nije** formalni Plugin Check na
instaliranom WordPressu, sigurnosni pentest ili odobrenje WordPress.org.

## 4. Vizualna usklađenost

Svi Android korisnički zasloni sada prikazuju naziv BAZNA/Bazna cijena
umjesto starog vidljivog naziva, uključujući detalje, filtar,
prikaz pravnog profila, postavke i ekrane trgovine.

WordPress pregled, upute, uređivač proizvoda i WooCommerce varijacije
usuglašene su s terminima aplikacije. Povijesni interni identifikatori,
meta-ključevi, postojeći URL-ovi i javni CSV stupci i dalje su prisutni
radi kompatibilnosti i izbjegavanja migracijskog gubitka.

Za **0 % vizualnog odstupanja** nije dovoljan prolazak CI-ja: još je
potrebno spojiti svaki izvorni referentni kadar i odgovarajuću snimku
na istim dimenzijama i usporediti piksele; WordPress administraciju
treba zasebno snimiti u pravom WordPress/WooCommerce okruženju za
svaku od osam kartica.

Ne postoji programatska ni pravna potvrda koja zamjenjuje provjeru
stvarnih iznosa od trgovca i pregled novog propisa kada bude objavljen.
