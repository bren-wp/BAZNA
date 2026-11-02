#!/usr/bin/env python3
"""Compare actual SIDRENA screenshots with supplied reference PNGs.

This is a QA-only utility (requires Pillow); it is NOT bundled into the
Android APK/AAB or WordPress plugin. No artificial screenshots or references
are generated. Exact 1:1 means zero changed pixels at equal resolution.

Examples:
  python3 tools/compare_ui_reference.py ref/01-splash.png actual/01-splash.png --diff-dir artifacts/ui-diff
  python3 tools/compare_ui_reference.py ref-360/ actual-360/ --diff-dir artifacts/ui-diff

Any missing reference, viewport mismatch, or excessive difference fails CI.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image, ImageChops, ImageDraw, ImageStat
except ImportError as exc:
    raise SystemExit("Install QA dependency: python3 -m pip install Pillow") from exc


def files_to_compare(reference: Path, actual: Path) -> list[tuple[Path, Path]]:
    if reference.is_file() and actual.is_file():
        return [(reference, actual)]
    if reference.is_dir() and actual.is_dir():
        reference_files = sorted(reference.glob("*.png"))
        actual_files = {path.name for path in actual.glob("*.png")}
        expected = {path.name for path in reference_files}
        if not reference_files:
            raise ValueError("No reference PNG files in " + str(reference))
        missing = expected - actual_files
        extra = actual_files - expected
        if missing or extra:
            raise ValueError(
                "Screenshot filename mismatch: missing=" + str(sorted(missing))
                + ", extra=" + str(sorted(extra))
            )
        return [(p, actual / p.name) for p in reference_files]
    raise ValueError("Inputs must both be PNG files or both be directories")


def compare(
    reference: Path, actual: Path, ignore_top: int, ignore_bottom: int,
    diff_file: Path | None,
) -> tuple[float, float, tuple[int, int, int, int] | None]:
    with Image.open(reference) as ref_img, Image.open(actual) as actual_img:
        if ref_img.size != actual_img.size:
            raise ValueError(
                f"{actual.name}: viewport mismatch: reference={ref_img.size}, "
                f"actual={actual_img.size}"
            )
        width, height = ref_img.size
        if ignore_top < 0 or ignore_bottom < 0 or ignore_top + ignore_bottom >= height:
            raise ValueError("Invalid system-bar mask height")

        # Compare screenshot RGB after compositing any alpha on opaque navy.
        navy = (7, 24, 45, 255)
        def flatten(im: Image.Image) -> Image.Image:
            rgba = im.convert("RGBA")
            base = Image.new("RGBA", im.size, navy)
            return Image.alpha_composite(base, rgba).convert("RGB")

        delta = ImageChops.difference(flatten(ref_img), flatten(actual_img))
        if ignore_top:
            ImageDraw.Draw(delta).rectangle((0, 0, width - 1, ignore_top - 1), fill=(0, 0, 0))
        if ignore_bottom:
            ImageDraw.Draw(delta).rectangle(
                (0, height - ignore_bottom, width - 1, height - 1), fill=(0, 0, 0)
            )

        channel_masks = [
            channel.point(lambda value: 255 if value != 0 else 0)
            for channel in delta.split()
        ]
        changed = ImageChops.lighter(
            ImageChops.lighter(channel_masks[0], channel_masks[1]), channel_masks[2]
        )
        counted_pixels = width * (height - ignore_top - ignore_bottom)
        changed_pixels = ImageStat.Stat(changed).sum[0] / 255
        changed_percentage = 100.0 * changed_pixels / counted_pixels
        # Ignore masked bars when computing average channel error.
        mean_absolute_error = sum(ImageStat.Stat(delta).sum) / (3 * counted_pixels)
        bbox = changed.getbbox()

        if diff_file is not None:
            diff_file.parent.mkdir(parents=True, exist_ok=True)
            delta.save(diff_file)
        return mean_absolute_error, changed_percentage, bbox


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path, help="original PNG or directory")
    parser.add_argument("actual", type=Path, help="emulator/browser PNG or directory")
    parser.add_argument("--diff-dir", type=Path, help="write difference PNGs here (not to chat)")
    parser.add_argument("--ignore-top", type=int, default=0, metavar="PX")
    parser.add_argument("--ignore-bottom", type=int, default=0, metavar="PX")
    parser.add_argument("--max-mae", type=float, default=0.0,
                        help="max allowed average RGB error, default exact match")
    parser.add_argument("--max-changed-pct", type=float, default=0.0,
                        help="max allowed percentage of changed pixels, default 0")
    args = parser.parse_args()
    if args.max_mae < 0 or args.max_changed_pct < 0:
        parser.error("Tolerances cannot be negative")
    try:
        pairs = files_to_compare(args.reference, args.actual)
        failures = 0
        for ref_file, actual_file in pairs:
            diff_file = (
                args.diff_dir / ("diff-" + ref_file.name)
                if args.diff_dir else None
            )
            mae, pct, bbox = compare(
                ref_file, actual_file, args.ignore_top, args.ignore_bottom,
                diff_file,
            )
            passed = mae <= args.max_mae and pct <= args.max_changed_pct
            failures += not passed
            state = "PASS" if passed else "FAIL"
            print(
                f"{state} {ref_file.name}: MAE={mae:.6f} "
                f"changed={pct:.4f}% bbox={bbox}"
            )
        print(f"Compared {len(pairs)} pair(s); {failures} failed.")
        return 1 if failures else 0
    except (OSError, ValueError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
