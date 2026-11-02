# SIDRENA

![SIDRENA by Brendigo](desktop/branding/sidrena-banner.svg)

### Cijene pod kontrolom. Poslovanje s povjerenjem.

**SIDRENA** povezuje evidenciju proizvoda, usluga, sidrenih cijena, javnih cjenika i arhiviranih objava. Rješenja za **Android, Windows i WordPress** dizajnirana su oko preglednih podataka, tamnoplave palete i prepoznatljivih narančastih detalja.

[Preuzmi SIDRENA](https://github.com/bren-wp/SIDRENA/releases) · [Web-stranica](https://brendigo.com/sidrene-cijene/) · [Podrška](mailto:sidrena@brendigo.com)

## Tri platforme. Jedan SIDRENA identitet.

| Platforma | Namjena | Izdanje |
|---|---|---|
| **Android** | Pregled javnih proizvoda, usluga, sidrenih cijena, poslovnica i arhive iz povezanog WordPressa | **1.5.9** |
| **Windows** | Lokalno upravljanje katalozima, poviješću cijena, lokacijama, CSV/XML datotekama i sigurnosnim kopijama | **0.10.6** |
| **WordPress** | Službeni dodatak za javne cjenike, sidrene cijene i arhivu | **1.5.9**, nadopunjen pravni profil |

### SIDRENA Android

Povežite WordPress stranicu, pregledajte proizvode i usluge, filtrirajte sidrene cijene, odaberite poslovnicu i otvorite objavljene cjenike ili arhivirane CSV/XML dokumente. Izgled aplikacije usklađen je s dizajnom SIDRENA i referentnim Android ekranima.

![SIDRENA vizualni identitet](desktop/branding/sidrena-banner.svg)

### Android ↔ WordPress povezivanje (SIDRENA 1.5.9)

Instalirajte službeni WordPress dodatak na javnu **HTTPS** domenu te u Android aplikaciju upišite adresu WordPress instalacije (npr. `https://primjer.hr/` ili `https://primjer.hr/trgovina/`). Za prikaz javnih cjenika nije potrebna WordPress prijava. Android podržava WordPress REST i u načinu lijepih poveznica (`/wp-json/`) i u načinu običnih poveznica (`?rest_route=`); prihvaća samo javne HTTPS adrese iste domene.

**Napomena:** podaci se prikazuju tek nakon što WordPress objavi javni CSV; prva aktivacija može zahtijevati dovršetak WP-Cron objave. Provjerite SIDRENA administracijski status objave i posjetite javni REST endpoint bez prijave. Podaci o sidrenoj cijeni nisu automatski procijenjeni i ovo nije potvrda zakonske sukladnosti cijena.

[Upute za instalaciju, povezivanje, sigurnosne provjere i pravni audit](docs/ANDROID-WORDPRESS-INTEGRATION-AUDIT-159.md)

### Android i WordPress — zajednički javni katalog

Android 1.5.9 i WordPress 1.5.9 uključuju korekcije mobilnog sučelja i pripremu novog izdanja. U WordPressu otvorite **SIDRENA → Javni cjenik** i generirajte javne CSV cjenike prije povezivanja. Panel **SIDRENA Android · Povezivanje** prikazuje jesu li CSV proizvodi i usluge objavljeni te omogućuje kopiranje HTTPS adrese stranice.

Android ostaje samo za čitanje: prihvaća REST v1, učitava stvarne proizvode/usluge i poslovnice, poštuje status objave, te prikazuje stvarne WooCommerce slike samo s iste javne HTTPS domene. Bez objavljenih CSV cjenika prikazuje jasnu obavijest i omogućuje naknadno osvježavanje; stari dodaci bez statusnih polja i dalje su kompatibilni. Niti jedna promjena nije objavljena u produkciji prije spajanja i izdavanja.

### SIDRENA Desktop za Windows

Nativno Windows sučelje za lokalno vođenje aktualnih i povijesnih cijena, kataloga, usluga, poslovnica i izvoza. Podržani su prijenosni **Portable.exe** i standardni **Setup.exe** instalacijski paket. Aplikacija ima provjere cjelovitosti pri radu sa sigurnosnim kopijama i evidenciju promjena cijena.

### SIDRENA za WordPress

Službeni dodatak **Brendigo Sidrena Cijena 1.5.9 (kandidat za izdanje 1.5.9)** osigurava javne cjenike i arhiviranje. Promjene dodatka izdaju se zasebno od Android i Windows aplikacije.

## Preuzimanja

Sve dostupne datoteke, uključujući Android APK/AAB, Windows Portable/Setup i službeni WordPress ZIP, nalaze se na [GitHub izdanjima](https://github.com/bren-wp/SIDRENA/releases).

Android APK namijenjen je instalaciji i provjeri. Android AAB treba odgovarajući produkcijski potpis prije objave u Google Playu. Javni Android pregled koristi već objavljene podatke te ne obavlja administratorsko uređivanje cijena.

## Dokumentacija

[Android](android/README.md) · [Windows](desktop/README.md) · [Vizualni standard Androida](docs/ANDROID-UI-REFERENCE.md) · [Izdanja i verzije](android/RELEASE.md) · [Audit izvornog koda](docs/CODE-AUDIT-2026-10-21.md)

**SIDRENA · by Brendigo**

[brendigo.com](https://brendigo.com/) · [sidrena@brendigo.com](mailto:sidrena@brendigo.com)

## Pravna revizija — listopad 2026.

WordPress **1.5.9**, Android **1.5.9** i Windows **0.10.6** koriste zajednički javni profil propisa NN 101/2026 i izmjena NN 110/2026. Datum proširene primjene je **17. studenoga 2026.** Nacrt novih pravilnika još nije konačni propis. Povijesne sidrene cijene ne izračunavaju se automatski iz sadašnje cijene.

[Pravna revizija 29. 10. 2026.](docs/LEGAL-REVIEW-2026-10-29.md) · [Pravni okvir](docs/LEGAL-2026-INTEROPERABILITY.md) · [Sigurnosni audit](docs/SECURITY-DEADCODE-AUDIT-2026-10-24.md)
