# SIDRENA — vizualni QA Android + WordPress

## Što znači 1:1

Kompilacija, Lint, funkcionalni testovi i snimanje PNG-a **ne dokazuju**
vizualnu podudarnost. Za status 1:1 potreban je usporediv prikaz s izvornim
referentnim PNG-om, identičnih dimenzija, sistemskog UI-a, fonta i gustoće.
Zadani kriterij alata `tools/compare_ui_reference.py` je **MAE = 0** i
**0% promijenjenih piksela**. Svako odstupanje daje neuspjeh.

Izvorni dizajn korisnika nije zamijenjen generiranim slikama; stvarne slike
i fotografije aplikacije ne nastaju u chatu.

## Android — redoslijed

| Br. | Zaslon | Naziv snimke |
|---|---|---|
| 01 | Splash | `01-splash.png` |
| 02 | Povezivanje | `02-connect.png` |
| 03 | Početna | `03-home.png` |
| 04 | Proizvodi | `04-products.png` |
| 05 | Usluge | `05-services.png` |
| 06 | Detalj stavke | `06-detail.png` |
| 07 | Cjenici | `07-documents.png` |
| 08 | Arhiva | `08-archive.png` |
| 09 | Poslovnice | `09-outlets.png` |
| 10 | Pretraživanje | `10-search.png` |
| 11 | Postavke | `11-settings.png` |
| 12 | O aplikaciji | `12-about.png` |

`tools/run_android_responsive.sh` snima 12 ekrana na 9 stvarnih emulator
konfiguracija (108 PNG-a). **01 Splash** sada se snima bez `Scaffold`
okvira i njegovog `systemBarsPadding`; to je nužno da test ne mijenja
geometriju referentne full-bleed grafike.

Izvorni Splash PNG je prema referentnoj dokumentaciji 1080×1920. Za točnu
usporedbu na emulatoru moraju odgovarati *fizička* rezolucija, način prikaza
statusne/navigacijske trake te verzija fonta. Ako se referenca i test prikazuju
u različitim viewportima, alat namjerno prekida usporedbu.

## WordPress — redoslijed

`Pregled` → `Proizvodi i usluge` → `Poslovnice` →
`Javni cjenik` → `Uvoz/Izvoz` → `Postavke` → `Upute`.
Provjeravaju se i javna stranica cijena, mobilne kartice, CSV/XML poveznice
te povezanost s Android REST v1.

`tools/test_responsive_browser.mjs` već snima javni cjenik i administraciju
na 13 širina (240–1920 px), ukupno 26 stvarnih Chrome PNG-a. Test sada
posebno zahtijeva prelamanje cijena i URL-ova pri širini do 720 px. Lokalna
CSS korekcija utječe samo na mobilni prikaz, ne mijenja vrijednosti cijena.

## Usporedba piksela

Pillow je **isključivo QA alat**, nije ovisnost WordPress dodatka ni Android
aplikacije.

```bash
python3 -m pip install Pillow
python3 tools/compare_ui_reference.py \
  reference/01-splash.png \
  artifacts/android-responsive/reference-393/01-splash.png \
  --diff-dir artifacts/ui-diff
```

**Važno:** primjeri s različitim dimenzijama referentne i snimljene slike
prijavit će grešku. Koristiti PNG iz reference pripremljen za istu fizičku
rezoluciju. Za više zaslona postaviti direktorij referentnih slika i
direktorij stvarnih snimki s identičnim nazivima.

```bash
python3 tools/compare_ui_reference.py reference-360/ actual-360/ \
  --diff-dir artifacts/ui-diff
```

Izlaz: `PASS/FAIL`, prosječna RGB pogreška (`MAE`), postotak
promijenjenih piksela, granični pravokutnik razlike te lokalni `diff-*.png`.
Opcije `--ignore-top` / `--ignore-bottom` postoje samo za dokazivo
neusporedive Android sustavne trake; bez njih se uspoređuju svi pikseli.
Pozitivna tolerancija mora se posebno dokumentirati, ne smije se prikazati
kao egzaktna 1:1 identičnost.

## Trenutačni status

- Splash: izvorni SVG i referentne koordinate kodirani; popravljena stabilnost
  odbrojavanja i korektnost snimanja u instrumentacijskom testu.
- WordPress mobilni javni cjenik: popravljena desktop `nowrap` pravila koja
  su mogla probiti usku karticu.
- **Nema konačne 1:1 potvrde** dok se korisnikove originalne PNG reference
  ne usporede sa stvarno snimljenim PNG-ovima i dok svi odstupajući
  zasloni nisu ispravljeni.
- Testovi i izdanja trebaju se provjeriti kroz GitHub Actions nakon PR-a.
