#!/usr/bin/env python3
"""Four-legged toy dogs. Body is horizontal, head is at the front, four paws on the ground."""

import json
import math
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "dogs")

WHITE = (0.97, 0.95, 0.92)
DARK = (0.14, 0.11, 0.09)
PINK = (0.93, 0.55, 0.60)
TAN = (0.90, 0.70, 0.48)
TAN_DEEP = (0.72, 0.48, 0.30)
CREAM = (0.96, 0.86, 0.68)
NAVY = (0.16, 0.24, 0.40)
GREY = (0.62, 0.60, 0.58)
RED = (0.80, 0.22, 0.28)
GOLD = (0.93, 0.74, 0.28)
TONGUE = (0.86, 0.45, 0.50)


def ellipsoid(rx, ry, rz, stacks=8, slices=12):
    verts, norms, idx = [], [], []
    for i in range(stacks + 1):
        phi = i / stacks * math.pi
        for j in range(slices + 1):
            theta = j / slices * math.tau
            nx = math.sin(phi) * math.cos(theta)
            ny = math.cos(phi)
            nz = math.sin(phi) * math.sin(theta)
            verts.append((nx * rx, ny * ry, nz * rz))
            nnx, nny, nnz = nx / max(rx, 1e-4), ny / max(ry, 1e-4), nz / max(rz, 1e-4)
            length = math.sqrt(nnx * nnx + nny * nny + nnz * nnz) or 1
            norms.append((nnx / length, nny / length, nnz / length))
    for i in range(stacks):
        for j in range(slices):
            a = i * (slices + 1) + j
            b = a + slices + 1
            idx += [a, b, a + 1, b, b + 1, a + 1]
    return verts, norms, idx


def cone(height, radius, slices=10):
    verts = [(0.0, height, 0.0)]
    norms = [(0.0, 1.0, 0.0)]
    for i in range(slices):
        a = i / slices * math.tau
        verts.append((radius * math.cos(a), 0.0, radius * math.sin(a)))
        nx, nz = math.cos(a), math.sin(a)
        ny = radius / max(height, 1e-4)
        length = math.sqrt(nx * nx + ny * ny + nz * nz)
        norms.append((nx / length, ny / length, nz / length))
    base = len(verts)
    verts.append((0.0, 0.0, 0.0))
    norms.append((0.0, -1.0, 0.0))
    idx = []
    for i in range(slices):
        j = 1 + i
        k = 1 + (i + 1) % slices
        idx += [0, k, j, base, j, k]
    return verts, norms, idx


def cylinder(height, radius, slices=10):
    verts, norms, idx = [], [], []
    for y, ny in ((height / 2, 1.0), (-height / 2, -1.0)):
        center = len(verts)
        verts.append((0.0, y, 0.0))
        norms.append((0.0, ny, 0.0))
        ring = len(verts)
        for i in range(slices):
            a = i / slices * math.tau
            verts.append((radius * math.cos(a), y, radius * math.sin(a)))
            norms.append((0.0, ny, 0.0))
        for i in range(slices):
            j = ring + i
            k = ring + (i + 1) % slices
            if ny > 0:
                idx += [center, k, j]
            else:
                idx += [center, j, k]
    side = len(verts)
    for i in range(slices + 1):
        a = i / slices * math.tau
        c, s = math.cos(a), math.sin(a)
        for y in (height / 2, -height / 2):
            verts.append((radius * c, y, radius * s))
            norms.append((c, 0.0, s))
    for i in range(slices):
        a = side + i * 2
        b = a + 2
        idx += [a, a + 1, b, a + 1, b + 1, b]
    return verts, norms, idx


class Dog:
    def __init__(self, name):
        self.name = name
        self.parts = []

    def add(self, name, parent, mesh, color, spec=0.32, t=(0, 0, 0), r=(0, 0, 0), s=(1, 1, 1)):
        self.parts.append({
            "name": name,
            "parent": parent,
            "t": [round(v, 4) for v in t],
            "r": [round(v, 4) for v in r],
            "s": [round(v, 4) for v in s],
            "color": [round(v, 4) for v in color],
            "spec": spec,
            "positions": [round(n, 4) for v in mesh[0] for n in v],
            "normals": [round(n, 4) for v in mesh[1] for n in v],
            "indices": mesh[2],
        })

    def eyes(self, y, z, sep, scale=0.07):
        eye = ellipsoid(scale, scale * 1.15, scale * 0.55, 6, 8)
        pupil = ellipsoid(scale * 0.46, scale * 0.52, scale * 0.3, 5, 8)
        spark = ellipsoid(scale * 0.16, scale * 0.16, scale * 0.1, 4, 6)
        for side, x in (("L", -sep), ("R", sep)):
            self.add(f"eye{side}", "head", eye, WHITE, 0.9, (x, y, z))
            self.add(f"pupil{side}", "head", pupil, DARK, 0.95, (x, y - scale * 0.06, z + scale * 0.38))
            self.add(f"spark{side}", "head", spark, WHITE, 1.0, (x - scale * 0.2, y + scale * 0.28, z + scale * 0.55))

    def nose(self, y, z, s=0.055):
        self.add("nose", "muzzle", ellipsoid(s * 1.15, s * 0.75, s * 0.7, 6, 8), DARK, 0.75, (0, y, z))

    def dump(self):
        names = [p["name"] for p in self.parts]
        out = []
        for part in self.parts:
            parent = part["parent"]
            item = dict(part)
            item["parent"] = -1 if parent in (-1, None) else names.index(parent)
            out.append(item)
        path = os.path.join(OUT, self.name + ".json")
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"id": self.name, "parts": out}, handle, separators=(",", ":"))
        print(self.name, len(out), "parts", sum(len(p["indices"]) // 3 for p in out), "tris")


def build(
    name,
    coat,
    ear,
    muzzle,
    body=(0.32, 0.24, 0.50),
    head=(0.26, 0.22, 0.22),
    snout=(0.13, 0.09, 0.13),
    leg_h=0.36,
    leg_r=0.055,
    ear_kind="drop",
    ear_size=(0.09, 0.16, 0.05),
    tail_color=None,
    belly=None,
    head_color=None,
    eye=0.062,
    eye_sep=0.10,
    skip_mouth=False,
    mask=None,
):
    dog = Dog(name)
    bx, by, bz = body
    hip_y = -by * 0.55
    center_y = hip_y - leg_h * 0.42
    body_y = 0.05 - center_y + leg_h / 2
    dog.add("body", None, ellipsoid(bx, by, bz, 10, 14), coat, 0.34, t=(0, body_y, 0))
    dog.add(
        "belly",
        "body",
        ellipsoid(bx * 0.62, by * 0.42, bz * 0.46, 6, 10),
        belly or lighten(coat),
        0.2,
        t=(0, -by * 0.35, bz * 0.08),
    )
    # Hips tuck into the belly. Cylinder is centered, so its center sits half a leg down.
    stance = (
        ("FL", -bx * 0.55, bz * 0.42, 10, 8),
        ("FR", bx * 0.55, bz * 0.42, -10, 8),
        ("BL", -bx * 0.55, -bz * 0.42, 8, -6),
        ("BR", bx * 0.55, -bz * 0.42, -8, -6),
    )
    leg_mesh = cylinder(leg_h, leg_r, 8)
    paw_mesh = ellipsoid(leg_r * 1.45, 0.04, leg_r * 1.7, 5, 8)
    for tag, x, z, rz, rx in stance:
        dog.add(f"leg{tag}", "body", leg_mesh, coat, 0.22, t=(x, center_y, z), r=(rx, 0, rz))
        dog.add(f"paw{tag}", f"leg{tag}", paw_mesh, coat, 0.2, t=(0, -leg_h * 0.5, 0.02))
    hx, hy, hz = head
    dog.add("neck", "body", ellipsoid(bx * 0.42, by * 0.55, bz * 0.22, 6, 8), coat, 0.2, t=(0, by * 0.15, bz * 0.72))
    dog.add("head", "body", ellipsoid(hx, hy, hz, 8, 12), head_color or coat, 0.36, t=(0, by * 0.55, bz * 0.92))
    sx, sy, sz = snout
    dog.add("muzzle", "head", ellipsoid(sx, sy, sz, 6, 10), muzzle, 0.28, t=(0, -hy * 0.28, hz * 0.85))
    dog.nose(-sy * 0.05, sz * 0.85, s=max(0.045, sx * 0.38))
    if mask:
        dog.add("mask", "head", ellipsoid(hx * 0.72, hy * 0.5, hz * 0.42, 6, 8), mask, t=(0, -hy * 0.2, hz * 0.45))
    if not skip_mouth:
        dog.add("mouth", "muzzle", ellipsoid(sx * 0.55, 0.025, 0.03, 4, 6), (0.45, 0.24, 0.24), t=(0, -sy * 0.55, sz * 0.35))
    dog.eyes(hy * 0.15, hz * 0.72, eye_sep, eye)
    if ear_kind == "point":
        mesh = cone(ear_size[1], ear_size[0], 8)
        dog.add("earL", "head", mesh, ear, t=(-hx * 0.72, hy * 0.35, -hz * 0.1), r=(-8, 0, 18))
        dog.add("earR", "head", mesh, ear, t=(hx * 0.72, hy * 0.35, -hz * 0.1), r=(-8, 0, -18))
    else:
        mesh = ellipsoid(ear_size[0], ear_size[1], ear_size[2], 6, 8)
        dog.add("earL", "head", mesh, ear, t=(-hx * 0.95, hy * 0.05, -hz * 0.05), r=(16, 0, 22))
        dog.add("earR", "head", mesh, ear, t=(hx * 0.95, hy * 0.05, -hz * 0.05), r=(16, 0, -22))
    dog.add(
        "tail",
        "body",
        ellipsoid(0.07, 0.07, 0.20, 6, 8),
        tail_color or coat,
        0.25,
        t=(0, by * 0.35, -bz * 0.92),
        r=(-50, 0, 0),
    )
    return dog


def lighten(color):
    return tuple(min(1.0, c + 0.12) for c in color)


def pug():
    dog = build(
        "pug", TAN, TAN_DEEP, (0.36, 0.27, 0.22),
        body=(0.36, 0.26, 0.42),
        head=(0.30, 0.24, 0.22),
        snout=(0.16, 0.10, 0.08),
        leg_h=0.30, leg_r=0.06,
        ear_kind="drop", ear_size=(0.07, 0.10, 0.045),
        eye=0.07, eye_sep=0.11,
    )
    dog.add("hat", "head", cylinder(0.12, 0.20, 12), RED, 0.4, t=(0, 0.26, -0.02))
    dog.add("brim", "head", ellipsoid(0.30, 0.028, 0.30, 4, 12), (0.55, 0.12, 0.14), t=(0, 0.18, 0))
    dog.dump()


def pamuk():
    dog = build(
        "pamuk", WHITE, WHITE, WHITE,
        body=(0.40, 0.30, 0.48),
        head=(0.30, 0.26, 0.26),
        snout=(0.14, 0.11, 0.12),
        leg_h=0.28, leg_r=0.07,
        ear_kind="drop", ear_size=(0.12, 0.18, 0.07),
        belly=(0.93, 0.90, 0.88),
    )
    dog.dump()


def boncuk():
    dog = build(
        "boncuk", TAN, TAN, TAN,
        body=(0.22, 0.18, 0.28),
        head=(0.20, 0.18, 0.16),
        snout=(0.08, 0.06, 0.07),
        leg_h=0.22, leg_r=0.035,
        ear_kind="point", ear_size=(0.07, 0.34, 0),
        eye=0.055, eye_sep=0.07,
    )
    dog.dump()


def tombul():
    dog = build(
        "tombul", TAN, TAN_DEEP, (0.34, 0.26, 0.22),
        body=(0.46, 0.22, 0.40),
        head=(0.32, 0.20, 0.22),
        snout=(0.18, 0.09, 0.08),
        leg_h=0.22, leg_r=0.065,
        ear_kind="drop", ear_size=(0.06, 0.08, 0.04),
        eye=0.06, eye_sep=0.12,
    )
    dog.dump()


def bebek():
    dog = build(
        "bebek", WHITE, DARK, WHITE,
        body=(0.26, 0.20, 0.32),
        head=(0.24, 0.22, 0.20),
        snout=(0.09, 0.07, 0.08),
        leg_h=0.24, leg_r=0.042,
        ear_kind="point", ear_size=(0.08, 0.16, 0),
        belly=PINK,
        eye=0.055, eye_sep=0.08,
    )
    dog.dump()


def kalp():
    dog = build(
        "kalp", CREAM, (0.95, 0.72, 0.76), CREAM,
        body=(0.30, 0.22, 0.40),
        head=(0.22, 0.20, 0.18),
        snout=(0.10, 0.07, 0.09),
        leg_h=0.30, leg_r=0.048,
        ear_kind="point", ear_size=(0.07, 0.18, 0),
        eye=0.055, eye_sep=0.08,
    )
    dog.add("stripe", "body", ellipsoid(0.32, 0.06, 0.42, 6, 10), RED, 0.35, t=(0, 0.02, 0.02))
    dog.add("heart", "body", ellipsoid(0.09, 0.08, 0.04, 6, 8), RED, 0.45, t=(0, 0.02, 0.40))
    dog.dump()


def somun():
    dog = build(
        "somun", TAN, TAN_DEEP, TAN,
        body=(0.32, 0.24, 0.46),
        head=(0.24, 0.20, 0.20),
        snout=(0.12, 0.08, 0.12),
        ear_kind="drop", ear_size=(0.08, 0.14, 0.05),
    )
    dog.add("bread", "body", cylinder(0.46, 0.09, 10), CREAM, 0.2, t=(0, 0.28, 0.02), r=(0, 0, 90))
    dog.dump()


def kahkaha():
    dog = build(
        "kahkaha", TAN, TAN_DEEP, TAN,
        body=(0.34, 0.25, 0.48),
        head=(0.28, 0.24, 0.24),
        snout=(0.15, 0.10, 0.12),
        ear_kind="drop", ear_size=(0.08, 0.14, 0.05),
        skip_mouth=True,
    )
    dog.add("jaw", "head", ellipsoid(0.16, 0.12, 0.08, 6, 8), DARK, 0.15, t=(0, -0.10, 0.22))
    dog.add("tongue", "jaw", ellipsoid(0.07, 0.10, 0.04, 5, 6), TONGUE, 0.4, t=(0, -0.12, 0.02))
    dog.add("toothL", "jaw", ellipsoid(0.025, 0.04, 0.02, 4, 4), WHITE, 0.6, t=(-0.05, 0.06, 0.05))
    dog.add("toothR", "jaw", ellipsoid(0.025, 0.04, 0.02, 4, 4), WHITE, 0.6, t=(0.05, 0.06, 0.05))
    dog.dump()


def goz():
    dog = build(
        "goz", WHITE, DARK, WHITE,
        body=(0.32, 0.24, 0.50),
        head=(0.24, 0.22, 0.22),
        snout=(0.11, 0.08, 0.12),
        ear_kind="point", ear_size=(0.055, 0.22, 0),
        eye=0.085, eye_sep=0.09,
    )
    dog.add("cap", "head", ellipsoid(0.25, 0.10, 0.20, 6, 8), DARK, t=(0, 0.14, -0.02))
    dog.dump()


def ceket():
    dog = build(
        "ceket", NAVY, GREY, GREY,
        body=(0.34, 0.24, 0.48),
        head=(0.20, 0.18, 0.18),
        snout=(0.09, 0.07, 0.09),
        ear_kind="drop", ear_size=(0.06, 0.11, 0.04),
        tail_color=NAVY,
        belly=(0.10, 0.14, 0.24),
        head_color=GREY,
    )
    dog.add("badge", "body", ellipsoid(0.06, 0.06, 0.025, 5, 6), GOLD, 0.7, t=(-0.12, 0.04, 0.42))
    dog.dump()


def kangal():
    dog = build(
        "kangal", (0.78, 0.64, 0.42), (0.62, 0.48, 0.30), (0.82, 0.68, 0.46),
        body=(0.38, 0.28, 0.58),
        head=(0.26, 0.22, 0.24),
        snout=(0.12, 0.09, 0.16),
        leg_h=0.40, leg_r=0.065,
        ear_kind="drop", ear_size=(0.08, 0.18, 0.045),
        eye=0.055, eye_sep=0.09,
        mask=DARK,
    )
    dog.dump()


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for make in (pug, pamuk, boncuk, tombul, bebek, kalp, somun, kahkaha, goz, ceket, kangal):
        make()
