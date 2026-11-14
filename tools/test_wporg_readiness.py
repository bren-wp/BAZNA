#!/usr/bin/env python3
"""Static WP package readiness check, not official Plugin Check approval."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[1]
src = ROOT / "wordpress/brendigo-sidrena-cijena"
readme = (src / "readme.txt").read_text(encoding="utf-8")
license_text = (src / "LICENSE").read_text(encoding="utf-8")
credits = (src / "ORIGINAL-CREDITS.txt").read_text(encoding="utf-8")
php = {p.relative_to(src).as_posix(): p.read_text(encoding="utf-8")
       for p in src.rglob("*.php")}
for marker in ("Jure Zadravec", "MJDD", "GPL"):
    assert marker in credits and marker in license_text, marker
assert "GPLv2 or later" in readme and "ORIGINAL-CREDITS.txt" in readme
assert "Does BAZNA require a license key" in readme
current_faq = readme.split("== Frequently Asked Questions ==", 1)[1].split("== Upgrade Notice ==", 1)[0]
assert "private license service receive" not in current_faq.lower()
assert "licenca.brendigo.com" not in current_faq
assert "includes/class-license.php" not in php
all_php = "\n".join(php.values())
for forbidden in ("Brendigo_Sidrena_Cijena_License::", "licenca.brendigo.com"):
    assert forbidden not in all_php, "stale runtime license dependency: " + forbidden
assert re.search(r"\bwp_verify_nonce\b|\bcheck_admin_referer\b",all_php), "WordPress nonce validation missing"
assert re.search(r"\bcurrent_user_can\b",all_php), "WordPress capability validation missing"
assert "register_rest_route" in all_php, "Android public REST API missing"
print("PASS: GPL attribution, no license runtime, nonce/capability basics, public API, current FAQ")
