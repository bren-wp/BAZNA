#!/usr/bin/env python3
"""Backward-compatible WordPress.org packager for the existing registered slug.

The former implementation renamed the plugin to "bazna-cjenici", producing a
different WordPress plugin instead of an update to "brendigo-sidrena-cijena".
Both distribution commands now build exactly the same unmodified plugin bytes.
WordPress.org itself receives individual files via SVN, not this ZIP.
"""
from __future__ import annotations

import argparse
from pathlib import Path

from package_plugin import package


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    print("Existing WordPress.org slug ZIP SHA-256:",
          package(Path(__file__).resolve().parents[1], args.output, args.version))
