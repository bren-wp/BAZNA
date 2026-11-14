#!/usr/bin/env python3
"""Build Play Store images from actual BAZNA Android emulator captures.

Requires pillow, cairosvg. Does not synthesize application interfaces.
CI supplies 12 screenshot PNGs for reference-393, tablet-600 and
landscape-1280 in a directory created by run_android_responsive.sh.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from io import BytesIO
from pathlib import Path

import cairosvg
from PIL import Image, ImageDraw, ImageFont, ImageFilter

NAVY = (8, 24, 43)
WHITE = (248, 251, 255)
ORANGE = (255, 133, 44)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
# Same 108x108 B vector as android/app/src/main/res/drawable/ic_launcher_foreground.xml
B_PATH = ("M28,13 L59,13 C78,13 87,22 87,35 C87,43 82,49 77,52 "
          "C88,58 92,65 92,78 C92,95 81,105 59,105 L28,105 Z "
          "M45,30 L57,30 C66,30 71,34 71,40 C71,46 66,50 57,50 L45,50 Z "
          "M45,66 L57,66 C68,66 76,72 76,79 C76,87 68,90 57,90 L45,90 Z")
ALL = [
    ("01-splash", "BAZNA", "Cijene pod kontrolom"),
    ("02-connect", "Povežite svoju stranicu", "Javni podaci putem HTTPS-a"),
    ("03-home", "Sve na jednom mjestu", "Pregled objavljenih cjenika"),
    ("04-products", "Objavljeni proizvodi", "Cijene i dostupnost"),
    ("05-services", "Usluge na jednom mjestu", "Pregled javnog kataloga"),
    ("06-detail", "Trenutačna i bazna cijena", "Datumi i detalji proizvoda"),
    ("07-documents", "Javni cjenici", "Pristup objavljenim datotekama"),
    ("08-archive", "Povijest cjenika", "Arhiva prethodnih objava"),
    ("09-outlets", "Sve vaše poslovnice", "Odaberite poslovnicu ili webshop"),
    ("10-search", "Brzo pronađite stavke", "Pretraživanje javnih podataka"),
    ("11-settings", "Jednostavne postavke", "Povezivanje i osvježavanje"),
    ("12-about", "BAZNA by BRENDIGO", "Informacije i pomoć"),
]
PHONE = ["03-home", "04-products", "06-detail", "05-services",
         "07-documents", "08-archive", "09-outlets", "02-connect"]
TABLET7 = ["03-home", "04-products", "06-detail", "08-archive"]
TABLET10 = ["03-home", "04-products", "07-documents", "11-settings"]

def ft(size, strong=False):
    return ImageFont.truetype(BOLD if strong else FONT, size)

def mark(size, fill=ORANGE):
    c = "#%02x%02x%02x" % fill
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
           f'viewBox="0 0 108 108"><path fill="{c}" fill-rule="evenodd" d="{B_PATH}"/></svg>')
    return Image.open(BytesIO(cairosvg.svg2png(bytestring=svg.encode(),
                  output_width=size*2, output_height=size*2))).convert("RGBA").resize(
                      (size, size), Image.Resampling.LANCZOS)

def bg(size):
    w, h = size
    im = Image.new("RGBA", size, NAVY + (255,))
    halo = Image.new("RGBA", size)
    d = ImageDraw.Draw(halo)
    d.ellipse((w-700, -200, w+350, 880), fill=(40, 98, 149, 85))
    im.alpha_composite(halo.filter(ImageFilter.GaussianBlur(200)))
    return im

def icon(path):
    im = bg((512, 512))
    im.alpha_composite(mark(360), (76, 73))
    im.save(path, optimize=True)
    assert im.size == (512, 512) and path.stat().st_size <= 1024*1024

def feature(path):
    im = bg((1024, 500))
    d = ImageDraw.Draw(im)
    d.text((65, 65), "BAZNA", font=ft(76, True), fill=WHITE)
    d.rounded_rectangle((68, 176, 143, 183), radius=3, fill=ORANGE)
    d.text((65, 209), "Cijene pod kontrolom.", font=ft(34, True), fill=WHITE)
    d.text((65, 266), "Objavljeni cjenici, proizvodi i arhiva", font=ft(19), fill=(190, 214, 232))
    d.text((65, 298), "povezani s vašom WordPress stranicom.", font=ft(19), fill=(190, 214, 232))
    d.text((65, 416), "by BRENDIGO", font=ft(19, True), fill=ORANGE)
    d.rounded_rectangle((659, 102, 979, 445), radius=29,
                        fill=(21, 56, 84), outline=(72, 123, 167), width=2)
    d.text((684, 129), "Javni cjenici", font=ft(26, True), fill=WHITE)
    for i, (kind, ext) in enumerate((("Proizvodi", "CSV"), ("Usluge", "CSV"), ("Arhiva", "XML"))):
        y = 201 + 72*i
        d.rounded_rectangle((680, y, 957, y+63), radius=12, fill=(32, 78, 109))
        d.rounded_rectangle((693, y+14, 732, y+48), radius=8, fill=(63, 130, 160))
        d.text((698, y+24), ext, font=ft(11, True), fill=WHITE)
        d.text((750, y+17), kind, font=ft(20, True), fill=WHITE)
        d.ellipse((924, y+27, 936, y+39), fill=ORANGE)
    im.convert("RGB").save(path, optimize=True)

def frame(im, source, box):
    x, y, w, h = box
    shadow = Image.new("RGBA", im.size)
    ImageDraw.Draw(shadow).rounded_rectangle((x-12, y-12, x+w+12, y+h+12),
                                             radius=38, fill=(0, 0, 0, 150))
    im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(25)))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((x-5, y-5, x+w+5, y+h+5),
                        radius=33, fill=(67, 112, 149))
    source = source.resize((w, h), Image.Resampling.LANCZOS).convert("RGBA")
    mask = Image.new("L", (w, h))
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w-1, h-1), radius=27, fill=255)
    im.paste(source, (x, y), mask)

def portrait(src, path, title, subtitle, tablet=False):
    out = bg((1080, 1920))
    out.alpha_composite(mark(92), (67, 62))
    d = ImageDraw.Draw(out)
    d.text((170, 82), "BAZNA", font=ft(43, True), fill=WHITE)
    d.text((73, 175), title, font=ft(34 if len(title)>26 else 39, True), fill=WHITE)
    d.text((75, 231), subtitle, font=ft(23), fill=(192, 215, 230))
    sw, sh = src.size
    scale = min((875 if tablet else 770)/sw, (1470 if tablet else 1550)/sh)
    w, h = round(sw*scale), round(sh*scale)
    frame(out, src, ((1080-w)//2, 308, w, h))
    d.text((75, 1878), "Stvarna snimka aplikacije · Primjer podataka",
           font=ft(17), fill=(177, 203, 222))
    out.convert("RGB").save(path, optimize=True)

def landscape(src, path, title):
    out = bg((1920, 1080))
    out.alpha_composite(mark(120), (71, 66))
    d = ImageDraw.Draw(out)
    d.text((194, 97), "BAZNA", font=ft(58, True), fill=WHITE)
    d.text((80, 242), title, font=ft(43, True), fill=WHITE)
    d.text((80, 336), "Javni podaci i cjenici", font=ft(26), fill=(190, 214, 232))
    d.text((80, 965), "by BRENDIGO", font=ft(24, True), fill=ORANGE)
    w = 1180
    frame(out, src, (622, 132, w, round(src.height*w/src.width)))
    d.text((621, 1035), "Stvarna snimka aplikacije · Primjer podataka",
           font=ft(17), fill=(182, 204, 223))
    out.convert("RGB").save(path, optimize=True)

def load_capture(root, group, stem):
    p = root / group / (stem + ".png")
    if not p.is_file():
        raise SystemExit(f"Missing real emulator screenshot: {p}")
    return Image.open(p).convert("RGB")

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--screenshots-dir", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    root = args.screenshots_dir
    out = args.output
    dirs = ("icon", "feature", "phone-upload", "all-screens",
            "tablet-7inch", "tablet-10inch", "launcher")
    for sub in dirs:
        (out/sub).mkdir(parents=True, exist_ok=True)
    icon(out/"icon"/"BAZNA-icon-512.png")
    feature(out/"feature"/"BAZNA-feature-1024x500.png")
    for i, (key, title, subtitle) in enumerate(ALL, 1):
        src = load_capture(root, "reference-393", key)
        portrait(src, out/"all-screens"/f"{i:02d}-{key}.png", title, subtitle)
    details = {key:(title, subtitle) for key,title,subtitle in ALL}
    for i, key in enumerate(PHONE, 1):
        portrait(load_capture(root, "reference-393", key),
                 out/"phone-upload"/f"{i:02d}-{key}.png", *details[key])
    for i, key in enumerate(TABLET7, 1):
        portrait(load_capture(root, "tablet-600", key),
                 out/"tablet-7inch"/f"{i:02d}-{key}.png", *details[key], tablet=True)
    for i, key in enumerate(TABLET10, 1):
        landscape(load_capture(root, "landscape-1280", key),
                  out/"tablet-10inch"/f"{i:02d}-{key}.png", details[key][0])
    # Optional Android adaptive resources, already defined as vectors in the app.
    fg = Image.new("RGBA", (432, 432))
    fg.alpha_composite(mark(265), (83, 83))
    fg.save(out/"launcher"/"foreground-432.png", optimize=True)
    mono = Image.new("RGBA", (432, 432))
    mono.alpha_composite(mark(265, WHITE), (83, 83))
    mono.save(out/"launcher"/"monochrome-432.png", optimize=True)
    Image.new("RGB", (432, 432), (16, 36, 58)).save(out/"launcher"/"background-432.png", optimize=True)
    result = {}
    for f in sorted(out.rglob("*.png")):
        with Image.open(f) as image:
            rel = f.relative_to(out).as_posix()
            result[rel] = dict(width=image.width, height=image.height,
                               mode=image.mode, bytes=f.stat().st_size,
                               sha256=hashlib.sha256(f.read_bytes()).hexdigest())
    assert len(list((out/"phone-upload").glob("*.png"))) == 8
    assert len(list((out/"all-screens").glob("*.png"))) == 12
    assert len(list((out/"tablet-7inch").glob("*.png"))) == 4
    assert len(list((out/"tablet-10inch").glob("*.png"))) == 4
    assert result["icon/BAZNA-icon-512.png"]["bytes"] <= 1024*1024
    assert result["feature/BAZNA-feature-1024x500.png"]["mode"] == "RGB"
    (out/"manifest.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("PASS: generated", len(result), "Play Store assets from real emulator captures")

if __name__ == "__main__":
    main()
