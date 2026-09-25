import csv
import json
import math
import os
import shutil
import struct
import sys
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
FONTS = ROOT / "app" / "src" / "main" / "res" / "font"
CACHE = Path(os.environ.get("RASAD_DATA_CACHE", ROOT / "tools" / ".cache"))

SOURCES = {
    "hyg.csv": "https://raw.githubusercontent.com/astronexus/HYG-Database/main/hyg/CURRENT/hygdata_v41.csv",
    "lines.json": "https://raw.githubusercontent.com/ofrohn/d3-celestial/master/data/constellations.lines.json",
    "constellations.json": "https://raw.githubusercontent.com/ofrohn/d3-celestial/master/data/constellations.json",
    "mw.json": "https://raw.githubusercontent.com/ofrohn/d3-celestial/master/data/mw.json",
    "land.geojson": "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_land.geojson",
    "moon.jpg": "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_1k.jpg",
    "FunnelDisplay.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/funneldisplay/FunnelDisplay%5Bwght%5D.ttf",
    "FunnelSans.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/funnelsans/FunnelSans%5Bwght%5D.ttf",
    "Amiri-Regular.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf",
}

GREEK = {
    "Alp": "α", "Bet": "β", "Gam": "γ", "Del": "δ", "Eps": "ε", "Zet": "ζ", "Eta": "η",
    "The": "θ", "Iot": "ι", "Kap": "κ", "Lam": "λ", "Mu": "μ", "Nu": "ν", "Xi": "ξ",
    "Omi": "ο", "Pi": "π", "Rho": "ρ", "Sig": "σ", "Tau": "τ", "Ups": "υ", "Phi": "φ",
    "Chi": "χ", "Psi": "ψ", "Ome": "ω",
}
SUPERSCRIPT = str.maketrans("0123456789", "⁰¹²³⁴⁵⁶⁷⁸⁹")

STAR_LIMIT_MAG = 6.5
MILKY_WAY_SIZE = (2048, 1024)


def fetch(name: str) -> Path:
    CACHE.mkdir(parents=True, exist_ok=True)
    target = CACHE / name
    if target.exists() and target.stat().st_size > 0:
        return target
    request = urllib.request.Request(SOURCES[name], headers={"User-Agent": "rasad-data-builder"})
    with urllib.request.urlopen(request, timeout=120) as response, open(target, "wb") as out:
        shutil.copyfileobj(response, out)
    print(f"fetched {name} ({target.stat().st_size} bytes)")
    return target


def greek_designation(bayer: str) -> str:
    if not bayer:
        return ""
    head = bayer.rstrip("0123456789-")
    tail = bayer[len(head):].replace("-", "")
    letter = GREEK.get(head, head)
    return letter + tail.translate(SUPERSCRIPT)


def unit_vector(ra_deg: float, dec_deg: float) -> tuple[float, float, float]:
    ra = math.radians(ra_deg)
    dec = math.radians(dec_deg)
    return math.cos(dec) * math.cos(ra), math.cos(dec) * math.sin(ra), math.sin(dec)


def build_stars() -> None:
    rows = []
    with open(fetch("hyg.csv"), newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["id"] == "0" or not row["mag"]:
                continue
            mag = float(row["mag"])
            if mag > STAR_LIMIT_MAG:
                continue
            rows.append(row)
    rows.sort(key=lambda r: float(r["mag"]))

    with open(ASSETS / "stars.bin", "wb") as binary:
        binary.write(struct.pack("<i", len(rows)))
        for row in rows:
            x, y, z = unit_vector(float(row["ra"]) * 15.0, float(row["dec"]))
            color_index = float(row["ci"]) if row["ci"] else 0.6
            hip = int(row["hip"]) if row["hip"] else 0
            binary.write(struct.pack("<fffffi", x, y, z, float(row["mag"]), color_index, hip))

    with open(ASSETS / "stars_meta.tsv", "w", encoding="utf-8") as meta:
        for index, row in enumerate(rows):
            distance = float(row["dist"]) if row["dist"] else 0.0
            if distance >= 100000:
                distance = 0.0
            fields = [
                str(index),
                row["hip"] or "0",
                row["proper"].strip(),
                greek_designation(row["bayer"].strip()),
                row["flam"].strip(),
                row["con"].strip(),
                f"{distance:.3f}",
                row["spect"].strip(),
            ]
            meta.write("\t".join(fields) + "\n")
    named = sum(1 for r in rows if r["proper"].strip())
    print(f"stars: {len(rows)} (named {named})")


def ra_from_lon(lon: float) -> float:
    return lon + 360.0 if lon < 0 else lon


def build_constellations() -> None:
    lines = json.loads(fetch("lines.json").read_text(encoding="utf-8"))
    meta = json.loads(fetch("constellations.json").read_text(encoding="utf-8"))
    labels = {}
    for feature in meta["features"]:
        lon, lat = feature["geometry"]["coordinates"]
        labels[feature["id"]] = (ra_from_lon(lon), lat, int(feature["properties"].get("rank", 3)))

    records = []
    for feature in lines["features"]:
        segments = []
        for polyline in feature["geometry"]["coordinates"]:
            for (lon1, lat1), (lon2, lat2) in zip(polyline, polyline[1:]):
                segments.append((ra_from_lon(lon1), lat1, ra_from_lon(lon2), lat2))
        code = feature["id"]
        label = labels.get(code)
        if label is None:
            continue
        records.append((code, label, segments))

    with open(ASSETS / "constellations.bin", "wb") as binary:
        binary.write(struct.pack("<i", len(records)))
        for code, (ra, dec, rank), segments in records:
            binary.write(code.encode("ascii").ljust(4, b" "))
            binary.write(struct.pack("<ffii", ra, dec, rank, len(segments)))
            for segment in segments:
                binary.write(struct.pack("<ffff", *segment))
    print(f"constellations: {len(records)} ({sum(len(r[2]) for r in records)} segments)")


def unwrap_ring(ring: list[list[float]]) -> list[tuple[float, float]]:
    result = []
    previous = None
    offset = 0.0
    for lon, lat in ring:
        if previous is not None:
            delta = lon - previous
            if delta > 180:
                offset -= 360
            elif delta < -180:
                offset += 360
        previous = lon
        result.append((lon + offset, lat))
    return result


def rasterize_milky_way() -> None:
    features = json.loads(fetch("mw.json").read_text(encoding="utf-8"))["features"]
    scale = 2
    width, height = MILKY_WAY_SIZE[0] * scale, MILKY_WAY_SIZE[1] * scale
    accumulator = np.zeros((height, width), dtype=np.float32)

    def to_pixel(ra: float, dec: float) -> tuple[float, float]:
        return ra / 360.0 * width, (90.0 - dec) / 180.0 * height

    for feature in features:
        level = np.zeros((height, width), dtype=bool)
        geometry = feature["geometry"]
        polygons = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
        for polygon in polygons:
            for ring in polygon:
                unwrapped = unwrap_ring(ring)
                if len(unwrapped) < 3:
                    continue
                if abs(unwrapped[-1][0] - unwrapped[0][0]) > 180:
                    unwrapped = unwrapped + [(unwrapped[-1][0], 90.0), (unwrapped[0][0], 90.0)]
                ring_layer = Image.new("1", (width, height), 0)
                draw = ImageDraw.Draw(ring_layer)
                for shift in (-360.0, 0.0, 360.0):
                    draw.polygon([to_pixel(lon + shift, lat) for lon, lat in unwrapped], fill=1)
                level ^= np.asarray(ring_layer, dtype=bool)
        accumulator += level.astype(np.float32)

    accumulator /= max(len(features), 1)
    image = Image.fromarray(np.clip(accumulator * 255.0, 0, 255).astype(np.uint8), mode="L")
    image = image.resize(MILKY_WAY_SIZE, Image.LANCZOS)
    image = image.filter(ImageFilter.GaussianBlur(radius=3.2))
    values = np.asarray(image, dtype=np.float32) / 255.0
    values = np.power(values, 0.85)
    Image.fromarray(np.clip(values * 255.0, 0, 255).astype(np.uint8), mode="L").save(ASSETS / "milkyway.png", optimize=True)
    print(f"milky way: {len(features)} contour levels -> milkyway.png")


def build_land() -> None:
    features = json.loads(fetch("land.geojson").read_text(encoding="utf-8"))["features"]
    rings = []
    for feature in features:
        geometry = feature["geometry"]
        polygons = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
        for polygon in polygons:
            rings.append(polygon[0])
    with open(ASSETS / "land.bin", "wb") as binary:
        binary.write(struct.pack("<i", len(rings)))
        for ring in rings:
            binary.write(struct.pack("<i", len(ring)))
            for lon, lat in ring:
                binary.write(struct.pack("<ff", lon, lat))
    print(f"land: {len(rings)} rings, {sum(len(r) for r in rings)} points")


def build_moon() -> None:
    image = Image.open(fetch("moon.jpg")).convert("RGB")
    if image.size != (1024, 512):
        image = image.resize((1024, 512), Image.LANCZOS)
    image.save(ASSETS / "moon.jpg", quality=88, optimize=True)
    print(f"moon texture: {image.size}")


def copy_fonts() -> None:
    FONTS.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(fetch("FunnelDisplay.ttf"), FONTS / "funnel_display.ttf")
    shutil.copyfile(fetch("FunnelSans.ttf"), FONTS / "funnel_sans.ttf")
    shutil.copyfile(fetch("Amiri-Regular.ttf"), FONTS / "amiri_regular.ttf")
    print("fonts copied")


def main() -> int:
    ASSETS.mkdir(parents=True, exist_ok=True)
    build_stars()
    build_constellations()
    rasterize_milky_way()
    build_land()
    build_moon()
    copy_fonts()
    return 0


if __name__ == "__main__":
    sys.exit(main())
