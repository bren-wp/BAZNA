#!/usr/bin/env python3
"""Strict pixel-by-pixel validation against actual, user-approved screenshots.

Run after downloading CI capture artifacts:
  python3 -m pip install Pillow
  python3 tools/verify_visual_references.py QA/approved-refs QA/new-captures

Every PNG in the reference directory must have a same-named capture.
No baseline is invented and no image is silently accepted on first run.
"""
from __future__ import annotations

import json
from pathlib import Path
import sys

try:
    from PIL import Image, ImageChops
except ImportError:
    print("ERROR: Pillow must be installed; cannot prove pixel parity without pixel decoding", file=sys.stderr)
    sys.exit(2)

def run(reference_dir: Path, capture_dir: Path) -> int:
    if not reference_dir.is_dir() or not capture_dir.is_dir():
        print("FAIL: reference and capture directories must both exist")
        return 1
    references = sorted(reference_dir.rglob("*.png"))
    if not references:
        print("UNVERIFIED: no approved reference screenshots; not a pixel-perfect result")
        return 3
    findings = []
    for baseline in references:
        relative = baseline.relative_to(reference_dir)
        actual = capture_dir / relative
        if not actual.is_file():
            findings.append({"screen": relative.as_posix(), "status": "missing_capture"})
            continue
        with Image.open(baseline) as source_image, Image.open(actual) as capture_image:
            if source_image.size != capture_image.size:
                findings.append({
                    "screen": relative.as_posix(), "status": "size_mismatch",
                    "reference_size": source_image.size, "capture_size": capture_image.size,
                })
                continue
            source = source_image.convert("RGBA")
            capture = capture_image.convert("RGBA")
            diff = ImageChops.difference(source, capture)
            pixels = sum(1 for rgb in diff.getdata() if rgb != (0, 0, 0, 0))
            findings.append({
                "screen": relative.as_posix(), "status": "match" if pixels == 0 else "pixel_mismatch",
                "pixels_different": pixels, "total_pixels": source.width * source.height,
            })
    print(json.dumps({
        "reference_count": len(references),
        "all_exact": all(row["status"] == "match" for row in findings),
        "screens": findings,
    }, ensure_ascii=False, indent=2))
    return 0 if all(row["status"] == "match" for row in findings) else 1

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: verify_visual_references.py APPROVED_REFERENCES_DIR CI_CAPTURES_DIR", file=sys.stderr)
        sys.exit(2)
    sys.exit(run(Path(sys.argv[1]), Path(sys.argv[2])))
