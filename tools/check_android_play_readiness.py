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
# Version is governed by the same-commit unified release contract, not a stale pinned test.
version_match = re.search(r'\bversionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"', gradle)
version_name = version_match.group(1) if version_match else ""
release_yaml = (root / ".github/workflows/desktop-release.yml").read_text(encoding="utf-8")
check(number("versionCode") > 27, "Google Play update must increment versionCode beyond prior release 27")
check(bool(version_name) and f"ANDROID_VERSION: '{version_name}'" in release_yaml,
      "Android versionName must match the unified BAZNA release on the same commit")
check(number("versionCode") > 0, "A positive versionCode is required")
check('applicationId = "com.brendigo.bazna"' in gradle, "BAZNA application ID must be com.brendigo.bazna (new Play listing)")
check("isMinifyEnabled = true" in gradle and "isShrinkResources = true" in gradle,
      "Release shrinker and resource minimization must remain enabled")
check("bundleRelease" in ci and "lintDebug" in ci and "testDebugUnitTest" in ci,
      "Android CI must build an AAB, run lint and unit tests")
# Store policy preflight is deliberately limited to build facts, not a claim of approval.
entry = root / "android/app/src/main/java/com/brendigo/bazna/MainActivity.kt"
viewmodel = root / "android/app/src/main/java/com/brendigo/bazna/SidrenaViewModel.kt"
repository = root / "android/app/src/main/java/com/brendigo/bazna/data/CatalogRepository.kt"
refresh_policy = root / "android/app/src/main/java/com/brendigo/bazna/data/ForegroundRefreshPolicy.kt"
refresh_test = root / "android/app/src/test/java/com/brendigo/bazna/data/ForegroundRefreshPolicyTest.kt"
check(all(p.exists() for p in (entry, viewmodel, repository, refresh_policy, refresh_test)),
      "Foreground-only public feed refresh and test must be present")
if entry.exists() and viewmodel.exists() and repository.exists():
    check("onResume()" in entry.read_text(encoding="utf-8") and
          "refreshIfStale()" in entry.read_text(encoding="utf-8") and
          "ForegroundRefreshPolicy.shouldRefresh" in viewmodel.read_text(encoding="utf-8"),
          "Returning to Android foreground must check whether published prices need revalidation")
    networking = repository.read_text(encoding="utf-8")
    check('header("Cache-Control", "no-cache, max-age=0")' in networking and
          ".followRedirects(false)" in networking and ".dns(PublicDns)" in networking,
          "Public WordPress feed refresh must revalidate current data and protect network boundaries")
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
