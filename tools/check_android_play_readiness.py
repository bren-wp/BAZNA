#!/usr/bin/env python3
"""Fail CI for a known Play packaging or user-data compliance regression.

This is a source preflight, NOT a Play Console acceptance certificate.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
gradle = (root / "android/app/build.gradle.kts").read_text(encoding="utf-8")
manifest = ET.parse(root / "android/app/src/main/AndroidManifest.xml").getroot()
about = (root / "android/app/src/main/java/com/brendigo/bazna/ui/AboutScreen.kt").read_text(encoding="utf-8")
external = (root / "android/app/src/main/java/com/brendigo/bazna/ui/ExternalIntents.kt").read_text(encoding="utf-8")
ci = (root / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
ns = "{http://schemas.android.com/apk/res/android}"
errors = []

def check(ok: bool, message: str) -> None:
    if not ok:
        errors.append(message)

def number(name: str) -> int:
    match = re.search(r"\b" + name + r"\s*=\s*(\d+)", gradle)
    return int(match.group(1)) if match else -1

check(number("targetSdk") >= 36, "Google Play targetSdk must be at least 36")
check(number("minSdk") >= 24, "Play-protected package must use minSdk >= 24")
check(number("compileSdk") >= number("targetSdk"), "compileSdk must not be below targetSdk")
check(number("versionCode") == 23, "The release candidate must use Android versionCode 20")
check('versionName = "1.5.20"' in gradle, "Android release must have versionName 1.5.20")
check(number("versionCode") > 0, "A positive versionCode is required")
check('applicationId = "com.brendigo.bazna"' in gradle, "BAZNA application ID must be com.brendigo.bazna (new Play listing)")
check("isMinifyEnabled = true" in gradle and "isShrinkResources = true" in gradle,
      "Release shrinker and resource minimization must remain enabled")
check("bundleRelease" in ci and "lintDebug" in ci and "testDebugUnitTest" in ci,
      "Android CI must build an AAB, run lint and unit tests")
permissions = [p.get(ns + "name") for p in manifest.findall("uses-permission")]
check(permissions == ["android.permission.INTERNET"],
      "Unexpected permission: revisit Data safety and Play disclosures")
application = manifest.find("application")
check(application is not None, "Missing application element")
if application is not None:
    check(application.get(ns + "usesCleartextTraffic") == "false", "Block cleartext HTTP")
    check(application.get(ns + "allowBackup") == "false", "Review backup/data transfer before enabling")
check("Politika privatnosti" in about and "onPrivacy" in about,
      "In-app privacy policy entry must exist")
check('https://brendigo.com/politika-privatnosti/' in external,
      "Missing public privacy policy link; publish app-specific disclosures before Play submission")
kotlin_files = list((root / "android/app/src").rglob("*.kt"))
check(bool(kotlin_files), "Kotlin app/test sources missing")
for file in kotlin_files:
    relative = file.relative_to(root).as_posix()
    check("/com/brendigo/sidrena/" not in relative, f"Old Kotlin source folder: {relative}")
    check("com.brendigo.sidrena" not in file.read_text(encoding="utf-8"),
          f"Legacy package reference in: {relative}")
check("Verify generated app identity and release version" in ci,
      "CI must check the real built Android APK package ID")
if errors:
    for error in errors:
        print("FAIL:", error)
    sys.exit(1)
print("PASS: BAZNA Android Play source preflight (Play Console review still required)")
