import json
import math
import shutil
import struct
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

FILM = Path(__file__).resolve().parents[1]
APP = FILM.parent / "app" / "src" / "main"
PUBLIC = FILM / "public"
DATA = PUBLIC / "data"
FONTS = PUBLIC / "fonts"
QGRIDS = FILM / "tools" / "qgrids"
LOGO_SOURCE = Path.home() / "Downloads" / "WIENER Logo (2).png"

Q_LOW = -1.2
Q_HIGH = 0.8
Q_UPSAMPLE = 4

NAMED = [
    "Vega", "Altair", "Deneb", "Sadr", "Albireo", "Aldebaran", "Betelgeuse", "Rigel", "Sirius",
    "Procyon", "Polaris", "Arcturus", "Spica", "Antares", "Capella", "Fomalhaut", "Regulus",
    "Alkaid", "Mizar", "Dubhe", "Merak", "Algol", "Alnilam", "Alnitak", "Mintaka", "Bellatrix",
    "Castor", "Pollux", "Shaula", "Kochab", "Schedar", "Caph", "Enif", "Markab", "Alpheratz",
]


def copy_assets() -> None:
    for name in ["stars.bin", "constellations.bin", "milkyway.png", "moon.jpg"]:
        shutil.copyfile(APP / "assets" / name, DATA / name)
    for name in ["funnel_display.ttf", "funnel_sans.ttf", "amiri_regular.ttf"]:
        shutil.copyfile(APP / "res" / "font" / name, FONTS / name)


def white_logo() -> None:
    if not LOGO_SOURCE.exists():
        return
    rgba = np.asarray(Image.open(LOGO_SOURCE).convert("RGBA")).astype(np.float32)
    luminance = (0.2126 * rgba[..., 0] + 0.7152 * rgba[..., 1] + 0.0722 * rgba[..., 2]) / 255.0
    coverage = np.clip(((1.0 - luminance) * rgba[..., 3] / 255.0 - 0.04) / 0.92, 0.0, 1.0)
    ys, xs = np.nonzero(coverage > 0.02)
    crop = coverage[ys.min(): ys.max() + 1, xs.min(): xs.max() + 1]
    height, width = crop.shape
    pad = round(max(height, width) * 0.08)
    out = np.zeros((height + 2 * pad, width + 2 * pad, 4), dtype=np.uint8)
    out[..., :3] = 255
    out[pad: pad + height, pad: pad + width, 3] = np.round(crop * 255).astype(np.uint8)
    Image.fromarray(out, mode="RGBA").save(DATA / "wiener-logo-white.png", optimize=True)


def read_stars():
    raw = (APP / "assets" / "stars.bin").read_bytes()
    count = struct.unpack_from("<i", raw, 0)[0]
    records = [struct.unpack_from("<fffffi", raw, 4 + i * 24) for i in range(count)]
    meta = {}
    for line in (APP / "assets" / "stars_meta.tsv").read_text(encoding="utf-8").splitlines():
        fields = line.split("\t")
        if len(fields) >= 3 and fields[2]:
            meta[fields[2]] = int(fields[0])
    return records, meta


def named_stars() -> None:
    records, meta = read_stars()
    out = {}
    for name in NAMED:
        index = meta.get(name)
        if index is None:
            continue
        x, y, z, mag, bv, _ = records[index]
        out[name] = {"v": [round(x, 6), round(y, 6), round(z, 6)], "mag": round(mag, 2), "bv": round(bv, 2)}
    (DATA / "named.json").write_text(json.dumps(out), encoding="utf-8")
    print(f"named stars: {len(out)}")


def land_paths() -> None:
    raw = (APP / "assets" / "land.bin").read_bytes()
    count = struct.unpack_from("<i", raw, 0)[0]
    offset = 4
    width, height = 3600.0, 1800.0
    paths = []
    for _ in range(count):
        points = struct.unpack_from("<i", raw, offset)[0]
        offset += 4
        coords = struct.unpack_from(f"<{points * 2}f", raw, offset)
        offset += points * 8
        parts = []
        previous = None
        for i in range(points):
            lon, lat = coords[i * 2], coords[i * 2 + 1]
            x = (lon + 180.0) / 360.0 * width
            y = (90.0 - lat) / 180.0 * height
            command = "M" if previous is None or abs(lon - previous) > 180 else "L"
            parts.append(f"{command}{x:.1f} {y:.1f}")
            previous = lon
        paths.append("".join(parts) + "Z")
    (DATA / "land.json").write_text(json.dumps({"width": width, "height": height, "paths": paths}), encoding="utf-8")
    print(f"land paths: {len(paths)}")


def q_textures() -> None:
    lines = (QGRIDS / "grids.txt").read_text().strip().splitlines()
    manifest = []
    for line in lines:
        date, conjunction, columns, rows = line.split()
        columns, rows = int(columns), int(rows)
        values = np.frombuffer((QGRIDS / f"q-{date}.f32").read_bytes(), dtype="<f4").reshape(rows, columns)
        holes = ~np.isfinite(values)
        moon_first = np.isneginf(values)
        if holes.any():
            nearest = ndimage.distance_transform_edt(holes, return_distances=False, return_indices=True)
            filled = values[tuple(nearest)]
        else:
            filled = values
        shape = ((rows - 1) * Q_UPSAMPLE + 1, (columns - 1) * Q_UPSAMPLE + 1)
        smooth = ndimage.zoom(filled.astype(np.float64), (shape[0] / rows, shape[1] / columns), order=3, mode="nearest", grid_mode=False)
        mask = ndimage.zoom(moon_first.astype(np.float64), (shape[0] / rows, shape[1] / columns), order=1, mode="nearest", grid_mode=False)
        code = np.round(np.clip((smooth - Q_LOW) / (Q_HIGH - Q_LOW), 0.0, 1.0) * 65535).astype(np.uint32)
        rgba = np.zeros(shape + (4,), dtype=np.uint8)
        rgba[..., 0] = (code >> 8).astype(np.uint8)
        rgba[..., 1] = np.round(np.clip(mask, 0.0, 1.0) * 255).astype(np.uint8)
        rgba[..., 2] = (code & 255).astype(np.uint8)
        rgba[..., 3] = 255
        rgba = rgba[::-1]
        Image.fromarray(rgba, mode="RGBA").save(DATA / f"q-{date}.png", optimize=True)
        visible = float(np.mean(np.nan_to_num(values, nan=-9, neginf=-9) > -0.014))
        manifest.append({"date": date, "conjunction": conjunction, "file": f"q-{date}.png", "nakedEyeShare": round(visible, 4)})
    (DATA / "qgrids.json").write_text(json.dumps({"low": Q_LOW, "high": Q_HIGH, "latTop": 60, "latBottom": -60, "grids": manifest}), encoding="utf-8")
    print(json.dumps(manifest, indent=1))


def main() -> int:
    DATA.mkdir(parents=True, exist_ok=True)
    FONTS.mkdir(parents=True, exist_ok=True)
    copy_assets()
    white_logo()
    named_stars()
    land_paths()
    q_textures()
    return 0


if __name__ == "__main__":
    sys.exit(main())
