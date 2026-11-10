#!/usr/bin/env python3
"""Fail closed when shipping a public image or visible interface copy from an old brand.

Only user-visible display strings are screened. Existing WordPress option keys,
shortcodes, Android package/class names and stable REST paths are compatibility
identifiers: changing them silently would break installed sites or updates.
"""
from __future__ import annotations

from pathlib import Path
import re
import sys
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1]
ERRORS: list[str] = []

PREVIOUS_BRAND_MIGRATION = "wordpress/brendigo-sidrena-cijena/includes/class-settings.php"
STRING_LITERAL = re.compile(r"""(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')""")
OBSOLETE_COPY = re.compile(
    r"(?i:\bsidren[aeiou]\s+cijen[a-zčć]*\b)|(?<![\w-])SIDRENA(?![\w-])"
)
DEVELOPER_COPY = re.compile(
    r"(?i)\blorem ipsum\b|\bdebug mode\b|\bdevelopment build\b"
    r"|\bdemo(?:nstracija)? mode\b|\btestni (?:build|prikaz)\b"
)
SOURCE_DIRS = (
    ROOT / "android/app/src/main/java/com/brendigo/sidrena/ui",
    ROOT / "wordpress/brendigo-sidrena-cijena/includes",
    ROOT / "wordpress/brendigo-sidrena-cijena/assets/js",
    ROOT / "web/public",
    ROOT / "web/src",
    ROOT / "joomla/plg_system_bazna",
    ROOT / "shopify/server",
    ROOT / "shopify/extensions",
)
SOURCE_EXTENSIONS = {".kt", ".php", ".js", ".liquid", ".html"}
PUBLIC_MEDIA_DIRS = (
    ROOT / "android/app/src/main/res",
    ROOT / "wordpress/brendigo-sidrena-cijena/assets",
    ROOT / "desktop/branding",
    ROOT / "branding",
    ROOT / "web/public/assets",
    ROOT / "shopify/extensions",
)
PUBLIC_MEDIA_EXTENSIONS = {".png", ".jpg", ".jpeg", ".svg", ".ico", ".webp", ".xml"}

def check_text(path: Path) -> None:
    relative = path.relative_to(ROOT).as_posix()
    code = path.read_text(encoding="utf-8-sig")
    is_wordpress_php = (
        path.suffix == ".php" and
        relative.startswith("wordpress/brendigo-sidrena-cijena/")
    )
    for line_number, line in enumerate(code.splitlines(), 1):
        stripped = line.lstrip()
        if not stripped or stripped.startswith(("//", "#", "*", "/*")):
            continue
        # WP metadata, imported historical CSV headers, REST aliases, options
        # and CSS IDs are not rendered labels. Limit PHP checks to actual
        # translated UI, raw emitted labels, and inline HTML.
        if is_wordpress_php and not re.search(
            r"(?:__|_e|_x|esc_html__|esc_attr__|esc_html_e|esc_attr_e)\s*\(|"
            r"\becho\s*['\"]|(?:>[^<]*<)", line
        ):
            continue
        for match in STRING_LITERAL.finditer(line):
            literal = match.group(0)
            flagged = OBSOLETE_COPY.search(literal) or DEVELOPER_COPY.search(literal)
            if flagged:
                ERRORS.append(
                    f"{relative}:{line_number}: obsolete public copy: {flagged.group(0)}"
                )


def check_images() -> None:
    for directory in PUBLIC_MEDIA_DIRS:
        if not directory.is_dir():
            ERRORS.append(f"Missing public media directory: {directory.relative_to(ROOT)}")
            continue
        for path in directory.rglob("*"):
            if path.is_file() and path.suffix.lower() in PUBLIC_MEDIA_EXTENSIONS:
                if "sidrena" in path.name.casefold():
                    ERRORS.append(f"Old branded media filename: {path.relative_to(ROOT)}")
    for path in (
        "branding/bazna-banner.png",
        "branding/bazna-icon.png",
        "android/app/src/main/res/drawable/ic_bazna_mark.xml",
        "wordpress/brendigo-sidrena-cijena/assets/icon-256x256.png",
        "wordpress/brendigo-sidrena-cijena/assets/images/bazna-logo.png",
    ):
        if not (ROOT / path).is_file():
            ERRORS.append(f"Missing current BAZNA media: {path}")
    icon = ROOT / "android/app/src/main/res/drawable/ic_bazna_mark.xml"
    if icon.is_file():
        try:
            root = ElementTree.parse(icon).getroot()
            if not root.tag.endswith("vector") or not any(
                node.tag.endswith("path") for node in root.iter()
            ):
                ERRORS.append("Android BAZNA mark is not a valid vector path")
        except ElementTree.ParseError:
            ERRORS.append("Android BAZNA mark is malformed XML")

def check_docs_and_packages() -> None:
    documents = (
        "README.md", "android/README.md", "android/RELEASE.md",
        "desktop/README.md", "web/README.md", "joomla/README.md",
        "shopify/README.md",
    )
    for relative in documents:
        source = ROOT / relative
        if not source.is_file():
            ERRORS.append(f"Missing public documentation: {relative}")
            continue
        body = source.read_text(encoding="utf-8")
        if re.search(r"https://github\.com/bren-wp/SIDRENA", body):
            ERRORS.append(f"Old public GitHub destination in {relative}")
        if re.search(r"mailto:sidrena@", body, re.I):
            ERRORS.append(f"Old public support address in {relative}")
        if re.search(r"(?m)^!\[[^\]]*\]\([^)]*sidrena-[^)]*\)", body, re.I):
            ERRORS.append(f"Old public README image in {relative}")
        if re.search(r"(?mi)^[-*]?\s*\x60?(?:SIDRENA-(?:Android|Desktop)|brendigo-sidrena-cijena-\d)", body):
            ERRORS.append(f"Old downloadable artifact referenced in {relative}")
    wp = (ROOT / "wordpress/brendigo-sidrena-cijena/readme.txt").read_text(
        encoding="utf-8"
    )
    sections = re.findall(r"(?m)^== Changelog ==$", wp)
    if len(sections) != 1:
        ERRORS.append(f"Expected one WordPress Changelog section, found {len(sections)}")
    changelog = wp.split("== Changelog ==")[-1]
    changelogs = re.findall(r"(?m)^= (\d+\.\d+\.\d+) =$", changelog)
    if len(changelogs) != len(set(changelogs)):
        ERRORS.append(f"Duplicate WordPress changelog version headings: {changelogs}")
    title = (ROOT / "README.md").read_text(encoding="utf-8")
    if not title.startswith("# BAZNA\n"):
        ERRORS.append("Marketing README does not use the BAZNA brand")

def check_runtime() -> None:
    for directory in SOURCE_DIRS:
        if not directory.is_dir():
            ERRORS.append(f"Missing runtime source directory: {directory.relative_to(ROOT)}")
            continue
        for path in directory.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in SOURCE_EXTENSIONS:
                continue
            if any(section in {"tests", "test", "fixtures"} for section in path.parts):
                continue
            check_text(path)

if __name__ == "__main__":
    check_runtime()
    check_images()
    check_docs_and_packages()
    if ERRORS:
        for error in ERRORS[:80]:
            print("FAIL:", error)
        if len(ERRORS) > 80:
            print(f"FAIL: ...and {len(ERRORS)-80} more findings")
        sys.exit(1)
    print("PASS: BAZNA public UI strings, brand images, release filenames,"
          " support links and unique WordPress changelog versions")
