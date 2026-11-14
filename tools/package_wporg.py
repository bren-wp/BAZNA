#!/usr/bin/env python3
"""Build a deterministic, GPL WordPress.org review candidate from the
SAME fully functional, license-free BAZNA source as the official WordPress ZIP.
Only the WordPress.org plugin slug, manifest text domain and readme differ.
Candidate is not an official WordPress.org approval.
"""
from __future__ import annotations
import argparse
from hashlib import sha256
from pathlib import Path
import re
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

OLD_SLUG = "brendigo-sidrena-cijena"
SLUG = "bazna-cjenici"
MAIN_FILE = "bazna-cjenici.php"
FIXED_DATE = (2026, 1, 1, 0, 0, 0)
TEXT_SUFFIXES = {".php", ".js", ".css", ".json", ".txt", ".html", ".md"}
README = """=== BAZNA – Bazne cijene i cjenici ===
Contributors: brendigo
Tags: bazne-cijene, cijene, cjenik, csv, woocommerce
Requires at least: 6.6
Tested up to: 7.1
Requires PHP: 7.4
Stable tag: {version}
License: GPLv2 or later
License URI: https://www.gnu.org/licenses/gpl-2.0.html

BAZNA by Brendigo: evidencija stvarnih povijesnih baznih cijena, javni HTML/CSV/XML cjenici i arhiva za WordPress ili WooCommerce.

== Description ==

BAZNA omogućuje upravljanje cijenama proizvoda i usluga, referentnim datumima, poslovnicama te javnim cjenicima i arhivom. Podržava samostalni WordPress i WooCommerce.

* Ručni unos bazne cijene na odgovarajući povijesni datum, na temelju stvarnih dokaza; ne izmišlja povijest cijena.
* Uređivanje proizvoda, usluga i poslovnica bez vanjskog servisa.
* Lokalni CSV, XML i XLSX uvoz/izvoz te javni HTML/CSV/XML cjenici.
* Arhiva najmanje 30 dana i automatizacija objave.
* Javni REST API samo za čitanje koji koristi BAZNA Android aplikacija.
* Bazna cijena razlikuje se od najniže cijene tijekom 30 dana prije sniženja.
* Besplatna, funkcionalna WordPress.org distribucija: ne zahtijeva licencni ključ ni aktivaciju vanjskog servisa.

== Installation ==

1. Instalirajte i aktivirajte dodatak BAZNA.
2. Otvorite BAZNA u WordPress administraciji i uredite Proizvode i poslovnice.
3. Provjerite dokumentirane povijesne cijene i datume prije objave.
4. Objavite javni cjenik i provjerite javne datoteke te arhivu.

Nije potreban WooCommerce za ručni katalog. WordPress.org paket ne sadrži pozive za provjeru licencnih ključeva. Ne aktivirajte oba BAZNA paketa istodobno: službeni ZIP i WordPress.org kandidat dijele kompatibilne klase, podatke i REST rute. WordPress.org distribucija zahtijeva zaseban službeni pregled.

== Frequently Asked Questions ==

= Jamči li dodatak pravnu usklađenost? =

Ne. Provjerite stvarne povijesne cijene, klasifikaciju robe/usluge, podatke u cjeniku i aktualne propise. Odluke NN 101/2026 i NN 110/2026 određuju 17. studenoga 2026. kao datum početka primjene; prijedlozi budućih pravilnika ne tretiraju se kao objavljen konačan propis.

= Šalju li se podaci ili ključevi vanjskom servisu? =

Ne. Lokalni katalozi i cjenici obrađuju se na WordPress poslužitelju. Javno objavljeni cjenici dostupni su posjetiteljima i aplikaciji preko javnog API-ja.

= Mogu li koristiti WooCommerce? =

Da. Uređivanje i prikaz povezanih cijena obavlja se u lokalnom WordPressu.

== Changelog ==

= {version} =

* BAZNA WordPress.org-ready independent GPL build without license-only service or locked local functions.
* Supports published legal transition date 17 November 2026 with verified historical price data.
* Public read-only integration with BAZNA Android and responsive administrator tabs.
"""

def package(root: Path, output: Path, version: str) -> str:
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Invalid WordPress version")
    source = root / "wordpress" / OLD_SLUG
    if f" * Version: {version}\n" not in (source / (OLD_SLUG + ".php")).read_text():
        raise ValueError("Source plugin version differs from requested version")
    if any(p.is_symlink() for p in source.rglob("*")):
        raise ValueError("Symlinks are forbidden")
    items = []
    for path in sorted(p for p in source.rglob("*") if p.is_file()):
        relative = path.relative_to(source).as_posix()
        if relative == OLD_SLUG + ".php":
            relative = MAIN_FILE
        data = path.read_bytes()
        if path.suffix in TEXT_SUFFIXES:
            text = data.decode("utf-8")
            text = text.replace(OLD_SLUG, SLUG)
            if relative == "readme.txt":
                text = README.format(version=version)
            data = text.encode("utf-8")
        items.append((relative, data))
    if len(items) < 120:
        raise ValueError("Unexpectedly incomplete plugin package")
    paths = {name for name, _ in items}
    assert MAIN_FILE in paths and "includes/class-license.php" not in paths
    assert OLD_SLUG + ".php" not in paths
    contents = dict(items)
    assert all("licenca.brendigo.com" not in body.decode("utf-8")
               for name, body in items if name.endswith(".php"))
    assert "bazna-license-form" not in contents["includes/class-admin.php"].decode()
    assert "'licensing' =>" not in contents["includes/class-public-api.php"].decode()
    assert f"Text Domain: {SLUG}" in contents[MAIN_FILE].decode()
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "w", compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for relative, data in items:
            info = ZipInfo(SLUG + "/" + relative, date_time=FIXED_DATE)
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data, compress_type=ZIP_DEFLATED, compresslevel=9)
    with ZipFile(output) as archive:
        assert archive.testzip() is None
        assert len(archive.namelist()) == len(items)
    digest = sha256(output.read_bytes()).hexdigest()
    output.with_suffix(output.suffix + ".sha256").write_text(
        f"{digest}  {output.name}\n", encoding="utf-8",
    )
    return digest

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    print("BAZNA WordPress.org candidate SHA-256:",
          package(Path(__file__).resolve().parents[1], args.output, args.version))
