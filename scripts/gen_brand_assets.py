#!/usr/bin/env python3
"""Generate all Bing-Bong brand assets (launcher icon, adaptive layers, TV banner,
Play Store artwork) from a single source-of-truth glyph.

Usage:  python3 scripts/gen_brand_assets.py

Requires `rsvg-convert` on PATH (macOS: `brew install librsvg`;
Debian/Ubuntu: `apt install librsvg2-bin`). Everything else is stdlib.

The mark: a flat amber desk bell ("bing-bong" — you tap it, the quick bars
appear) with two chime arcs, on a near-black tile. The amber matches the
overlay's light/switch domain accent (ui/QuickBar/foundation/TileAccents.kt).
All geometry is original and defined below; tweak GLYPH/palette and re-run.
"""

import os
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
PLAY = os.path.join(ROOT, "assets", "play")

# ---------------------------------------------------------------- palette ---
BG = "#141821"        # near-black tile (slight blue cast, like the overlay surface)
ACCENT = "#FFB74D"    # amber — light/switch domain accent from TileAccents.kt
ACCENT_SOFT = "#FFD9A0"  # lighter amber: bell base + chime arcs
TEXT = "#F5F6F8"
SUBTEXT = "#9AA3AF"

# ------------------------------------------------------ glyph (96x96 box) ---
# A desk bell: dome (semicircle), button knob on top, base plate below,
# flanked by two "bing-bong" chime arcs. Visual centre ~ (48, 41).
GCX, GCY = 48.0, 41.0

# (pathData, fill, stroke, strokeWidth) — stroke paths use round caps.
GLYPH = [
    # dome: semicircle, flat bottom at y=57, r=26 centred on (48, 57)
    ("M22 57 A26 26 0 0 1 74 57 Z", ACCENT, None, 0),
    # button stem + knob on top of the dome
    ("M45 27 h6 v8 h-6 Z", ACCENT, None, 0),
    ("M48 20 m-6 0 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0", ACCENT, None, 0),
    # base plate (slightly wider than the dome, lighter amber)
    ("M17 62 h62 a4.5 4.5 0 0 1 0 9 h-62 a4.5 4.5 0 0 1 0 -9 Z",
     ACCENT_SOFT, None, 0),
    # chime arcs, top-left and top-right of the knob
    ("M30 8 A34 34 0 0 0 14 25", None, ACCENT_SOFT, 6),
    ("M66 8 A34 34 0 0 1 82 25", None, ACCENT_SOFT, 6),
]


def glyph_svg(scale, cx, cy):
    """SVG group placing the glyph centred on (cx, cy) at `scale`."""
    tx, ty = cx - GCX * scale, cy - GCY * scale
    parts = [f'<g transform="translate({tx:.2f} {ty:.2f}) scale({scale})">']
    for d, fill, stroke, sw in GLYPH:
        if fill:
            parts.append(f'<path d="{d}" fill="{fill}"/>')
        else:
            parts.append(f'<path d="{d}" fill="none" stroke="{stroke}" '
                         f'stroke-width="{sw}" stroke-linecap="round"/>')
    parts.append("</g>")
    return "".join(parts)


def glyph_vector_paths(indent="    "):
    """The same glyph as Android <path> elements (24dp-agnostic, 96-unit box)."""
    out = []
    for d, fill, stroke, sw in GLYPH:
        if fill:
            out.append(f'{indent}<path\n{indent}    android:pathData="{d}"\n'
                       f'{indent}    android:fillColor="{fill}"/>\n')
        else:
            out.append(f'{indent}<path\n{indent}    android:pathData="{d}"\n'
                       f'{indent}    android:fillColor="#00000000"\n'
                       f'{indent}    android:strokeColor="{stroke}"\n'
                       f'{indent}    android:strokeWidth="{sw}"\n'
                       f'{indent}    android:strokeLineCap="round"/>\n')
    return "".join(out)


def svg(w, h, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
            f'viewBox="0 0 {w} {h}">{body}</svg>')


# ------------------------------------------------------------- templates ----
# Full launcher icon (legacy square, also the Play 512 hi-res icon)
ICON_FULL = svg(108, 108,
                f'<rect width="108" height="108" rx="24" fill="{BG}"/>'
                + glyph_svg(1.0, 54, 55))

# Round legacy icon
ICON_ROUND = svg(108, 108,
                 f'<circle cx="54" cy="54" r="54" fill="{BG}"/>'
                 + glyph_svg(0.86, 54, 55))

# Adaptive-icon foreground layer (transparent ground; glyph sized so its
# corners stay inside the 66dp safe-zone circle under any launcher mask)
FG_SCALE = 0.72
ICON_FOREGROUND = svg(108, 108, glyph_svg(FG_SCALE, 54, 55))


def banner(w, h):
    """TV banner / store banner: mark in an accent ring + wordmark, 16:9."""
    s = h / 180.0  # design in 320x180 units, scale up
    ccx, ccy, r = 70 * s, 90 * s, 40 * s
    return svg(w, h,
               f'<rect width="{w}" height="{h}" fill="{BG}"/>'
               f'<circle cx="{ccx}" cy="{ccy}" r="{r}" '
               f'fill="{ACCENT}" fill-opacity="0.12"/>'
               f'<circle cx="{ccx}" cy="{ccy}" r="{r}" fill="none" '
               f'stroke="{ACCENT}" stroke-opacity="0.35" stroke-width="{1.5 * s}"/>'
               + glyph_svg(0.56 * s, ccx, ccy)
               + f'<text x="{124 * s}" y="{96 * s}" font-family="Helvetica Neue, '
                 f'Helvetica, Arial, sans-serif" font-weight="bold" '
                 f'font-size="{31 * s}" fill="{TEXT}">Bing-Bong</text>'
                 f'<text x="{125 * s}" y="{121 * s}" font-family="Helvetica Neue, '
                 f'Helvetica, Arial, sans-serif" font-size="{13 * s}" '
                 f'fill="{SUBTEXT}">for Home Assistant</text>')


def feature_graphic(w=1024, h=500):
    return svg(w, h,
               f'<rect width="{w}" height="{h}" fill="{BG}"/>'
               f'<circle cx="512" cy="185" r="108" fill="{ACCENT}" fill-opacity="0.12"/>'
               f'<circle cx="512" cy="185" r="108" fill="none" stroke="{ACCENT}" '
               f'stroke-opacity="0.35" stroke-width="4"/>'
               + glyph_svg(1.55, 512, 185)
               + f'<text x="512" y="372" text-anchor="middle" font-family="Helvetica '
                 f'Neue, Helvetica, Arial, sans-serif" font-weight="bold" '
                 f'font-size="72" fill="{TEXT}">Bing-Bong</text>'
                 f'<text x="512" y="428" text-anchor="middle" font-family="Helvetica '
                 f'Neue, Helvetica, Arial, sans-serif" font-size="30" '
                 f'fill="{SUBTEXT}">Home Assistant quick bars for Android TV</text>')


# ------------------------------------------------- Android vector drawables --
def vector_drawable(viewport, scale, cx, cy, dp):
    tx, ty = cx - GCX * scale, cy - GCY * scale
    return (f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{dp}dp"\n    android:height="{dp}dp"\n'
            f'    android:viewportWidth="{viewport}"\n'
            f'    android:viewportHeight="{viewport}">\n'
            f'  <group android:scaleX="{scale}" android:scaleY="{scale}"\n'
            f'      android:translateX="{tx:.2f}" android:translateY="{ty:.2f}">\n'
            f'{glyph_vector_paths()}  </group>\n</vector>\n')


# Adaptive-icon foreground (matches ICON_FOREGROUND raster)
VD_FOREGROUND = vector_drawable(108, FG_SCALE, 54, 55, dp=108)

# In-app brand mark / splash / notification icon: glyph on transparent
VD_ICON_SVG = vector_drawable(512, 4.6, 256, 256, dp=512)

BG_COLOR_XML = ('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
                f'    <color name="ic_icon_background">{BG}</color>\n'
                '</resources>\n')


# --------------------------------------------------------------- rendering --
def render(svg_text, out_png, w, h):
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as f:
        f.write(svg_text)
        tmp = f.name
    try:
        subprocess.run(["rsvg-convert", "-w", str(w), "-h", str(h),
                        "-o", out_png, tmp], check=True)
    finally:
        os.unlink(tmp)
    print(f"  {os.path.relpath(out_png, ROOT)}  ({w}x{h})")


def main():
    densities = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0,
                 "xxhdpi": 3.0, "xxxhdpi": 4.0}

    print("Launcher icons (legacy + adaptive raster layers):")
    for name, mult in densities.items():
        d = os.path.join(RES, f"mipmap-{name}")
        icon = int(48 * mult)      # 48dp legacy icon
        layer = int(108 * mult)    # 108dp adaptive layer
        render(ICON_FULL, os.path.join(d, "ic_icon.png"), icon, icon)
        render(ICON_ROUND, os.path.join(d, "ic_icon_round.png"), icon, icon)
        render(ICON_FOREGROUND, os.path.join(d, "ic_icon_foreground.png"),
               layer, layer)
        render(svg(108, 108, f'<rect width="108" height="108" fill="{BG}"/>'),
               os.path.join(d, "ic_icon_background.png"), layer, layer)

    print("TV banners (160x90dp `android:banner` resource):")
    for name, w, h in (("xhdpi", 320, 180), ("xxhdpi", 480, 270),
                       ("xxxhdpi", 640, 360)):
        render(banner(w, h), os.path.join(RES, f"mipmap-{name}", "ic_banner.png"),
               w, h)

    print("Play Store listing assets (assets/play/):")
    render(ICON_FULL, os.path.join(PLAY, "icon-512.png"), 512, 512)
    render(banner(1280, 720), os.path.join(PLAY, "tv-banner-1280x720.png"),
           1280, 720)
    render(feature_graphic(), os.path.join(PLAY, "feature-graphic-1024x500.png"),
           1024, 500)

    print("Vector drawables + background color:")
    for rel, content in (
            (os.path.join("drawable", "ic_icon_foreground.xml"), VD_FOREGROUND),
            (os.path.join("drawable", "icon_svg.xml"), VD_ICON_SVG),
            (os.path.join("values", "ic_icon_background.xml"), BG_COLOR_XML)):
        path = os.path.join(RES, rel)
        with open(path, "w") as f:
            f.write(content)
        print(f"  {os.path.relpath(path, ROOT)}")


if __name__ == "__main__":
    if subprocess.run(["which", "rsvg-convert"],
                      capture_output=True).returncode != 0:
        sys.exit("rsvg-convert not found — brew install librsvg")
    main()
