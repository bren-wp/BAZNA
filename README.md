# BAZNA

![BAZNA by Brendigo](desktop/branding/sidrena-banner.svg)

### Cijene pod kontrolom. Poslovanje s povjerenjem.

**BAZNA** povezuje evidenciju proizvoda, usluga, baznih cijena, javnih cjenika i arhiviranih objava. Postojeće platforme **Android, Windows i WordPress** nadopunjuju razvojne integracije za **samostalne PHP stranice, Joomlu i Shopify**, uz zajednički javni API v1 i domensko licenciranje.

[Preuzmi BAZNA](https://github.com/bren-wp/BAZNA/releases) · [Web-stranica](https://brendigo.com/) · [Podrška](https://brendigo.com/)

## Više platformi. Jedan BAZNA identitet.

| Platforma | Namjena | Izdanje |
|---|---|---|
| **Android** | Pregled javnih proizvoda, usluga, baznih cijena, poslovnica i arhive iz povezanog WordPressa | **1.5.14** |
| **Windows** | Lokalno upravljanje katalozima, poviješću cijena, lokacijama, CSV/XML datotekama i sigurnosnim kopijama | **0.10.6** |
| **WordPress** | Službeni dodatak za javne cjenike, bazne cijene i arhivu | **1.5.14**, nadopunjen pravni profil |

### Novi BAZNA adapteri — PHP, Joomla, Shopify

Ove integracije trenutačno su **razvojni kandidati 1.1.0**, ne odobreni produkcijski dodaci za svaku platformu. Kod i instalacijske upute nalaze se u `/web`, `/joomla` i `/shopify`.

| Integracija | Osnovne funkcije | Produkcijski status |
|---|---|---|
| **Web PHP** | Devet kartica, grafički editor, lokalni JSON, domenska licenca, CSV/XML, arhiva i javni API v1 | 182-snimka Chrome QA; potreban hosting E2E |
| **Joomla** | System plugin, Joomla ACL/CSRF, dijeljeni grafički editor i javni API v1 | Kandidat ZIP-a; potreban test na stvarnoj Joomla instalaciji |
| **Shopify** | Potpisani app-proxy, izolirane trgovine, uvoz EUR varijanti, privacy webhooks i theme blok | Custom-store adapter; OAuth i App Store objava nisu dovršeni |

Android automatski prepoznaje javne BAZNA putanje bez CMS lozinki; pritom zadržava izvorne WordPress REST putanje i sigurnosno ograničenje na istu HTTPS domenu. Pravilo povijesne bazne cijene ostaje ručna potvrda uz dokaz. Shopify `compareAtPrice` ne koristi se za izračun bazne cijene.

[Arhitektura svih platformi](docs/BAZNA-MULTIPLATFORM-ARCHITECTURE.md) · [PHP upute](web/README.md) · [Joomla upute](joomla/README.md) · [Shopify upute](shopify/README.md)

### BAZNA Android

Povežite BAZNA stranicu na podržanoj platformi, pregledajte proizvode i usluge, filtrirajte bazne cijene, odaberite poslovnicu i otvorite objavljene cjenike ili arhivirane CSV/XML dokumente. Izgled aplikacije razvija se prema referentnim prikazima BAZNA. Za potvrdu 1:1 potrebno je usporediti stvarne snimke zaslona s izvornim referencama na jednakim dimenzijama.

![BAZNA vizualni identitet](desktop/branding/sidrena-banner.svg)

### Android ↔ WordPress povezivanje (BAZNA 1.5.14)

Instalirajte službeni WordPress dodatak na javnu **HTTPS** domenu te u Android aplikaciju upišite adresu WordPress instalacije (npr. `https://primjer.hr/` ili `https://primjer.hr/trgovina/`). Za prikaz javnih cjenika nije potrebna WordPress prijava. Android podržava WordPress REST i u načinu lijepih poveznica (`/wp-json/`) i u načinu običnih poveznica (`?rest_route=`); prihvaća samo javne HTTPS adrese iste domene.

**Napomena:** podaci se prikazuju tek nakon što WordPress objavi javni CSV; prva aktivacija može zahtijevati dovršetak WP-Cron objave. Provjerite BAZNA administracijski status objave i posjetite javni REST endpoint bez prijave. Podaci o baznoj cijeni nisu automatski procijenjeni i ovo nije potvrda zakonske sukladnosti cijena.

[Upute za instalaciju, povezivanje, sigurnosne provjere i pravni audit](docs/ANDROID-WORDPRESS-INTEGRATION-AUDIT-159.md)

### Android i WordPress — zajednički javni katalog

Android 1.5.14 i WordPress 1.5.14 donose objavljene korekcije mobilnog sučelja i javnog kataloga. U WordPressu otvorite **BAZNA → Javni cjenik** i generirajte javne CSV cjenike prije povezivanja. Panel **BAZNA Android · Povezivanje** prikazuje jesu li CSV proizvodi i usluge objavljeni te omogućuje kopiranje HTTPS adrese stranice.

Android ostaje samo za čitanje: prihvaća REST v1, učitava stvarne proizvode/usluge i poslovnice, poštuje status objave, te prikazuje stvarne WooCommerce slike samo s iste javne HTTPS domene. Bez objavljenih CSV cjenika prikazuje jasnu obavijest i omogućuje naknadno osvježavanje; stari dodaci bez statusnih polja i dalje su kompatibilni. Produkcijska objava provodi se zasebno nakon spajanja i uspješnih provjera izdanja.

### BAZNA Desktop za Windows

Nativno Windows sučelje za lokalno vođenje aktualnih i povijesnih cijena, kataloga, usluga, poslovnica i izvoza. Podržani su prijenosni **Portable.exe** i standardni **Setup.exe** instalacijski paket. Aplikacija ima provjere cjelovitosti pri radu sa sigurnosnim kopijama i evidenciju promjena cijena.

### BAZNA za WordPress

Službeni dodatak **BAZNA – Bazne cijene 1.5.14** osigurava javne cjenike i arhiviranje. Promjene dodatka izdaju se zasebno od Android i Windows aplikacije.

## Preuzimanja

Sve dostupne datoteke, uključujući Android APK/AAB, Windows Portable/Setup i službeni WordPress ZIP, nalaze se na [GitHub izdanjima](https://github.com/bren-wp/BAZNA/releases).

Android APK namijenjen je instalaciji i provjeri. Android AAB treba odgovarajući produkcijski potpis prije objave u Google Playu. Javni Android pregled koristi već objavljene podatke te ne obavlja administratorsko uređivanje cijena.

## Dokumentacija

[Android](android/README.md) · [Windows](desktop/README.md) · [Vizualni standard Androida](docs/ANDROID-UI-REFERENCE.md) · [Izdanja i verzije](android/RELEASE.md) · [Audit izvornog koda](docs/CODE-AUDIT-2026-10-21.md)

**BAZNA · by Brendigo**

[brendigo.com](https://brendigo.com/) · [sidrena@brendigo.com](mailto:sidrena@brendigo.com)

## Pravna revizija — listopad 2026.

WordPress **1.5.14**, Android **1.5.14** i Windows **0.10.6** koriste zajednički javni profil propisa NN 101/2026 i izmjena NN 110/2026. Datum proširene primjene je **17. studenoga 2026.** Nacrt novih pravilnika još nije konačni propis. Povijesne sidrene cijene ne izračunavaju se automatski iz sadašnje cijene.

[Pravna revizija 29. 10. 2026.](docs/LEGAL-REVIEW-2026-10-29.md) · [Pravni okvir](docs/LEGAL-2026-INTEROPERABILITY.md) · [Sigurnosni audit](docs/SECURITY-DEADCODE-AUDIT-2026-10-24.md)

## Provjera kvalitete i ograničenja

CI provjerava kompilaciju, jedinične testove i dio sigurnosnih pravila. Zeleni build **nije** potvrda odsutnosti ranjivosti ni vizualne podudarnosti 1:1. Za usporedbu izgleda potrebni su izvorni PNG-ovi, snimke istih dimenzija i izvještaj `tools/compare_ui_reference.py`. Nazivi nekih postojećih programskih paketa, shortcodeova i izgradnih artefakata još nose povijesni naziv `sidrena` radi kompatibilnosti; javni naziv proizvoda je **BAZNA**.
