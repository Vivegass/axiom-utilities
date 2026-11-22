"""Draws the Axiom Utilities icon: a light-grey gear with an exploded isometric cube inside,
in the flat RGB style of Axiom's own editor gizmos.

Usage: python tools/make_icon.py <output.png> [size]
"""
import math
import sys

from PIL import Image, ImageDraw

GREY = (230, 230, 230, 255)
RED = (255, 0, 43, 255)
BLUE = (0, 170, 255, 255)
GREEN = (0, 255, 86, 255)

SS = 8  # supersampling factor


def gear(draw, cx, cy, r_outer, r_inner, r_hole, teeth, tooth_width, fill):
    pts = []
    steps = teeth * 4
    for i in range(steps):
        a0 = 2 * math.pi * i / steps - math.pi / 2
        # tooth on even quarter-steps, valley otherwise
        r = r_outer if (i % 4) in (0, 1) else r_inner
        a_off = tooth_width * (1 if (i % 4) in (1, 3) else -1) * 0.0
        pts.append((cx + r * math.cos(a0 + a_off), cy + r * math.sin(a0 + a_off)))
    draw.polygon(pts, fill=fill)
    draw.ellipse((cx - r_hole, cy - r_hole, cx + r_hole, cy + r_hole), fill=(0, 0, 0, 0))


def iso_cube(draw, cx, cy, s, gap):
    """Exploded isometric cube centred at (cx, cy) with edge length s; each face pushed out by gap."""
    dx = s * math.cos(math.radians(30))
    dy = s * 0.5
    # vertices of the hexagon around the centre point (centre = the near corner)
    top = [(cx, cy), (cx - dx, cy - dy), (cx, cy - 2 * dy), (cx + dx, cy - dy)]
    left = [(cx, cy), (cx - dx, cy - dy), (cx - dx, cy + dy), (cx, cy + 2 * dy)]
    right = [(cx, cy), (cx + dx, cy - dy), (cx + dx, cy + dy), (cx, cy + 2 * dy)]

    def shift(poly, vx, vy):
        return [(x + vx, y + vy) for x, y in poly]

    g = gap
    draw.polygon(shift(top, 0, -g), fill=GREEN)
    draw.polygon(shift(left, -g * math.cos(math.radians(30)), g * 0.5), fill=RED)
    draw.polygon(shift(right, g * math.cos(math.radians(30)), g * 0.5), fill=BLUE)


def make(size):
    n = size * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = n / 2

    # gear: 8 chunky teeth, like Axiom's chunky gizmo arrows
    teeth = 8
    r_out = n * 0.49
    r_in = n * 0.405
    pts = []
    for t in range(teeth):
        base = 2 * math.pi * t / teeth - math.pi / 2
        half_tooth = math.radians(11)
        half_gap = math.pi / teeth - half_tooth
        for a, r in (
            (base - half_tooth - half_gap * 0.15, r_in),
            (base - half_tooth * 0.78, r_out),
            (base + half_tooth * 0.78, r_out),
            (base + half_tooth + half_gap * 0.15, r_in),
        ):
            pts.append((c + r * math.cos(a), c + r * math.sin(a)))
    d.polygon(pts, fill=GREY, outline=(178, 178, 186, 255), width=max(1, int(n * 0.012)))
    hole = n * 0.33
    d.ellipse((c - hole, c - hole, c + hole, c + hole), fill=(0, 0, 0, 0))
    w = max(1, int(n * 0.012))
    d.ellipse((c - hole, c - hole, c + hole, c + hole), outline=(178, 178, 186, 255), width=w)

    # cube inside the ring; the near corner sits slightly below centre so the cube looks centred
    s = n * 0.225
    iso_cube(d, c, c + s * 0.02, s, gap=n * 0.022)
    # small white hub where the faces meet, echoing the gizmo centre in Axiom's icon
    hub = n * 0.032
    d.ellipse((c - hub, c - hub, c + hub, c + hub), fill=(255, 255, 255, 255))

    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    out = sys.argv[1]
    size = int(sys.argv[2]) if len(sys.argv) > 2 else 128
    make(size).save(out)
    print("wrote", out, size)
