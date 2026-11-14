#!/usr/bin/env python3
"""Deterministic portable BAZNA PHP / Joomla plugin / Shopify adapter bundles.

No secrets, node_modules, build cache or user data are packaged. Installs of
Joomla and Shopify require platform-specific testing/authorization separately.
"""
from __future__ import annotations
import argparse
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
FIXED = (2026, 1, 1, 0, 0, 0)


def files_under(folder: Path) -> list[Path]:
    if not folder.is_dir():
        raise ValueError(f"Missing source folder: {folder}")
    members = sorted(p for p in folder.rglob("*") if p.is_file())
    if any(p.is_symlink() for p in folder.rglob("*")):
        raise ValueError("Symlinks are forbidden in distribution packages")
    return [p for p in members if p.name != ".DS_Store"]


def archive(path: Path, members: dict[str, bytes]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if len(members) < 4:
        raise ValueError("Unexpectedly incomplete BAZNA platform package")
    with ZipFile(path, "w", ZIP_DEFLATED, compresslevel=9) as handle:
        for key in sorted(members):
            if key.startswith("/") or ".." in key.split("/"):
                raise ValueError("Unsafe archive member")
            info = ZipInfo(key, date_time=FIXED)
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            handle.writestr(info, members[key], compress_type=ZIP_DEFLATED, compresslevel=9)
    with ZipFile(path) as handle:
        assert handle.testzip() is None
        assert len(handle.namelist()) == len(members)
    path.with_suffix(path.suffix + ".sha256").write_text(
        f"{sha256(path.read_bytes()).hexdigest()}  {path.name}\n", encoding="utf-8"
    )


def build(target: Path) -> None:
    web_root = ROOT / "web"
    shop_root = ROOT / "shopify"
    joomla_root = ROOT / "joomla/plg_system_bazna"
    web = {
        "bazna-web/" + p.relative_to(web_root).as_posix(): p.read_bytes()
        for p in files_under(web_root)
        if not p.parts[-2:] == ("tests", "test.php")
    }
    archive(target / "BAZNA-Web-1.1.4.zip", web)

    joomla = {
        p.relative_to(joomla_root).as_posix(): p.read_bytes()
        for p in files_under(joomla_root)
    }
    joomla["vendor/Core.php"] = (web_root / "src/Core.php").read_bytes()
    joomla["vendor/LicenseClient.php"] = (web_root / "src/LicenseClient.php").read_bytes()
    joomla["vendor/AdminCatalog.php"] = (web_root / "src/AdminCatalog.php").read_bytes()
    joomla["assets/bazna.css"] = (web_root / "public/assets/bazna.css").read_bytes()
    assert "bazna.xml" in joomla and "services/provider.php" in joomla
    archive(target / "BAZNA-Joomla-Plugin-1.1.4.zip", joomla)

    shopify = {
        "bazna-shopify/shopify/" + p.relative_to(shop_root).as_posix(): p.read_bytes()
        for p in files_under(shop_root)
        if "tests" not in p.relative_to(shop_root).parts
    }
    for p in files_under(web_root / "src"):
        shopify["bazna-shopify/web/src/" + p.name] = p.read_bytes()
    archive(target / "BAZNA-Shopify-Adapter-1.1.4.zip", shopify)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", type=Path, default=ROOT / "artifacts/multiplatform")
    args = parser.parse_args()
    build(args.output_dir)
    print("PASS: reproducible BAZNA web, Joomla and Shopify packages")
