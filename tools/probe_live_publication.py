#!/usr/bin/env python3
"""Opt-in external audit of a REAL published BAZNA WordPress site.

Run ONLY against a domain you control:
  python3 tools/probe_live_publication.py https://example.com --kind proizvodi

This tests anonymous HTTP availability and CSV/XML schemas. Merchant evidence,
legal exceptions, hosting schedules, and a full 15-day observation still require
independent production verification.
"""
from __future__ import annotations

import argparse
import csv
import io
import ipaddress
import json
import re
import socket
import sys
import urllib.error
import urllib.request
from datetime import datetime
from html.parser import HTMLParser
from urllib.parse import urlsplit, urljoin
from zoneinfo import ZoneInfo

MAX_BYTES = 12 * 1024 * 1024
REQUIRED = {
    "proizvodi": ["naziv", "sku", "marka", "neto_kolicina", "jedinica_mjere",
                 "maloprodajna_cijena", "poseban_oblik_prodaje", "cijena_jedinice_mjere",
                 "barkod", "ean_barkod", "bazna_cijena"],
    "usluge": ["naziv", "maloprodajna_cijena", "poseban_oblik_prodaje", "bazna_cijena"],
}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        return None


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.items = []

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            value = dict(attrs).get("href")
            if value:
                self.items.append(value)


def verify_site(value: str) -> str:
    site = urlsplit(value)
    if site.scheme != "https" or not site.hostname or site.username or site.password or site.query or site.fragment:
        raise ValueError("Odaberite kanonsku HTTPS domenu trgovca bez lozinki ili parametara.")
    if site.port not in (None, 443):
        raise ValueError("Javni cjenik mora koristiti HTTPS na standardnoj javnoj adresi.")
    # Conservative preflight for accidental use with private or metadata hosts.
    for info in socket.getaddrinfo(site.hostname, 443, type=socket.SOCK_STREAM):
        ip = ipaddress.ip_address(info[4][0])
        if not ip.is_global:
            raise ValueError("Privatna, lokalna ili rezervirana mrežna adresa nije dopuštena.")
    return value.rstrip("/")


def fetch(opener, url: str, origin: str, accept: str) -> bytes:
    target = urlsplit(url)
    root = urlsplit(origin)
    if target.scheme != "https" or target.hostname != root.hostname or target.port != root.port:
        raise ValueError("Cjenik vodi na drugu domenu ili na nesiguran HTTP.")
    request = urllib.request.Request(url, headers={
        "User-Agent": "BAZNA-Legal-Publication-Probe/1.0 (+anonymous; no cookies)",
        "Accept": accept,
        "Cache-Control": "no-cache",
    })
    with opener.open(request, timeout=14) as response:
        if response.status != 200:
            raise ValueError(f"Javna datoteka ne vraća HTTP 200: {response.status}")
        body = response.read(MAX_BYTES + 1)
        if len(body) > MAX_BYTES:
            raise ValueError("Odgovor prelazi sigurnosnu granicu probnog dohvaćanja.")
        return body


def check_csv(data: bytes, kind: str) -> int:
    content = data.decode("utf-8-sig", errors="strict")
    if "\x00" in content:
        raise ValueError("CSV sadrži nedopušten NUL znak.")
    records = csv.reader(io.StringIO(content), delimiter=";", strict=True)
    head = next(records, [])
    if head != REQUIRED[kind]:
        raise ValueError(f"CSV stupci nisu u obveznom redoslijedu: {head}")
    rows = 0
    for item in records:
        if len(item) != len(head):
            raise ValueError("CSV redak nema točan broj stupaca.")
        rows += 1
    return rows


def inspect(site: str, kind: str, changed_on: str | None) -> dict:
    origin = verify_site(site)
    opener = urllib.request.build_opener(NoRedirect())
    index = origin + "/wp-json/brendigo-bazna/v1/cjenici"
    data = json.loads(fetch(opener, index, origin, "application/json").decode("utf-8"))
    if data.get("api_version") != 1:
        raise ValueError("Nepodržana verzija javnog BAZNA API-ja.")
    feeds = data.get("official_feeds", {}).get(kind, {})
    csv_url = feeds.get("csv") if feeds.get("available_csv") else None
    xml_url = feeds.get("xml") if feeds.get("available_xml") else None
    if not csv_url and not xml_url:
        raise ValueError("Nije pronađen javno objavljeni službeni cjenik tražene vrste.")
    result = {"site": origin, "kind": kind, "discovery": index, "checked": [], "warnings": []}
    if csv_url:
        payload = fetch(opener, csv_url, origin, "text/csv")
        result["checked"].append({"format": "csv", "bytes": len(payload),
                                   "rows": check_csv(payload, kind), "url": csv_url})
    if xml_url:
        from xml.etree import ElementTree
        payload = fetch(opener, xml_url, origin, "application/xml")
        root = ElementTree.fromstring(payload)
        if root.tag != "cjenik":
            raise ValueError("XML nema zakonski očekivan korijenski element.")
        tag = "roba" if kind == "proizvodi" else "usluga"
        for entry in root:
            if entry.tag != tag or [item.tag for item in entry] != REQUIRED[kind]:
                raise ValueError("XML nema očekivani tip retka ili redoslijed polja.")
        result["checked"].append({"format": "xml", "bytes": len(payload),
                                   "rows": len(root), "url": xml_url})
    if changed_on:
        # Published_at is generated feed metadata, NOT an independent POS event time.
        published = data.get("published_at")
        if not isinstance(published, str):
            raise ValueError("Nedostaje vrijeme zadnje objave za usporedbu s danom promjene.")
        event = datetime.fromisoformat(published.replace("Z", "+00:00"))
        stamp = event.astimezone(ZoneInfo("Europe/Zagreb"))
        if stamp.date().isoformat() != changed_on or (stamp.hour, stamp.minute) > (11, 0):
            raise ValueError("Timestamp objave nije unutar roka 11:00 na zadan dan promjene.")
        result["checked"].append({"publication_local": stamp.isoformat(),
                                   "changed_on": changed_on})
        result["warnings"].append("Vrijeme promjene mora se neovisno potvrditi iz POS revizijskog traga.")
    archive = data.get("archive")
    if isinstance(archive, str) and archive.startswith("https://"):
        try:
            html = fetch(opener, archive, origin, "text/html").decode("utf-8", errors="strict")
            parser = Links()
            parser.feed(html)
            candidates = [urljoin(archive, link) for link in parser.items
                          if re.search(r"_\\d{8}_\\d{4}\\.(?:csv|xml)(?:$|[?#])", link)]
            result["checked"].append({"archive_url": archive, "dated_snapshot_links": len(candidates)})
            if not candidates:
                result["warnings"].append("Javna arhivska stranica nema strojno provjerene službene vremenske nazive.")
        except (ValueError, UnicodeError, urllib.error.URLError):
            result["warnings"].append("Arhiva nije automatski provjerena; otvorite anonimno u drugom pregledniku.")
    result["warnings"].append("15 dana dostupnosti od zamjene može se dokazati samo kroz vrijeme; ovaj jednokratni test nije potvrda.")
    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("site", help="Kanonska HTTPS adresa vlastite BAZNA WordPress instalacije.")
    p.add_argument("--kind", choices=tuple(REQUIRED), required=True)
    p.add_argument("--changed-on", help="Datum stvarne izmjene prema POS-u, ISO YYYY-MM-DD")
    args = p.parse_args()
    try:
        if args.changed_on:
            datetime.strptime(args.changed_on, "%Y-%m-%d")
        result = inspect(args.site, args.kind, args.changed_on)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        print("PASS: javni tehnički dohvat (nije pravni certifikat)")
    except (OSError, ValueError, urllib.error.URLError, json.JSONDecodeError) as e:
        print("FAIL: javni tehnički dohvat:", e, file=sys.stderr)
        sys.exit(1)
