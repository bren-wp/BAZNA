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
    "android/app/src/main/java/com/brendigo/sidrena/ui/Components.kt": [
        'Text("SIDRENA"',
    ],
    "android/app/src/main/java/com/brendigo/sidrena/ui/ServiceCatalogScreen.kt": [
        "WordPress dodatku SIDRENA",
    ],
    "android/app/src/main/java/com/brendigo/sidrena/ui/PublicProductImage.kt": [
        '"SIDRENA-Android"',
    ],
    "wordpress/brendigo-sidrena-cijena/includes/admin/tab-upute.php": [
        "SIDRENA ZIP",
        "Brendigo Sidrena Cijena →",
        "SIDRENA dodaje",
    ],
}
for path, old_names in checks.items():
    content = read(path)
    for old_name in old_names:
        if old_name in content:
            errors.append(f"Legacy public label in {path}: {old_name}")

if errors:
    for issue in errors:
        print("FAIL:", issue)
    sys.exit(1)

print("PASS: BAZNA public copy, compatible source version and release URLs")
