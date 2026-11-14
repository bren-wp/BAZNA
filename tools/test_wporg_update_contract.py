#!/usr/bin/env python3
"""Guard update-in-place for the existing WordPress.org plugin.

This is a source/distribution identity test, not a live SVN publish.
It deliberately does not build or rename the separate bazna-cjenici candidate.
"""
from __future__ import annotations

from pathlib import Path
import re
import sys
import tempfile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SLUG = "brendigo-sidrena-cijena"
SOURCE = ROOT / "wordpress" / SLUG
MAIN = SOURCE / (SLUG + ".php")
README = SOURCE / "readme.txt"
WORKFLOW = ROOT / ".github/workflows/deploy-wordpress-org.yml"


def validate() -> None:
    php = MAIN.read_text(encoding="utf-8")
    readme = README.read_text(encoding="utf-8")
    deploy = WORKFLOW.read_text(encoding="utf-8")

    versions = re.findall(r"^ \* Version: (\d+\.\d+\.\d+)\s*$", php, re.MULTILINE)
    if len(versions) != 1:
        raise AssertionError("Exactly one semantic Plugin Version header is required")
    version = versions[0]
    assert re.search(r"^Stable tag: " + re.escape(version) + r"\s*$", readme, re.MULTILINE)
    assert "Text Domain: " + SLUG in php, "Existing translations must keep the same text domain"
    assert f"BRENDIGO_SIDRENA_CIJENA_VERSION', '{version}'" in php
    assert "Update URI:" not in php, "A third-party updater must not override WordPress.org"
    assert "defined( 'ABSPATH' ) || exit;" in php
    assert "register_activation_hook( __FILE__" in php, "Keep existing activation hook"
    assert "SLUG: " + SLUG in deploy
    assert "BUILD_DIR: wordpress/" + SLUG in deploy
    assert "10up/action-wordpress-plugin-deploy@2.3.0" in deploy
    assert "SVN_USERNAME:" in deploy and "SVN_PASSWORD:" in deploy
    assert "bazna-cjenici" not in deploy, "Do not deploy the alternate package as an update"
    assert "git diff --quiet" in deploy, "Prevent unpublished code from reaching WordPress.org"
    assert "gh release view" in deploy, "Deploy only a published GitHub version"

    from package_plugin import package

    with tempfile.TemporaryDirectory(prefix="bazna-wporg-") as tmp:
        archive = Path(tmp) / f"{SLUG}-{version}.zip"
        package(ROOT, archive, version)
        source_files = {p.relative_to(SOURCE).as_posix(): p
                        for p in SOURCE.rglob("*") if p.is_file()}
        with ZipFile(archive) as zipped:
            names = set(zipped.namelist())
            expected = {SLUG + "/" + name for name in source_files}
            assert names == expected, "ZIP must have exactly one existing plugin root directory"
            assert zipped.testzip() is None
            for name, path in source_files.items():
                assert zipped.read(SLUG + "/" + name) == path.read_bytes(), (
                    "WordPress.org must use unchanged source files: " + name
                )
            assert SLUG + "/" + SLUG + ".php" in names

    print(f"PASS: WordPress.org in-place update contract for {SLUG} {version}")


if __name__ == "__main__":
    try:
        validate()
    except (AssertionError, OSError, ValueError) as exc:
        sys.exit("FAIL: " + str(exc))
