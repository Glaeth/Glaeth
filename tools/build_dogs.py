#!/usr/bin/env python3
"""Sitting toy dogs, big head toward the camera, like a 3D character in a room."""

import json
import math
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "dogs")

WHITE = (0.98, 0.97, 0.95)
DARK = (0.12, 0.09, 0.08)
PINK = (0.95, 0.55, 0.62)
TONGUE = (0.90, 0.42, 0.48)
GOLD = (0.95, 0.74, 0.22)
NAVY = (0.12, 0.22, 0.42)
RED = (0.82, 0.18, 0.24)


def ellipsoid(stacks=10, slices=14):
    verts, norms, idx = [], [], []
    for i in range(stacks + 1):
        phi = i / stacks * math.pi
        for j in range(slices + 1):
            theta = j / slices * math.tau
            nx = math.sin(phi) * math.cos(theta)
            ny = math.cos(phi)
            nz = math.sin(phi) * math.sin(theta)
            verts.append((nx, ny, nz))
            norms.append((nx, ny, nz))
    for i in range(stacks):
        for j in range(slices):
            a = i * (slices + 1) + j
            b = a + slices + 1
            idx += [a, b, a + 1, b, b + 1, a + 1]
    return verts, norms, idx


def cone(slices=10):
    """Tip at y=1, base at y=0, radius 1. Scale sets the real size."""
    verts = [(0.0, 1.0, 0.0)]
    norms = [(0.0, 1.0, 0.0)]
    slope = 1.0
    for i in range(slices):
        a = i / slices * math.tau
        c, s = math.cos(a), math.sin(a)
        verts.append((c, 0.0, s))
        length = math.sqrt(1 + slope * slope)
        norms.append((c / length, slope / length, s / length))
    base = len(verts)
    verts.append((0.0, 0.0, 0.0))
    norms.append((0.0, -1.0, 0.0))
    idx = []
    for i in range(slices):
        j = 1 + i
        k = 1 + (i + 1) % slices
        idx += [0, k, j, base, j, k]
    return verts, norms, idx


UNIT = ellipsoid()
CONE = cone()
EYE = ellipsoid(8, 10)


def rot_x(v, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v
    return (x, y * c - z * s, y * s + z * c)


def rot_y(v, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v
    return (x * c + z * s, y, -x * s + z * c)


def rot_z(v, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v
    return (x * c - y * s, x * s + y * c, z)


class Dog:
    def __init__(self, name):
        self.name = name
        self.parts = []
        self.placed = {}

    def add(self, name, parent, pos, size, color, spec=0.32, rot=(0, 0, 0), mesh=None):
        mesh = UNIT if mesh is None else mesh
        if parent is None:
            local_t, local_s = pos, size
            index = -1
        else:
            origin, scale = self.placed[parent]
            local_t = tuple((pos[i] - origin[i]) / scale[i] for i in range(3))
            local_s = tuple(size[i] / scale[i] for i in range(3))
            index = next(i for i, part in enumerate(self.parts) if part["name"] == parent)
        self.placed[name] = (pos, size)
        self.parts.append({
            "name": name,
            "parent": index,
            "t": [round(v, 4) for v in local_t],
            "r": [round(v, 4) for v in rot],
            "s": [round(v, 4) for v in local_s],
            "color": [round(v, 4) for v in color],
            "spec": spec,
            "positions": [round(n, 4) for v in mesh[0] for n in v],
            "normals": [round(n, 4) for v in mesh[1] for n in v],
            "indices": mesh[2],
        })

    def save(self):
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, self.name + ".json")
        with open(path, "w", encoding="utf-8") as handle:
            json.dump({"parts": self.parts}, handle, separators=(",", ":"))
        print(f"{self.name:8} {len(self.parts):2} parts")


def face(dog, coat, belly, snout, nose):
    dog.add("head", "body", (0.0, 1.00, 0.08), (0.50, 0.44, 0.40), coat, 0.38)
    dog.add("muzzle", "head", (0.0, 0.86, 0.40), (0.20, 0.13, snout), belly, 0.28)
    tip = 0.40 + snout * 0.72
    dog.add("nose", "head", (0.0, 0.90, tip), (0.075, 0.055, 0.045), nose, 0.8)
    dog.add("glint", "head", (0.02, 0.92, tip + 0.03), (0.018, 0.014, 0.01), WHITE, 1.0)
    dog.add("mouth", "head", (0.0, 0.78, 0.40), (0.11, 0.028, 0.05), (0.28, 0.10, 0.11), 0.2)
    dog.add("tongue", "head", (0.0, 0.74, 0.46), (0.055, 0.02, 0.07), TONGUE, 0.45)
    for side, x in (("L", -0.16), ("R", 0.16)):
        dog.add(f"eye{side}", "head", (x, 1.06, 0.40), (0.105, 0.125, 0.045), WHITE, 0.9, mesh=EYE)
        dog.add(f"pupil{side}", "head", (x, 1.03, 0.44), (0.052, 0.064, 0.028), DARK, 0.95, mesh=EYE)
        dog.add(f"spark{side}", "head", (x + 0.03, 1.09, 0.46), (0.024, 0.024, 0.012), WHITE, 1.0, mesh=EYE)


def ears(dog, kind, color):
    # Ears sit outside the head so they read from the front, the way the
    # reference character's ears break the silhouette.
    if kind == "point":
        dog.add("earL", "head", (-0.22, 1.36, -0.02), (0.15, 0.40, 0.12), color, 0.25, rot=(-6, 0, 14), mesh=CONE)
        dog.add("earR", "head", (0.22, 1.36, -0.02), (0.15, 0.40, 0.12), color, 0.25, rot=(-6, 0, -14), mesh=CONE)
    elif kind == "fold":
        dog.add("earL", "head", (-0.54, 0.92, 0.12), (0.16, 0.22, 0.09), color, 0.22, rot=(18, 0, 42))
        dog.add("earR", "head", (0.54, 0.92, 0.12), (0.16, 0.22, 0.09), color, 0.22, rot=(18, 0, -42))
    else:
        dog.add("earL", "head", (-0.58, 0.74, 0.10), (0.12, 0.34, 0.08), color, 0.2, rot=(16, 0, 12))
        dog.add("earR", "head", (0.58, 0.74, 0.10), (0.12, 0.34, 0.08), color, 0.2, rot=(16, 0, -12))


def sit(name, coat, belly, ear, kind, snout=0.15, nose=DARK, collar=GOLD, blush=False):
    dog = Dog(name)
    dog.add("body", None, (0.0, 0.48, 0.0), (0.40, 0.34, 0.30), coat, 0.26)
    dog.add("belly", "body", (0.0, 0.40, 0.16), (0.26, 0.22, 0.12), belly, 0.18)
    dog.add("haunch", "body", (0.0, 0.24, -0.10), (0.42, 0.16, 0.26), coat, 0.2)
    for side, x in (("L", -0.15), ("R", 0.15)):
        dog.add(f"legF{side}", "body", (x, 0.22, 0.12), (0.075, 0.20, 0.075), coat, 0.22)
        dog.add(f"paw{side}", f"legF{side}", (x, 0.055, 0.18), (0.10, 0.045, 0.12), belly, 0.28)
        dog.add(f"toe{side}", f"paw{side}", (x, 0.07, 0.28), (0.045, 0.028, 0.035), belly, 0.3)
    for side, x in (("L", -0.24), ("R", 0.24)):
        dog.add(f"legB{side}", "body", (x, 0.13, -0.02), (0.13, 0.09, 0.16), coat, 0.18)
    dog.add("tail", "body", (0.46, 0.58, -0.04), (0.10, 0.24, 0.08), coat, 0.24, rot=(10, 25, -55))
    dog.add("collar", "body", (0.0, 0.74, 0.08), (0.24, 0.045, 0.18), collar, 0.65)
    face(dog, coat, belly, snout, nose)
    ears(dog, kind, ear)
    if blush:
        dog.add("cheekL", "head", (-0.28, 0.92, 0.28), (0.07, 0.04, 0.03), PINK, 0.2)
        dog.add("cheekR", "head", (0.28, 0.92, 0.28), (0.07, 0.04, 0.03), PINK, 0.2)
    return dog


def build():
    dogs = [
        sit("pug", (0.96, 0.78, 0.52), (0.93, 0.62, 0.48), (0.55, 0.34, 0.24), "fold", 0.11, DARK, RED),
        sit("pamuk", (0.96, 0.94, 0.90), (0.98, 0.86, 0.86), (0.90, 0.78, 0.74), "flop", 0.14, PINK, (0.95, 0.45, 0.62), True),
        sit("boncuk", (0.98, 0.72, 0.28), (0.98, 0.88, 0.62), (0.72, 0.40, 0.16), "point", 0.12, DARK, GOLD),
        sit("tombul", (0.62, 0.40, 0.24), (0.86, 0.66, 0.46), (0.42, 0.26, 0.16), "flop", 0.16, DARK, GOLD),
        sit("bebek", (0.98, 0.82, 0.86), (0.99, 0.94, 0.94), (0.96, 0.70, 0.76), "flop", 0.10, PINK, (0.98, 0.55, 0.70), True),
        sit("kalp", (0.93, 0.28, 0.38), (0.98, 0.72, 0.74), (0.72, 0.16, 0.28), "point", 0.13, DARK, (0.98, 0.82, 0.30), True),
        sit("somun", (0.86, 0.62, 0.34), (0.96, 0.84, 0.60), (0.62, 0.40, 0.20), "fold", 0.15, DARK, (0.55, 0.32, 0.16)),
        sit("kahkaha", (0.20, 0.16, 0.14), (0.55, 0.42, 0.32), (0.10, 0.08, 0.08), "point", 0.16, (0.02, 0.02, 0.02), GOLD),
        sit("goz", (0.95, 0.93, 0.88), (0.98, 0.96, 0.92), (0.75, 0.72, 0.68), "point", 0.13, DARK, NAVY),
        sit("ceket", (0.93, 0.90, 0.84), (0.96, 0.93, 0.88), (0.70, 0.66, 0.60), "fold", 0.14, DARK, GOLD),
        sit("kangal", (0.72, 0.48, 0.22), (0.55, 0.32, 0.16), (0.48, 0.28, 0.12), "point", 0.22, DARK, (0.85, 0.15, 0.18)),
    ]
    # Forehead folds so the pug reads as a pug, not a plain ball.
    dogs[0].add("foldA", "head", (0.0, 1.22, 0.30), (0.16, 0.035, 0.04), (0.55, 0.34, 0.22), 0.15)
    dogs[0].add("foldB", "head", (0.0, 1.14, 0.36), (0.12, 0.028, 0.035), (0.55, 0.34, 0.22), 0.15)
    # Loaf of bread balanced on Somun.
    dogs[6].add("loaf", "head", (0.0, 1.40, 0.02), (0.20, 0.08, 0.13), (0.86, 0.58, 0.28), 0.15)
    # Pale chest patch already there; Kahkaha gets a cream brow spot.
    dogs[7].add("brow", "head", (0.0, 1.22, 0.36), (0.10, 0.04, 0.03), (0.90, 0.82, 0.62), 0.2)
    # Mask band behind the eyes.
    dogs[8].add("mask", "head", (0.0, 1.04, 0.30), (0.42, 0.09, 0.06), (0.10, 0.12, 0.18), 0.25)
    # Jacket shell over the chest. Drawn with the body so the head stays in front.
    dogs[9].add("jacket", "body", (0.0, 0.46, 0.06), (0.46, 0.30, 0.26), NAVY, 0.35)
    dogs[9].add("pocket", "body", (0.12, 0.40, 0.28), (0.08, 0.07, 0.02), GOLD, 0.4)
    for dog in dogs:
        dog.save()


def preview(path, dog="pug"):
    """Front-view raster, so the silhouette can be checked without a device."""
    with open(os.path.join(OUT, dog + ".json"), encoding="utf-8") as handle:
        parts = json.load(handle)["parts"]
    width, height = 280, 360
    color = [[(150, 186, 214) for _ in range(width)] for _ in range(height)]
    depth = [[-9.0 for _ in range(width)] for _ in range(height)]

    def chain(index):
        part = parts[index]
        parent = part["parent"]
        return [] if parent < 0 else chain(parent) + [parent]

    def world_point(index, vertex):
        part = parts[index]
        point = (
            vertex[0] * part["s"][0],
            vertex[1] * part["s"][1],
            vertex[2] * part["s"][2],
        )
        point = rot_x(point, part["r"][0])
        point = rot_y(point, part["r"][1])
        point = rot_z(point, part["r"][2])
        point = (point[0] + part["t"][0], point[1] + part["t"][1], point[2] + part["t"][2])
        for parent in reversed(chain(index)):
            p = parts[parent]
            point = (
                point[0] * p["s"][0],
                point[1] * p["s"][1],
                point[2] * p["s"][2],
            )
            point = rot_x(point, p["r"][0])
            point = rot_y(point, p["r"][1])
            point = rot_z(point, p["r"][2])
            point = (point[0] + p["t"][0], point[1] + p["t"][1], point[2] + p["t"][2])
        return point

    def project(point):
        turned = rot_y(point, -18)
        x = int((turned[0] + 0.85) / 1.70 * (width - 1))
        y = int((1.70 - turned[1]) / 1.85 * (height - 1))
        return x, y, turned[2]

    for index, part in enumerate(parts):
        positions = part["positions"]
        verts = [
            world_point(index, (positions[i], positions[i + 1], positions[i + 2]))
            for i in range(0, len(positions), 3)
        ]
        rgb = tuple(int(c * 255) for c in part["color"])
        indices = part["indices"]
        for t in range(0, len(indices), 3):
            a, b, c = (project(verts[indices[t + k]]) for k in range(3))
            pts = sorted((a, b, c), key=lambda p: p[1])
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts
            if y2 == y0:
                continue
            for y in range(max(0, y0), min(height - 1, y2) + 1):
                if y < y1:
                    t1 = 0 if y1 == y0 else (y - y0) / (y1 - y0)
                    xa, za = x0 + (x1 - x0) * t1, z0 + (z1 - z0) * t1
                else:
                    t1 = 0 if y2 == y1 else (y - y1) / (y2 - y1)
                    xa, za = x1 + (x2 - x1) * t1, z1 + (z2 - z1) * t1
                t2 = (y - y0) / (y2 - y0)
                xb, zb = x0 + (x2 - x0) * t2, z0 + (z2 - z0) * t2
                if xa > xb:
                    xa, xb, za, zb = xb, xa, zb, za
                span = max(1, int(xb) - int(xa))
                for x in range(max(0, int(xa)), min(width - 1, int(xb)) + 1):
                    z = za + (zb - za) * ((x - xa) / span)
                    if z > depth[y][x]:
                        depth[y][x] = z
                        shade = 0.55 + 0.45 * max(0.0, min(1.0, (z + 0.2) / 1.1))
                        color[y][x] = tuple(int(ch * shade) for ch in rgb)
    rows = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            row += bytes(color[y][x])
        rows.append(bytes(row))
    with open(path, "wb") as handle:
        handle.write(f"P6\n{width} {height}\n255\n".encode())
        handle.write(b"".join(rows))
    print("preview", path)


if __name__ == "__main__":
    build()
    root = os.path.join(os.path.dirname(__file__), "..", "dist")
    preview(os.path.join(root, "pug_front.ppm"), "pug")
    preview(os.path.join(root, "boncuk_front.ppm"), "boncuk")
