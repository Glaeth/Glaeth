#!/usr/bin/env python3
"""Build a distinct low-poly toy mesh for every dog."""

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
GREY = (0.70, 0.68, 0.64)
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
            # Ellipsoid normal.
            nnx, nny, nnz = nx / max(rx, 1e-4), ny / max(ry, 1e-4), nz / max(rz, 1e-4)
            length = math.sqrt(nnx * nnx + nny * nny + nnz * nnz) or 1
            norms.append((nnx / length, nny / length, nnz / length))
    for i in range(stacks):
        for j in range(slices):
            a = i * (slices + 1) + j
            b = a + slices + 1
            idx += [a, b, a + 1, b, b + 1, a + 1]
    return verts, norms, idx


def cone(height, radius, slices=12):
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
        idx += [0, k, j]
        idx += [base, j, k]
    return verts, norms, idx


def cylinder(height, radius, slices=12):
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


def merge(parts):
    verts, norms, idx = [], [], []
    for mesh in parts:
        base = len(verts)
        verts.extend(mesh[0])
        norms.extend(mesh[1])
        idx.extend(i + base for i in mesh[2])
    return verts, norms, idx


class Dog:
    def __init__(self, name):
        self.name = name
        self.parts = []

    def add(self, name, parent, mesh, color, spec=0.28, t=(0, 0, 0), r=(0, 0, 0), s=(1, 1, 1)):
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

    def eyes(self, parent, y, z, sep, scale=0.10):
        eye = ellipsoid(scale, scale * 1.18, scale * 0.62, 6, 10)
        pupil = ellipsoid(scale * 0.48, scale * 0.55, scale * 0.36, 5, 8)
        spark = ellipsoid(scale * 0.16, scale * 0.16, scale * 0.12, 4, 6)
        for side, x in (("L", -sep), ("R", sep)):
            self.add(f"eye{side}", parent, eye, WHITE, 0.85, (x, y, z))
            self.add(f"pupil{side}", parent, pupil, DARK, 0.9, (x, y - scale * 0.05, z + scale * 0.42))
            self.add(
                f"spark{side}",
                parent,
                spark,
                WHITE,
                1.0,
                (x - scale * 0.22, y + scale * 0.28, z + scale * 0.62),
            )

    def nose(self, parent, y, z, s=0.07):
        self.add("nose", parent, ellipsoid(s, s * 0.72, s * 0.7), DARK, 0.7, (0, y, z))

    def tail(self, color):
        self.add("tail", "body", ellipsoid(0.10, 0.10, 0.22), color, 0.25, (0, 0.42, -0.42), (28, 0, 0))

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
        print(self.name, sum(len(p["indices"]) // 3 for p in out), "tris")


def pug():
    dog = Dog("pug")
    dog.add("body", None, ellipsoid(0.62, 0.48, 0.55), TAN, t=(0, 0.48, 0))
    dog.add("head", "body", ellipsoid(0.50, 0.40, 0.38), TAN, t=(0, 0.55, 0.08))
    dog.add("muzzle", "head", ellipsoid(0.26, 0.16, 0.18), (0.36, 0.27, 0.22), t=(0, -0.12, 0.28))
    dog.add("earL", "head", ellipsoid(0.12, 0.16, 0.08), TAN_DEEP, t=(-0.40, 0.18, -0.02), r=(0, 0, 28))
    dog.add("earR", "head", ellipsoid(0.12, 0.16, 0.08), TAN_DEEP, t=(0.40, 0.18, -0.02), r=(0, 0, -28))
    dog.eyes("head", 0.06, 0.30, 0.16, 0.09)
    dog.nose("head", -0.02, 0.46, 0.07)
    dog.add("mouth", "head", ellipsoid(0.10, 0.035, 0.04), (0.45, 0.24, 0.24), t=(0, -0.16, 0.40))
    dog.add("hat", "head", cylinder(0.16, 0.22, 14), RED, 0.35, t=(0, 0.42, 0))
    dog.add("brim", "head", ellipsoid(0.40, 0.035, 0.40), (0.55, 0.12, 0.14), t=(0, 0.32, 0))
    dog.tail(TAN)
    dog.dump()


def pamuk():
    dog = Dog("pamuk")
    dog.add("body", None, ellipsoid(0.70, 0.62, 0.66), WHITE, t=(0, 0.58, 0))
    dog.add("head", "body", ellipsoid(0.58, 0.52, 0.54), WHITE, t=(0, 0.62, 0.06))
    dog.add("earL", "head", ellipsoid(0.20, 0.28, 0.12), WHITE, t=(-0.48, -0.02, 0), r=(0, 0, 18))
    dog.add("earR", "head", ellipsoid(0.20, 0.28, 0.12), WHITE, t=(0.48, -0.02, 0), r=(0, 0, -18))
    dog.eyes("head", 0.04, 0.40, 0.16, 0.10)
    dog.nose("head", -0.08, 0.50, 0.07)
    dog.add("mouth", "head", ellipsoid(0.08, 0.03, 0.03), PINK, t=(0, -0.18, 0.46))
    dog.tail(WHITE)
    dog.dump()


def boncuk():
    dog = Dog("boncuk")
    dog.add("body", None, ellipsoid(0.38, 0.42, 0.34), TAN, t=(0, 0.40, 0))
    dog.add("head", "body", ellipsoid(0.34, 0.32, 0.30), TAN, t=(0, 0.48, 0.04))
    dog.add("earL", "head", cone(0.55, 0.16, 10), TAN, t=(-0.22, 0.16, 0), r=(0, 0, 16))
    dog.add("earR", "head", cone(0.55, 0.16, 10), TAN, t=(0.22, 0.16, 0), r=(0, 0, -16))
    dog.add("innerL", "earL", cone(0.40, 0.08, 8), PINK, t=(0.02, 0.02, 0.04))
    dog.add("innerR", "earR", cone(0.40, 0.08, 8), PINK, t=(-0.02, 0.02, 0.04))
    dog.eyes("head", 0.02, 0.24, 0.12, 0.08)
    dog.nose("head", -0.08, 0.32, 0.055)
    dog.add("mouth", "head", ellipsoid(0.06, 0.025, 0.03), PINK, t=(0, -0.12, 0.28))
    dog.add("legL", "body", cylinder(0.22, 0.05, 8), TAN_DEEP, t=(-0.12, -0.28, 0.08))
    dog.add("legR", "body", cylinder(0.22, 0.05, 8), TAN_DEEP, t=(0.12, -0.28, 0.08))
    dog.tail(TAN)
    dog.dump()


def tombul():
    dog = Dog("tombul")
    dog.add("body", None, ellipsoid(0.78, 0.42, 0.62), TAN, t=(0, 0.42, 0))
    dog.add("head", "body", ellipsoid(0.55, 0.36, 0.40), TAN, t=(0, 0.28, 0.18))
    dog.add("muzzle", "head", ellipsoid(0.32, 0.20, 0.18), (0.34, 0.26, 0.22), t=(0, -0.08, 0.26))
    dog.add("earL", "head", ellipsoid(0.10, 0.12, 0.07), TAN_DEEP, t=(-0.42, 0.16, 0))
    dog.add("earR", "head", ellipsoid(0.10, 0.12, 0.07), TAN_DEEP, t=(0.42, 0.16, 0))
    dog.eyes("head", 0.06, 0.28, 0.16, 0.085)
    dog.nose("head", -0.02, 0.42, 0.075)
    dog.add("mouth", "head", ellipsoid(0.12, 0.03, 0.03), (0.45, 0.24, 0.24), t=(0, -0.14, 0.36))
    dog.tail(TAN)
    dog.dump()


def bebek():
    dog = Dog("bebek")
    dog.add("body", None, ellipsoid(0.48, 0.42, 0.42), WHITE, t=(0, 0.48, 0))
    dog.add("belly", "body", ellipsoid(0.28, 0.22, 0.16), PINK, t=(0, -0.02, 0.28))
    dog.add("head", "body", ellipsoid(0.40, 0.34, 0.36), WHITE, t=(0, 0.50, 0.04))
    dog.add("earL", "head", cone(0.28, 0.16, 10), DARK, t=(-0.28, 0.16, 0), r=(0, 0, 24))
    dog.add("earR", "head", cone(0.28, 0.16, 10), DARK, t=(0.28, 0.16, 0), r=(0, 0, -24))
    dog.add("armL", "body", cylinder(0.36, 0.07, 8), WHITE, t=(-0.42, 0.28, 0.08), r=(0, 0, 70))
    dog.add("armR", "body", cylinder(0.36, 0.07, 8), WHITE, t=(0.42, 0.28, 0.08), r=(0, 0, -70))
    dog.add("pawL", "armL", ellipsoid(0.08, 0.06, 0.06), PINK, t=(0, 0.22, 0))
    dog.add("pawR", "armR", ellipsoid(0.08, 0.06, 0.06), PINK, t=(0, 0.22, 0))
    dog.eyes("head", 0.02, 0.28, 0.13, 0.075)
    dog.nose("head", -0.06, 0.36, 0.055)
    dog.add("mouth", "head", ellipsoid(0.07, 0.025, 0.03), PINK, t=(0, -0.10, 0.32))
    dog.tail(WHITE)
    dog.dump()


def kalp():
    dog = Dog("kalp")
    dog.add("body", None, ellipsoid(0.52, 0.46, 0.46), CREAM, t=(0, 0.50, 0))
    dog.add("stripe", "body", ellipsoid(0.54, 0.08, 0.48), RED, t=(0, -0.06, 0.02))
    heart = merge([
        ellipsoid(0.10, 0.10, 0.06),
        ([ (x + 0.08, y, z) for x, y, z in ellipsoid(0.10, 0.10, 0.06)[0] ], ellipsoid(0.10, 0.10, 0.06)[1], ellipsoid(0.10, 0.10, 0.06)[2]),
    ])
    # Simpler heart: one red ellipsoid on the chest.
    dog.add("heart", "body", ellipsoid(0.16, 0.14, 0.08), RED, 0.4, t=(0, 0.08, 0.40))
    dog.add("head", "body", ellipsoid(0.40, 0.34, 0.34), WHITE, t=(0, 0.52, 0.02))
    dog.add("earL", "head", cone(0.26, 0.15, 10), (0.95, 0.72, 0.76), t=(-0.28, 0.16, 0), r=(0, 0, 22))
    dog.add("earR", "head", cone(0.26, 0.15, 10), (0.95, 0.72, 0.76), t=(0.28, 0.16, 0), r=(0, 0, -22))
    dog.eyes("head", 0.02, 0.26, 0.13, 0.075)
    dog.nose("head", -0.06, 0.34, 0.05)
    dog.add("mouth", "head", ellipsoid(0.06, 0.022, 0.03), PINK, t=(0, -0.10, 0.30))
    dog.tail(CREAM)
    dog.dump()


def somun():
    dog = Dog("somun")
    dog.add("body", None, ellipsoid(0.58, 0.46, 0.50), TAN, t=(0, 0.50, 0))
    dog.add("head", "body", ellipsoid(0.42, 0.36, 0.36), TAN, t=(0, 0.52, 0.08))
    dog.add("earL", "head", ellipsoid(0.16, 0.22, 0.08), TAN_DEEP, t=(-0.38, 0.02, 0), r=(0, 0, 20))
    dog.add("earR", "head", ellipsoid(0.16, 0.22, 0.08), TAN_DEEP, t=(0.38, 0.02, 0), r=(0, 0, -20))
    dog.eyes("head", 0.04, 0.26, 0.14, 0.08)
    dog.nose("head", -0.06, 0.36, 0.06)
    dog.add("mouth", "head", ellipsoid(0.08, 0.03, 0.03), PINK, t=(0, -0.12, 0.32))
    dog.add("bread", "body", cylinder(0.70, 0.11, 12), CREAM, 0.2, t=(0, -0.05, 0.42), r=(0, 0, 80))
    dog.tail(TAN)
    dog.dump()


def kahkaha():
    dog = Dog("kahkaha")
    dog.add("body", None, ellipsoid(0.62, 0.50, 0.55), TAN, t=(0, 0.50, 0))
    dog.add("head", "body", ellipsoid(0.52, 0.42, 0.46), TAN, t=(0, 0.52, 0.06), r=(-12, 0, 0))
    dog.add("earL", "head", ellipsoid(0.12, 0.18, 0.08), TAN_DEEP, t=(-0.46, 0.10, 0))
    dog.add("earR", "head", ellipsoid(0.12, 0.18, 0.08), TAN_DEEP, t=(0.46, 0.10, 0))
    dog.add("eyeL", "head", ellipsoid(0.09, 0.035, 0.04), DARK, 0.4, t=(-0.16, 0.10, 0.36))
    dog.add("eyeR", "head", ellipsoid(0.09, 0.035, 0.04), DARK, 0.4, t=(0.16, 0.10, 0.36))
    dog.add("mouth", "head", ellipsoid(0.28, 0.22, 0.16), DARK, 0.15, t=(0, -0.16, 0.30))
    dog.add("tongue", "mouth", ellipsoid(0.12, 0.16, 0.06), TONGUE, 0.4, t=(0, -0.16, 0.08))
    dog.add("toothL", "mouth", ellipsoid(0.04, 0.06, 0.03), WHITE, 0.6, t=(-0.10, 0.10, 0.10))
    dog.add("toothR", "mouth", ellipsoid(0.04, 0.06, 0.03), WHITE, 0.6, t=(0.10, 0.10, 0.10))
    dog.add("nose", "head", ellipsoid(0.08, 0.05, 0.06), DARK, 0.6, t=(0, 0.16, 0.44))
    dog.tail(TAN)
    dog.dump()


def goz():
    dog = Dog("goz")
    dog.add("body", None, ellipsoid(0.58, 0.50, 0.52), WHITE, t=(0, 0.50, 0))
    dog.add("head", "body", ellipsoid(0.48, 0.46, 0.44), WHITE, t=(0, 0.56, 0.04))
    dog.add("cap", "head", ellipsoid(0.50, 0.22, 0.46), DARK, t=(0, 0.28, -0.02))
    dog.add("earL", "head", ellipsoid(0.10, 0.28, 0.08), DARK, t=(-0.42, -0.02, 0))
    dog.add("earR", "head", ellipsoid(0.10, 0.28, 0.08), DARK, t=(0.42, -0.02, 0))
    dog.eyes("head", 0.02, 0.32, 0.16, 0.14)
    dog.nose("head", -0.12, 0.42, 0.08)
    dog.add("mouth", "head", ellipsoid(0.07, 0.025, 0.03), PINK, t=(0, -0.20, 0.36))
    dog.tail(WHITE)
    dog.dump()


def ceket():
    dog = Dog("ceket")
    dog.add("body", None, ellipsoid(0.62, 0.50, 0.42), NAVY, 0.35, t=(0, 0.52, 0))
    dog.add("seam", "body", ellipsoid(0.04, 0.48, 0.08), (0.08, 0.12, 0.22), t=(0, 0.02, 0.36))
    dog.add("badge", "body", ellipsoid(0.08, 0.08, 0.03), GOLD, 0.7, t=(-0.18, 0.08, 0.38))
    dog.add("head", "body", ellipsoid(0.32, 0.28, 0.30), GREY, t=(0.28, 0.42, 0.16))
    dog.add("earL", "head", ellipsoid(0.10, 0.14, 0.06), GREY, t=(-0.22, 0.10, 0))
    dog.add("earR", "head", ellipsoid(0.10, 0.14, 0.06), GREY, t=(0.16, 0.12, -0.04))
    dog.eyes("head", 0.02, 0.18, 0.10, 0.06)
    dog.nose("head", -0.04, 0.28, 0.05)
    dog.add("mouth", "head", ellipsoid(0.05, 0.02, 0.02), PINK, t=(0.04, -0.08, 0.24))
    dog.tail(NAVY)
    dog.dump()


def kangal():
    dog = Dog("kangal")
    dog.add("body", None, ellipsoid(0.72, 0.58, 0.62), (0.78, 0.64, 0.42), t=(0, 0.58, 0))
    dog.add("head", "body", ellipsoid(0.52, 0.46, 0.48), (0.82, 0.68, 0.46), t=(0, 0.58, 0.10))
    dog.add("mask", "head", ellipsoid(0.40, 0.28, 0.22), DARK, t=(0, -0.02, 0.22))
    dog.add("earL", "head", ellipsoid(0.14, 0.30, 0.08), (0.62, 0.48, 0.30), t=(-0.40, 0.10, -0.02))
    dog.add("earR", "head", ellipsoid(0.14, 0.30, 0.08), (0.62, 0.48, 0.30), t=(0.40, 0.10, -0.02))
    dog.eyes("head", 0.06, 0.30, 0.15, 0.09)
    dog.nose("head", -0.08, 0.46, 0.09)
    dog.add("mouth", "head", ellipsoid(0.12, 0.04, 0.04), (0.55, 0.28, 0.26), t=(0, -0.18, 0.36))
    dog.add("tooth", "mouth", ellipsoid(0.035, 0.04, 0.02), WHITE, 0.6, t=(0.05, -0.02, 0.04))
    dog.tail((0.78, 0.64, 0.42))
    dog.dump()


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    pug()
    pamuk()
    boncuk()
    tombul()
    bebek()
    kalp()
    somun()
    kahkaha()
    goz()
    ceket()
    kangal()
