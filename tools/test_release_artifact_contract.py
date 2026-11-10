#!/usr/bin/env python3
"""Prevent release failures caused by stale CI artifact names or version drift.

Offline, read-only release preflight. GitHub Actions does all build verification
separately; this script guards the handoff between successful workflows.
"""
from pathlib import Path
import re
import sys
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def read(file: str) -> str:
    return (ROOT / file).read_text(encoding="utf-8")


def require(condition: bool, description: str) -> None:
    if not condition:
        errors.append(description)


def main_push_is_unconditional(yaml: str) -> bool:
    # A release is created from the Android run. WordPress and adapters must
    # run on each main commit, even when only the release workflow changed.
    match = re.search(r"(?ms)^  push:\n(.*?)(?=^\S|\Z)", yaml)
    if match is None:
        return False
    block = match.group(1)
    return ("branches: [main]" in block
            and "paths:" not in block
            and "paths-ignore:" not in block)


wp_ci = read(".github/workflows/official-plugin-ci.yml")
android_ci = read(".github/workflows/android-ci.yml")
windows_ci = read(".github/workflows/desktop-ci.yml")
multi_ci = read(".github/workflows/multiplatform-ci.yml")
release_ci = read(".github/workflows/desktop-release.yml")
android_gradle = read("android/app/build.gradle.kts")
wp_php = read("wordpress/brendigo-sidrena-cijena/brendigo-sidrena-cijena.php")
wp_readme = read("wordpress/brendigo-sidrena-cijena/readme.txt")
web_core = read("web/src/Core.php")
joomla_manifest = ET.fromstring(read("joomla/plg_system_bazna/bazna.xml"))
packager = read("tools/package_platforms.py")

android_match = re.search(r'versionName\s*=\s*"([^"]+)"', android_gradle)
wp_match = re.search(r"^ \* Version: ([0-9.]+)\s*$", wp_php, re.MULTILINE)
web_match = re.search(r"public const VERSION = '([^']+)';", web_core)
android_version = android_match.group(1) if android_match else ""
wp_version = wp_match.group(1) if wp_match else ""
web_version = web_match.group(1) if web_match else ""

require(bool(android_version) and android_version == wp_version,
        "Android and WordPress must share the next unified release version")
require(f"RELEASE_VERSION: '{wp_version}'" in release_ci,
        "Release tag must match WordPress version")
require(f"ANDROID_VERSION: '{android_version}'" in release_ci and
        f"WORDPRESS_VERSION: '{wp_version}'" in release_ci,
        "Release version variables must match the actual source")
require(f"Stable tag: {wp_version}" in wp_readme,
        "WordPress stable tag does not match release version")
require(web_version == joomla_manifest.findtext("version"),
        "Shared Web core and Joomla manifest versions differ")
require(f"WEB_JOOMLA_SHOPIFY_VERSION: '{web_version}'" in release_ci,
        "Cross-platform release version does not match the shared Core")
require(all(f"BAZNA-{platform}-$WEB_JOOMLA_SHOPIFY_VERSION.zip" in release_ci
            for platform in ("Web", "Joomla-Plugin", "Shopify-Adapter")),
        "The release must list all three adapters using their exact version")
require(all(f"BAZNA-{platform}-{web_version}.zip" in packager
            for platform in ("Web", "Joomla-Plugin", "Shopify-Adapter")),
        "Build filenames differ from release adapter filenames")

# Artifacts are uploaded by one workflow and downloaded by another.
# No historical guesses or aliases: the producer must be the source of truth.
for producer, consumer, name in [
    (android_ci, release_ci, "BAZNA-Android-unsigned-build"),
    (windows_ci, release_ci, "SIDRENA-Desktop-release-binaries"),
    (multi_ci, release_ci, "BAZNA-web-joomla-shopify-candidate"),
]:
    require(f"name: {name}" in producer,
            f"Producer no longer publishes artifact {name}")
    require(f"--name '{name}'" in consumer,
            f"Release downloads an obsolete or unexpected artifact name: {name}")

for name in (
    "BAZNA-Android-debug.apk",
    "BAZNA-Android-unsigned.aab",
    "BAZNA-Android-SHA256SUMS.txt",
    "SIDRENA-Desktop-win-x64-Portable.exe",
    "SIDRENA-Desktop-win-x64-Setup.exe",
    "SIDRENA-Desktop-win-x64-SHA256SUMS.txt",
    "SIDRENA-Desktop-release-info.json",
):
    require(name in release_ci, f"Release missing required asset: {name}")
    require(name in (android_ci if name.startswith("BAZNA-Android") else windows_ci),
            f"Producer does not stage required asset: {name}")

for workflow in ("official-plugin-ci.yml", "repository-audit.yml",
                 "multiplatform-ci.yml"):
    require(workflow in release_ci,
            f"Release does not await successful same-commit {workflow}")
require(main_push_is_unconditional(wp_ci),
        "WordPress checks must run on every main commit for release gates")
require(main_push_is_unconditional(multi_ci),
        "Web/Joomla/Shopify checks must run on every main commit for release gates")
require("head_sha=$CI_SHA" in release_ci,
        "Release must validate source-commit provenance")
require('sha256sum -c' in release_ci,
        "Release must validate ZIP checksums")
require('[[' in release_ci and '"$count" -eq 17' in release_ci,
        "Release must verify exactly 17 expected assets")
require("--prerelease" in release_ci,
        "Third-party adapter candidates must not be advertised as completed store apps")
require("SIDRENA-Android-unsigned-build" not in release_ci,
        "Legacy Android artifact name will break release download")
require("release/*" in release_ci, "Release command must upload verified assets")

if errors:
    for error in errors:
        print("FAIL:", error)
    sys.exit(1)

print("PASS: BAZNA release 17-asset handoff, actual Android/Windows/adapter producers,"
      " legal versions, WordPress metadata, SHA-256 and same-commit CI gates")
