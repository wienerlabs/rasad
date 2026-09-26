import pathlib
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[1]
RAW = ROOT / "docs/play-store/raw"
OUT = ROOT / "docs/play-store/assets"
FONTS = ROOT / "app/src/main/res/font"

INK = (5, 7, 11)
TOP = (16, 20, 32)
TEXT = (244, 242, 236)

SCENES = ["1-sky", "2-constellation", "3-star", "4-hilal", "5-prayer", "6-dawn", "7-events", "8-dua"]

CAPTIONS = {
    "tr": [
        "Gökyüzü, tam olduğu yerde",
        "Aradığın takımyıldızına yol gösterir",
        "Yıldız adlarının Arapça kökeni",
        "Hilali nerede ve ne zaman arayacağını bil",
        "Namaz vakitleri, gökte gösterilir",
        "Sahte şafağı gerçek şafaktan ayırt et",
        "Kıble güneşi ve tutulmalar",
        "Hilali görünce okunan dua",
    ],
    "en": [
        "The sky, exactly where it is",
        "Guides you to any constellation",
        "The Arabic roots of star names",
        "Know where and when to look for the crescent",
        "Prayer times you can see in the sky",
        "Tell the false dawn from the true dawn",
        "Qibla sun days and eclipses",
        "The du'a for sighting the crescent",
    ],
}

TAGLINES = {
    "tr": "Gökyüzü, hilal, kıble ve namaz vakitleri",
    "en": "Sky, crescent, qibla and prayer times",
}

MAJOR_TICKS = [((54, 25.5), (54, 30)), ((78.5, 54), (83, 54)), ((54, 78), (54, 82.5)), ((25, 54), (29.5, 54))]
MINOR_TICKS = [
    ((54, 33.5), (54, 35.2)), ((74.5, 54), (72.8, 54)), ((54, 74.5), (54, 72.8)), ((33.5, 54), (35.2, 54)),
    ((68.5, 39.5), (67.3, 40.7)), ((68.5, 68.5), (67.3, 67.3)), ((39.5, 68.5), (40.7, 67.3)), ((39.5, 39.5), (40.7, 40.7)),
]
CRESCENT_OUTER = ((50.0, 54.0), 13.0)
CRESCENT_INNER = ((56.49, 50.51), 11.5)


def font(name, size, weight):
    face = ImageFont.truetype(str(FONTS / name), size)
    face.set_variation_by_name(weight)
    return face


def glyph(size, units=76.0, opacity=1.0):
    ss = 4
    side = size * ss
    k = side / units

    def point(x, y):
        return ((x - 54) * k + side / 2, (y - 54) * k + side / 2)

    def stroke(layer, a, b, width):
        draw = ImageDraw.Draw(layer)
        draw.line([point(*a), point(*b)], fill=255, width=round(width * k))
        r = width * k / 2
        for q in (a, b):
            cx, cy = point(*q)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)

    major = Image.new("L", (side, side), 0)
    draw = ImageDraw.Draw(major)
    cx, cy = point(54, 54)
    ring = 24 * k
    width = 2.2 * k
    draw.ellipse([cx - ring - width / 2, cy - ring - width / 2, cx + ring + width / 2, cy + ring + width / 2], outline=255, width=round(width))
    for a, b in MAJOR_TICKS:
        stroke(major, a, b, 2.2)
    (ox, oy), orad = CRESCENT_OUTER
    (ix, iy), irad = CRESCENT_INNER
    crescent = Image.new("L", (side, side), 0)
    cdraw = ImageDraw.Draw(crescent)
    px, py = point(ox, oy)
    cdraw.ellipse([px - orad * k, py - orad * k, px + orad * k, py + orad * k], fill=255)
    qx, qy = point(ix, iy)
    cdraw.ellipse([qx - irad * k, qy - irad * k, qx + irad * k, qy + irad * k], fill=0)
    major.paste(255, mask=crescent)

    minor = Image.new("L", (side, side), 0)
    for a, b in MINOR_TICKS:
        stroke(minor, a, b, 1.4)
    minor = minor.point(lambda v: round(v * 0.6))

    alpha = Image.new("L", (side, side), 0)
    alpha.paste(minor, mask=minor)
    alpha.paste(major, mask=major)
    alpha = alpha.point(lambda v: round(v * opacity))
    layer = Image.new("RGBA", (side, side), TEXT + (0,))
    layer.putalpha(alpha)
    return layer.resize((size, size), Image.LANCZOS)


def gradient(width, height, top, bottom):
    image = Image.new("RGB", (width, height), bottom)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / (height - 1)
        draw.line([(0, y), (width, y)], fill=tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return image


def star_field(width, height, count, seed):
    rng = random.Random(seed)
    ss = 2
    field = Image.new("RGBA", (width * ss, height * ss), (0, 0, 0, 0))
    draw = ImageDraw.Draw(field)
    for _ in range(count):
        x = rng.uniform(0, width * ss)
        y = rng.uniform(0, height * ss)
        u = rng.random()
        brightness = 0.25 + 0.75 * u ** 4
        radius = (0.55 + 1.7 * u ** 6) * ss
        tint = rng.choice([(255, 250, 240), (235, 240, 255), (255, 236, 214), (244, 242, 236)])
        draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill=tint + (round(255 * brightness),))
    return field.resize((width, height), Image.LANCZOS)


def milky_way(width, height, seed):
    rng = random.Random(seed)
    band = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(band)
    for _ in range(260):
        t = rng.random()
        x = width * (0.30 + 0.75 * t) + rng.gauss(0, 34)
        y = height * (1.05 - 1.2 * t) + rng.gauss(0, 34)
        r = rng.uniform(18, 60)
        draw.ellipse([x - r, y - r, x + r, y + r], fill=round(rng.uniform(4, 14)))
    return band.filter(ImageFilter.GaussianBlur(28))


def write_text(draw, position, text, face, color, opacity=1.0):
    draw.text(position, text, font=face, fill=color + (round(255 * opacity),))


def wrap(text, face, max_width):
    words = text.split(" ")
    lines = [""]
    for word in words:
        trial = (lines[-1] + " " + word).strip()
        if face.getlength(trial) <= max_width:
            lines[-1] = trial
        else:
            lines.append(word)
    return lines


def feature_graphic(lang):
    width, height = 1024, 500
    base = gradient(width, height, (12, 16, 28), INK).convert("RGBA")
    glow = milky_way(width, height, 7)
    base.alpha_composite(Image.merge("RGBA", (glow, glow, glow, glow)))
    base.alpha_composite(star_field(width, height, 620, 2026))
    shade = Image.new("L", (width, height), 0)
    sdraw = ImageDraw.Draw(shade)
    for x in range(width):
        t = min(1.0, x / (width * 0.62))
        sdraw.line([(x, 0), (x, height)], fill=round(215 * (1 - t) ** 1.6))
    base.alpha_composite(Image.merge("RGBA", (Image.new("L", (width, height), INK[0]), Image.new("L", (width, height), INK[1]), Image.new("L", (width, height), INK[2]), shade)))
    mark = glyph(340, units=70.0, opacity=0.92)
    base.alpha_composite(mark, (width - 340 - 48, (height - 340) // 2))
    draw = ImageDraw.Draw(base)
    write_text(draw, (72, 128), "Rasad", font("funnel_display.ttf", 132, "SemiBold"), TEXT)
    write_text(draw, (78, 292), TAGLINES[lang], font("funnel_sans.ttf", 30, "Regular"), TEXT, 0.8)
    write_text(draw, (78, 392), "Wiener Labs", font("funnel_sans.ttf", 22, "Medium"), TEXT, 0.5)
    return base.convert("RGB")


def screenshot(raw, caption):
    width, height = 1080, 1920
    canvas = gradient(width, height, TOP, INK).convert("RGBA")
    draw = ImageDraw.Draw(canvas)
    face = font("funnel_display.ttf", 70, "Medium")
    lines = wrap(caption, face, 920)
    y = 118 if len(lines) > 1 else 160
    for line in lines:
        draw.text(((width - face.getlength(line)) / 2, y), line, font=face, fill=TEXT + (255,))
        y += 88
    shot = Image.open(raw).convert("RGB").crop((0, 110, 1080, 2345))
    target_height = 1500
    target_width = round(shot.width * target_height / shot.height)
    shot = shot.resize((target_width, target_height), Image.LANCZOS)
    x = (width - target_width) // 2
    top = height - target_height - 70
    mask = Image.new("L", (target_width, target_height), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, target_width - 1, target_height - 1], radius=42, fill=255)
    canvas.paste(shot, (x, top), mask)
    border = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle([x - 1, top - 1, x + target_width, top + target_height], radius=43, outline=TEXT + (60,), width=3)
    canvas.alpha_composite(border)
    return canvas.convert("RGB")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon = Image.new("RGBA", (512, 512), INK + (255,))
    icon.alpha_composite(glyph(512))
    icon.convert("RGB").save(OUT / "icon-512.png", optimize=True)
    for lang in ("tr", "en"):
        feature_graphic(lang).save(OUT / f"feature-graphic-{lang}.png", optimize=True)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for index, scene in enumerate(SCENES):
            screenshot(RAW / f"{scene}.png", CAPTIONS[lang][index]).save(folder / f"{scene}.png", optimize=True)
    print(sorted(str(p.relative_to(ROOT)) for p in OUT.rglob("*.png")))


if __name__ == "__main__":
    main()
