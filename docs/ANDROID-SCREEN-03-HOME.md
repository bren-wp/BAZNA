# SIDRENA Android — zaslon 03/12: Početna

## Referenca i cilj

Treći telefon korisnički potvrđenog 10-screen SIDRENA branding boarda. Usporedni cilj: tamnoplava podloga, horizontalni SIDRENA znak i tražilica, status **Povezano**, 2×2 kartice **Proizvodi / Usluge / Cjenici / Arhiva**, zasebne **Poslovnice**, tri redaka zadnjih javnih objava, te donja navigacija.

Dizajn je implementiran u nativnom Kotlin/Jetpack Composeu. Raspored je responzivan; doslovna usporedba 1:1 zahtijeva screenshot emulatora i overlay u odnosu na izvornu sliku.

## Izmjene

- `ui/HomeScreens.kt`: novi HomePage s vertikalnim pomicanjem na niskim zaslonima, stvarnim podacima i interaktivnim karticama.
- `ui/SidrenaApp.kt`: skriven prethodni generički zaglavni blok samo na početnoj stranici; ostali zasloni ostaju nepromijenjeni.
- `ui/Components.kt`: aktivna stavka navigacije bez dodatne Material pill pozadine i puneće ikone.
- `ui/HomeScreens.kt`: dva posebna UI statusa (konekcija, ponovna sinkronizacija); padajući izbornik za osvježavanje i promjenu WordPress stranice.
- `HomePresentationTest.kt`: datumi ISO i HR, sortiranje arhive, uklanjanje duplikata, naziv vrste cjenika.
- UI prolaz nakon 0.3.5: zeleno stanje povezivanja pojavljuje se samo kada nema neobjavljenih promjena; menu hit target 48 dp; interaktivne kartice i arhivski retci izričito su označeni kao tipke za TalkBack.

## Očekivana mjerenja u dp

| Dio | Dimenzija/paleta |
|---|---|
| Pozadina | #07182D |
| Kartice | #10263D |
| Obrub | #28455F / 1 dp |
| Narančasti naglasci | #FF852C |
| Status povezanosti | #52D9A3 |
| Zaglavlje | 62 dp |
| Kartice u mreži | 132 dp visine, 10 dp između stupaca |
| Poslovnice | 78 dp visine |
| Redovi nedavnih objava | 62 dp |
| Glavni card radius | 16 dp |
| Min. dodirna površina glavnih radnji | 48 × 48 dp |

## Podaci i istinitost

- Proizvodi, usluge, poslovnice i arhiva imaju stvarne brojčane vrijednosti iz `CatalogSnapshot`. U screenshotu su uzorci 1248, 320, 24 i 156 **isključivo referentni**, ne stavljaju se u produkcijski kod.
- Cjenici prikazuje **Javni** kada postoji stvarna javna stranica `index.publicPage`, inače **—**. API ne vraća pouzdan broj javnih cjenika pa se broj ne izmišlja.
- Nedavne objave preuzimaju stvarne CSV/XML datoteke iz arhive, sortiraju ih po datumu i čuvaju njihov isti-origin URL. Nema simuliranih obavijesti o promjenama cijena.
- Postojeći javni API i sigurnosne kontrole povezivanja nisu promijenjeni.
- Pravni profil ostaje dostupan putem proširivog odjeljka, ali aplikacija ne certificira usklađenost.

## Obvezna provjera prije mergea

1. GitHub Actions: Android compile, unit tests, lint, APK/AAB i sigurnosni audit.
2. Pixel-diff overlay na emulatoru 393×852 dp i 360×640 dp, 100% font scaling, zatim i 150% font scaling (moguće kontrolirano odstupanje zbog pristupačnosti).
3. Povezana WordPress domena bez arhive, sa 1 i 1000 arhivskih zapisa, bez datumskih polja, s dugačkom domenom i praznim outlet popisom.
4. Klik na pretraživanje, četiri kartice, poslovnice, zadnju arhivsku objavu, ponovno osvježavanje i promjenu stranice.
5. Provjera da kartica **Cjenici** otvara samo javni HTTPS URL prethodno potvrđen putem `SiteUrl.publicLink()`.

Nije prihvatljivo označiti ekran kao matematički 1:1 dok nije izrađena usporedna snimka stvarne aplikacije.
