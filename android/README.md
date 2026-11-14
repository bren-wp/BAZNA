# BAZNA — Bazne cijene za Android

**BAZNA by Brendigo** · **1.5.21**

WordPress.org nadogradnje istog dodatka koristi postojeći slug `brendigo-sidrena-cijena` i službeni SVN, ne odvojenu instalaciju `bazna-cjenici`. Upute su u [WordPress.org vodiču](../docs/WORDPRESS-ORG-AZURIRANJE.md).

![BAZNA](../branding/bazna-banner.png)

Pregledajte objavljene cjenike, proizvode, usluge i poslovnice sa svoje WordPress stranice, uz prepoznatljiv tamnoplavi izgled i narančaste kontrole.

## Zasloni i mogućnosti

| Zaslon | Mogućnosti |
|---|---|
| Povezivanje | HTTPS adresa WordPress stranice |
| Početna | Povezana stranica, pregled podataka, zadnja objava i brzi pristup |
| Proizvodi | Popis, pretraga, filtri sidrenih cijena i dostupnost |
| Usluge | Objavljene usluge i pripadajuće cijene |
| Detalj stavke | SKU, trenutačna i bazna cijena, datum i jedinica mjere |
| Arhiva | CSV/XML zapisi, filtriranje i otvaranje datoteka |
| Poslovnice | Pregled i odabir poslovnice ili webshopa |
| Pretraživanje | Pretraga po nazivima, šiframa i ID-ovima |
| Postavke | Povezivanje, osvježavanje, odspajanje i informacije o aplikaciji |
| O aplikaciji | Verzija, kratke upute i kontakt podrške |

Izgled je usklađen s [Android dizajnerskim standardom](../docs/ANDROID-UI-REFERENCE.md).

## Povezivanje s dodatkom BAZNA

Aplikacija koristi javno dostupne rute postojećeg WordPress dodatka:

- `GET /wp-json/brendigo-bazna/v1/cjenici`
- `GET /wp-json/brendigo-bazna/v1/cjenici/proizvodi/csv`
- `GET /wp-json/brendigo-bazna/v1/cjenici/usluge/csv`
- `GET /wp-json/brendigo-bazna/v1/arhiva`

Pojedinačni CSV može se preusmjeriti na objavljenu datoteku na istoj HTTPS domeni. WordPress 1.5.16 dodaje opcionalni status `feeds.proizvodi.available_csv` i `feeds.usluge.available_csv` za webshop i svaku poslovnicu: `false` znači da datoteka još nije objavljena. Stari dodaci bez statusnih polja ostaju podržani. Proizvodi mogu neobavezno sadržavati stvarni `slika_url` s iste HTTPS domene, uz lokalnu ikonu kada ga nema. Prije povezivanja na WordPressu generirajte javni CSV cjenik, zatim u aplikaciji po potrebi odaberite Osvježi. Sadržaj poslovnica i arhive ovisi o stvarno objavljenim podacima na stranici. Aplikacija ne prikazuje izmišljene podatke kada javno sučelje ne nudi povijest promjena ili druge informacije.

**Uređivanje cijena ostaje u WordPressu ili Windows aplikaciji** dok ne postoji autorizirani REST API za mobilno uređivanje.

## Google Play — novi identifikator

Android koristi `com.brendigo.bazna` od verzije 1.5.17; verzija 1.5.21 nastavlja taj isti identifikator bez ponovne migracije. Ako je prethodna aplikacija s identifikatorom `com.brendigo.sidrena` bila objavljena u Google Playu, ova verzija zahtijeva **novu Play aplikaciju**. Android je neće instalirati kao nadogradnju stare aplikacije niti automatski prenijeti lokalnu adresu povezane stranice. Korisnik može ponovno unijeti istu HTTPS adresu i poslovnicu; javni cjenici ostaju na izvornom poslužitelju. Nepotpisani AAB potpišite ključem za novu Play aplikaciju.

## Instalacija

[Otvorite GitHub izdanja](https://github.com/bren-wp/BAZNA/releases) i odaberite Android APK. Za izdavanje u Google Playu potreban je odgovarajući potpisani AAB.

## Tehnička dokumentacija

Izvorni kod koristi Kotlin, Jetpack Compose i Material 3. Za izgradnju otvorite mapu `android/` u Android Studiju (JDK 17, Android SDK 37.0, Gradle 9.6). Provjere kompilacije, lint i JUnit nalaze se u GitHub Actions postupku.

[Google Play kontrolni popis](../docs/ANDROID-GOOGLE-PLAY-CHECKLIST.md) · [Izdanja](RELEASE.md) · [Audit](../docs/CODE-AUDIT-2026-10-21.md) · [Podrška](mailto:info@brendigo.com)

## Pravni profil (NN 101/2026 i NN 110/2026)

Aplikacija čita isti javni pravni profil kao Windows aplikacija iz WordPress dodatka 1.5.16. Prikazuje datum početka proširenih obveza **17. 11. 2026.**, oba referentna datuma **2. 5. 2025.** i **10. 9. 2026.**, minimalnu arhivu 30 dana te vrijeme dnevne objave do 8:00. Ne mijenja i ne izmišlja povijesne cijene.

[Pravni izvori i kontrolni popis](../docs/LEGAL-2026-INTEROPERABILITY.md) · [Sigurnosni audit](../docs/SECURITY-DEADCODE-AUDIT-2026-10-24.md)

Pravni profil dopunjen je informativnim identifikatorima NN 59/2026 i NN 105/2026; pravne obveze ne dokazuju se samim postojanjem profila.


### Pravna dopuna 29. 10. 2026.

Javni profil razlikuje baznu cijenu od najniže prethodne cijene za sniženja u pravilu 30 dana, bilježi pravila NN 105/2026 od 26. 9. 2026. i ostaje kompatibilan sa starijim WordPress profilima. Aplikacija je read-only i ne potvrđuje pravnu usklađenost trgovca.

## Usluge s cijenom na upit (1.5.21)

Na zaslonu **Usluge** prikazuju se i odvojeno objavljene usluge
bez unaprijed određene cijene. Svaka prikazuje naziv i kriterije,
uz poštovanje odabrane poslovnice. Te stavke se ne pretvaraju u
numerički CSV/XML i nisu automatski sniženja ili bazne cijene.
Ako poslužitelj ne podržava opcionalnu rutu, osnovni cjenici
i arhiva nastavljaju normalno raditi.

## Pretraživanje svih javno objavljenih usluga — 1.5.21

Zaslon **Pretraživanje** prikazuje proizvode, usluge s numeričkim
iznosom i javne usluge s cijenom na upit. Usluge na upit pokazuju
**naziv + kriterije**, ne izmišljeni iznos. Rezultati poštuju
odabranu poslovnicu i kategoriju pretrage. Opći katalog i
arhiva rade i sa starijim poslužiteljima bez opcionalnog
`on_request` REST proširenja.
