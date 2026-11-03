#!/usr/bin/env python3
"""Reproducible BAZNA PHP/JSON server archive; private secrets never go to CI.

Examples:
  python3 tools/package_license.py --version 1.5.12 --output artifacts/BAZNA-Licenca-Server-1.5.12.zip
  python3 tools/package_license.py --version 1.5.12 --output /secure/BAZNA-Licenca-PRIVATE.zip --private-config /secure/licenca/private/config.php

The private archive includes configuration, so NEVER publish it on GitHub.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import re
import stat
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "licenca"
REQUIRED = [
    "api.php", "lib.php", ".htaccess", "DEPLOY.md", "README.md",
    "private/.htaccess", "private/index.php", "private/config.example.php",
    "bin/generate.php", "bin/preflight.php", "bin/.htaccess",
    "tests/test.php", "tests/.htaccess", "nginx.conf.example",
]
FORBIDDEN = ("activations.json", "rate.json", "activation.lock", "rate.lock")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--output", type=Path, required=True)
    ap.add_argument("--private-config", type=Path, default=None)
    opts = ap.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+", opts.version):
        ap.error("Invalid semantic version.")
    paths = {}
    for relative in REQUIRED:
        source = SOURCE / relative
        if source.is_symlink() or not source.is_file():
            ap.error(f"Missing or unsafe source file {relative}")
        paths["licenca/" + relative] = source.read_bytes()
    if opts.private_config:
        source = opts.private_config.resolve()
        if not source.is_file() or source.is_symlink():
            ap.error("Private config file missing or unsafe.")
        raw = source.read_bytes()
        if not re.search(rb"['\"]master_secret_hex['\"]\s*=>\s*['\"][a-fA-F0-9]{64}['\"]", raw):
            ap.error("Private config does not contain a valid 256-bit master secret.")
        paths["licenca/private/config.php"] = raw
    if opts.output.exists():
        ap.error("Output already exists. Use a new filename to avoid replacing prior packages.")
    opts.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(opts.output, "x", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(paths):
            if any(x in name for x in FORBIDDEN):
                raise ValueError("Runtime JSON data must never be packed!")
            info = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (stat.S_IFREG | 0o600) << 16 if name.endswith("/config.php") else (stat.S_IFREG | 0o644) << 16
            archive.writestr(info, paths[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    digest = hashlib.sha256(opts.output.read_bytes()).hexdigest()
    sum_path = opts.output.with_suffix(opts.output.suffix + ".sha256")
    sum_path.write_text(digest + "  " + opts.output.name + "\n", encoding="ascii")
    print(f"Packaged {len(paths)} files; SHA256 {digest}; private={bool(opts.private_config)}")


if __name__ == "__main__":
    main()
