#!/usr/bin/env python3
"""Regression guard for public BAZNA branding and release metadata.

Legacy code identifiers, REST paths and WordPress shortcodes must remain
backward compatible; this check targets user-visible copy and documentation.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


wp = read("wordpress/brendigo-sidrena-cijena/brendigo-sidrena-cijena.php")
android = read("android/app/build.gradle.kts")
plugin_readme = read("wordpress/brendigo-sidrena-cijena/readme.txt")
main_readme = read("README.md")
wp_version = re.search(r"^ \* Version: (\d+\.\d+\.\d+)\s*$", wp, re.M)
android_version = re.search(r'versionName\s*=\s*"(\d+\.\d+\.\d+)"', android)
if not wp_version or not android_version:
    errors.append("Missing source versions")
else:
    version = wp_version.group(1)
    if android_version.group(1) != version:
        errors.append("Android / WordPress release version mismatch")
    if f"Stable tag: {version}" not in plugin_readme:
        errors.append("WordPress readme stable tag mismatch")
    if f"**{version}**" not in main_readme:
        errors.append("Root README does not show the current version")

if not main_readme.startswith("# BAZNA\n"):
    errors.append("Root README still uses the legacy product name")
for path, contents in (
    ("README.md", main_readme),
    ("android/README.md", read("android/README.md")),
    ("android/RELEASE.md", read("android/RELEASE.md")),
):
    if "https://github.com/bren-wp/SIDRENA/releases" in contents:
        errors.append(f"Stale release URL: {path}")
    if "https://github.com/bren-wp/BAZNA/releases" not in contents:
        errors.append(f"Missing BAZNA release URL: {path}")

checks = {
    "android/app/src/main/java/com/brendigo/bazna/ui/Components.kt": [
        'Text("SIDRENA"',
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/ServiceCatalogScreen.kt": [
        "WordPress dodatku SIDRENA",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/PublicProductImage.kt": [
        '"SIDRENA-Android"',
    ],
    "wordpress/brendigo-sidrena-cijena/includes/admin/tab-upute.php": [
        "SIDRENA ZIP",
        "Brendigo Sidrena Cijena →",
        "SIDRENA dodaje",
        "Sidrenu cijenu unosite",
    ],
    "wordpress/brendigo-sidrena-cijena/includes/class-product-fields.php": [
        "Brendigo Sidrena Cijena",
        "'Sidrena cijena',",
    ],
    "wordpress/brendigo-sidrena-cijena/includes/class-feed-routes.php": [
        "Arhiva sidrene cijene",
        "Cjenik - sidrena cijena",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/SearchScreen.kt": [
        '"Sidrena: "',
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/HomeScreens.kt": [
        'Text("Sidrene cijene',
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/ProductCatalogScreen.kt": [
        '"Sa sidrenom cijenom"',
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/CatalogScreens.kt": [
        '"Sidrena cijena:',
        '"Datum sidrene cijene:',
    ],
}
for path, old_names in checks.items():
    content = read(path)
    for old_name in old_names:
        if old_name in content:
            errors.append(f"Legacy public label in {path}: {old_name}")


# Public media file names and drawable resource IDs must not expose the
# former brand. Internal Java/Kotlin namespaces, WP option keys and REST v1
# routes intentionally stay stable for update and data compatibility.
required_media = (
    "android/app/src/main/res/drawable/ic_bazna_mark.xml",
    "desktop/branding/bazna-banner.svg",
    "desktop/branding/bazna-feature.svg",
    "wordpress/brendigo-sidrena-cijena/assets/images/bazna-logo.png",
    "wordpress/brendigo-sidrena-cijena/assets/images/bazna-brand-mark.png",
)
for media in required_media:
    if not (ROOT / media).is_file():
        errors.append(f"Missing BAZNA public media: {media}")
obsolete_media = (
    "android/app/src/main/res/drawable/ic_sidrena_mark.xml",
    "desktop/branding/sidrena-banner.svg",
    "desktop/branding/sidrena-feature.svg",
    "wordpress/brendigo-sidrena-cijena/assets/images/sidrena-logo.png",
    "wordpress/brendigo-sidrena-cijena/assets/images/sidrena-brand-mark.png",
)
for media in obsolete_media:
    if (ROOT / media).exists():
        errors.append(f"Obsolete public media still packaged: {media}")

visible_copies = {
    "android/app/src/main/java/com/brendigo/bazna/ui/AboutScreen.kt": [
        '"WordPress dodatak"', '"WordPress stranica"',
        "ZAKONSKI STATUS NA 2. 11. 2026.",
        "Pravilnik je još u savjetovanju do 13.",
        "ic_sidrena_mark",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/HomeScreens.kt": [
        "Objavite promjene u WordPressu", "u WordPress dodatku", "ic_sidrena_mark",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/Components.kt": ["ic_sidrena_mark"],
    "android/app/src/main/java/com/brendigo/bazna/ui/SidrenaApp.kt": [
        "mailto:sidrena@", "Podaci na WordPress stranici",
        "svoje WordPress stranice s aktivnim dodatkom",
        "izvornoj WordPress stranici",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/ServiceCatalogScreen.kt": [
        '"Objavite cjenik u WordPress', "na povezanoj WordPress stranici",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/LocationsScreen.kt": [
        "Adrese se prikazuju samo kada su objavljene u WordPressu",
    ],
    "android/app/src/main/java/com/brendigo/bazna/ui/PublicPriceListsScreen.kt": [
        "WordPress ima neobjavljene izmjene",
        "CSV/XML cjenika u WordPress dodatku",
    ],
    "wordpress/brendigo-sidrena-cijena/assets/js/admin.js": [
        'prefix = "Sidrena cijena"',
    ],
    "wordpress/brendigo-sidrena-cijena/assets/js/admin-catalog.js": [
        "[sidrena-cijena id=", "redovna cijena i sidrena cijena",
    ],
    "wordpress/brendigo-sidrena-cijena/includes/admin/tab-postavke.php": [
        "u sidrenu ili baznu",
    ],
}
for path, tokens in visible_copies.items():
    content = read(path)
    for token in tokens:
        if token in content:
            errors.append(f"Visible legacy UI or stale platform copy: {path}: {token}")

if errors:
    for issue in errors:
        print("FAIL:", issue)
    sys.exit(1)

print("PASS: BAZNA public copy, compatible source version and release URLs")
