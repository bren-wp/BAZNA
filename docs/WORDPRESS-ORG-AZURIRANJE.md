# BAZNA: ažuriranja postojećeg dodatka na WordPress.org

Službeni direktorij: https://wordpress.org/plugins/brendigo-sidrena-cijena/  
Službeni SVN: https://plugins.svn.wordpress.org/brendigo-sidrena-cijena/  
Izvorni kod: `wordpress/brendigo-sidrena-cijena/`  
Glavna PHP datoteka: `brendigo-sidrena-cijena.php`  
Text domain: `brendigo-sidrena-cijena`

## Cilj

WordPress.org je službeni kanal automatskih ažuriranja. **GitHub Releases nije kanal WordPress.org automatskih ažuriranja.** GitHub je izvor razvoja i testiranja; SVN `trunk/` i `tags/<verzija>/` služe za izdavanje.

Dodatak ne smije biti instaliran pod drugim imenom direktorija: time bi WordPress mogao smatrati novu instalaciju drugim dodatkom. Ne mijenjati postojeća imena tablica, opcija, metapodataka, shortcodeove, REST putanje ili uninstall ponašanje samo radi promjene brenda.

> **Važno:** `tools/package_wporg.py` trenutačno stvara zaseban kandidat `bazna-cjenici/` (i `bazna-cjenici.php`). To **nije** distribucija za ažuriranje već objavljenog dodatka `brendigo-sidrena-cijena`. Za postojeći WordPress.org dodatak uvijek koristiti `tools/package_plugin.py` i workflow `deploy-wordpress-org.yml`.

## Jednokratna konfiguracija

Osoba koja ima pravo objave na već registrirani WordPress.org dodatak upisuje sljedeće GitHub **Actions repository secrets** u `bren-wp/BAZNA` kroz **Settings → Secrets and variables → Actions → New repository secret**:

- `SVN_USERNAME`: WordPress.org korisničko ime s pravom objave na slug `brendigo-sidrena-cijena`.
- `SVN_PASSWORD`: posebna WordPress.org **SVN lozinka** (postavlja se u WordPress.org profilnim postavkama za SVN; nije GitHub token).

Nikada ne spremati SVN lozinku u repozitorij, workflow YAML, arhivu, komentare ili release notes. Sâm GitHub connector ne postavlja te tajne; to radi ovlašteni administrator.

## Objavljivanje nove verzije

1. U jedinstvenom izdanju BAZNA povećati verziju dodatka. PHP `Version`, konstanta `BRENDIGO_SIDRENA_CIJENA_VERSION`, `readme.txt` `Stable tag` i metapodaci ostalih platformi moraju biti usklađeni prema postojećem release gateu.
2. Pokrenuti provjere i uspješno završiti objedinjeno GitHub izdanje iste verzije. **Ne mijenjati datoteke dodatka nakon izrade oznake izdanja** bez nove verzije.
3. Nakon uspješnog GitHub release workflowa `Deploy BAZNA to existing WordPress.org plugin` pokušava objaviti istu provjerenu verziju u WordPress.org SVN, ako su SVN tajne ispravno postavljene.
4. Za ručno ponovno pokretanje otvoriti **GitHub → Actions → Deploy BAZNA to existing WordPress.org plugin → Run workflow** na grani `main`. Neobavezno upisati inačicu kao zaštitnu provjeru. Proces odbija objavljivanje neprovjerenog koda, nepodudarnog sluga i verzije bez odgovarajućeg GitHub izdanja.
5. Provjeriti SVN `trunk` i `tags/<verzija>`, WordPress.org stranicu dodatka i aktualnu verziju unutar instaliranog WordPressa. WordPress.org ažuriranja mogu kasniti zbog osvježavanja podataka.

**Prije bilo kakvog ažuriranja produkcijske trgovine napraviti sigurnosnu kopiju datoteka i baze podataka.** Objavljivanje ne smije automatski obrisati katalog, cjenike, arhivu ili postavke.

## Provjera iz WordPress administracije

Otvoriti **Dodaci → Instalirani dodaci**, pronaći **BAZNA / SIDRENA**, zatim **Nadzorna ploča → Ažuriranja → Ponovno provjeri**. Kad WordPress.org distribuira veću verziju, odabrati **Ažuriraj odmah** ili uključiti uobičajena WordPress automatska ažuriranja.

Za WP-CLI, kada je dostupan:

```bash
wp plugin list --fields=name,version,update,update_version
wp plugin update brendigo-sidrena-cijena --dry-run
wp plugin update brendigo-sidrena-cijena
```

Zadržati jedinstvenu mapu `wp-content/plugins/brendigo-sidrena-cijena/`. ZIP s mapom `bazna-cjenici` nije zamjenska nadogradnja.

## Sigurnosne provjere

`python3 tools/test_wporg_update_contract.py` potvrđuje da se službeni plugin ne preimenuje, da verzije odgovaraju, da je ZIP determinističan i da svi sadržaji odgovaraju izvornim datotekama. `official-plugin-ci.yml` izvršava taj test na PR-u. Deploy dodatno provjerava GitHub release, uspješne WordPress i audit provjere **istog označenog commita**, te uspoređuje izvor dodatka s njegovom objavljenom Git oznakom.

Objava na WordPress.org moguća je samo uz ispravne SVN ovlasti i uspješno izvršavanje deploy workflowa. Ovaj repozitorijski postupak nije sam po sebi dokaz da je nova verzija već javno objavljena.
