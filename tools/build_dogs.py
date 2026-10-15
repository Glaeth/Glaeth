#!/usr/bin/env python3
"""Smooth standing dogs. One blended surface, not a stack of spheres."""

import json
import math
import os

import numpy as np
from skimage.measure import marching_cubes

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "dogs")

WHITE = (0.98, 0.97, 0.95)
DARK = (0.10, 0.07, 0.06)
PINK = (0.95, 0.55, 0.62)
TONGUE = (0.90, 0.42, 0.48)


def ellipsoid_mesh(stacks=8, slices=10):
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


EYE = ellipsoid_mesh()


def smin(a, b, k):
    h = np.maximum(k - np.abs(a - b), 0.0) / k
    return np.minimum(a, b) - h * h * k * 0.25


def capsule(p, a, b, radius):
    a = np.asarray(a, dtype=np.float64)
    b = np.asarray(b, dtype=np.float64)
    pa = p - a
    ba = b - a
    denom = float(np.dot(ba, ba)) or 1.0
    h = np.clip((pa @ ba) / denom, 0.0, 1.0)
    return np.linalg.norm(pa - ba * h[:, None], axis=1) - radius


def ell(p, center, radii):
    r = np.asarray(radii, dtype=np.float64)
    q = (p - np.asarray(center, dtype=np.float64)) / r
    k0 = np.linalg.norm(q, axis=1)
    k1 = np.linalg.norm(q / r, axis=1)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-6)


def dog_field(p, kind):
    """Horizontal body, head in front, four legs. Reads as a dog from the side."""
    body = kind["body"]
    head_s = kind["head"]
    snout = kind["snout"]
    leg = kind["leg"]
    back = -0.34 * body
    front = 0.22 * body
    d = capsule(p, (0, 0.40, back), (0, 0.36, front), 0.115)
    d = smin(d, ell(p, (0, 0.33, front - 0.02), (0.125, 0.095, 0.11)), 0.05)
    hc = np.array((0.0, 0.58 + 0.03 * (head_s - 1), front + 0.10))
    d = smin(d, capsule(p, (0, 0.42, front - 0.02), (hc[0], hc[1] - 0.04, hc[2] - 0.04), 0.065), 0.045)
    hr = np.array((0.105, 0.095, 0.095)) * head_s
    d = smin(d, ell(p, hc, hr), 0.045)
    mz = hc + np.array((0.0, -0.035 * head_s, hr[2] * 0.55 + snout * 0.55))
    d = smin(d, ell(p, mz, (0.055 * head_s, 0.042 * head_s, max(snout, 0.05))), 0.035)
    for x in (-0.07, 0.07):
        for z, forward in ((front - 0.04, 0.03), (back + 0.06, -0.015)):
            top = np.array((x, 0.32, z))
            bot = np.array((x * 1.15, 0.04, z + forward))
            d = smin(d, capsule(p, top, bot, 0.032 * (0.85 + 0.2 * leg)), 0.028)
            d = smin(d, ell(p, bot + (0, -0.004, 0.02), (0.04, 0.016, 0.045)), 0.02)
    ear = kind["ear"]
    for side in (-1.0, 1.0):
        if ear == "point":
            base = hc + np.array((0.05 * side * head_s, 0.06 * head_s, -0.01))
            tip = base + np.array((0.035 * side, 0.18, -0.01))
            d = smin(d, capsule(p, base, tip, 0.022), 0.02)
        elif ear == "flop":
            base = hc + np.array((0.09 * side * head_s, 0.0, 0.0))
            tip = base + np.array((0.03 * side, -0.16, 0.02))
            d = smin(d, capsule(p, base, tip, 0.028), 0.025)
        else:
            base = hc + np.array((0.085 * side * head_s, 0.03 * head_s, 0.02))
            tip = base + np.array((0.03 * side, -0.05, 0.03))
            d = smin(d, capsule(p, base, tip, 0.026), 0.02)
    rump = np.array((0.0, 0.44, back - 0.02))
    d = smin(d, capsule(p, rump, rump + (0.05, 0.04, -0.04), 0.022), 0.025)
    return d, hc, mz, hr


def smooth_mesh(verts, faces, rounds=2):
    n = len(verts)
    acc = np.zeros((n, 3))
    cnt = np.zeros(n)
    edges = np.vstack([faces[:, [0, 1]], faces[:, [1, 2]], faces[:, [2, 0]]])
    np.add.at(acc, edges[:, 0], verts[edges[:, 1]])
    np.add.at(cnt, edges[:, 0], 1)
    for _ in range(rounds):
        avg = acc / np.maximum(cnt, 1)[:, None]
        verts = verts * 0.55 + avg * 0.45
        acc[:] = 0
        np.add.at(acc, edges[:, 0], verts[edges[:, 1]])
    return verts


def face_normals(verts, faces):
    a, b, c = verts[faces[:, 0]], verts[faces[:, 1]], verts[faces[:, 2]]
    n = np.cross(b - a, c - a)
    acc = np.zeros_like(verts)
    for k in range(3):
        np.add.at(acc, faces[:, k], n)
    length = np.linalg.norm(acc, axis=1)
    length[length < 1e-8] = 1
    return acc / length[:, None]


def colorize(verts, normals, kind, hc):
    coat = np.array(kind["coat"])
    belly = np.array(kind["belly"])
    ear = np.array(kind["ear_color"])
    cols = np.tile(coat, (len(verts), 1))
    y, z, x = verts[:, 1], verts[:, 2], verts[:, 0]
    belly_w = np.clip((0.40 - y) / 0.16, 0, 1) * np.clip((z + 0.15) / 0.35, 0, 1)
    belly_w *= np.clip(normals[:, 1] * -1.2 + normals[:, 2], 0, 1)
    cols = cols * (1 - belly_w[:, None]) + belly * belly_w[:, None]
    ear_w = np.clip((np.abs(x) - 0.10) / 0.08, 0, 1) * np.clip((y - (hc[1] - 0.08)) / 0.12, 0, 1)
    cols = cols * (1 - ear_w[:, None] * 0.85) + ear * ear_w[:, None] * 0.85
    if kind.get("mask"):
        mask = np.array(kind["mask"])
        band = np.clip((z - (hc[2] - 0.02)) / 0.12, 0, 1)
        band *= np.clip(1 - np.abs(x) / 0.12, 0, 1)
        band *= np.clip(1 - np.abs(y - (hc[1] - 0.04)) / 0.12, 0, 1)
        cols = cols * (1 - band[:, None] * 0.75) + mask * band[:, None] * 0.75
    # Gentle shade along the back so the coat is not one flat color.
    muzzle = np.clip((z - (hc[2] + 0.02)) / 0.08, 0, 1)
    cols = cols * (1 - muzzle[:, None] * 0.55) + belly * muzzle[:, None] * 0.55
    back = np.clip(normals[:, 1], 0, 1) * 0.08
    cols *= (0.94 + back)[:, None]
    return np.clip(cols, 0, 1)


def sculpt(kind):
    xs = np.linspace(-0.55, 0.55, 56)
    ys = np.linspace(-0.02, 1.15, 64)
    zs = np.linspace(-0.62, 0.85, 64)
    # Sample in chunks so the field stays vectorized without a huge temporary.
    volume = np.empty((len(xs), len(ys), len(zs)), dtype=np.float32)
    hc = mz = hr = None
    for i, x in enumerate(xs):
        yy, zz = np.meshgrid(ys, zs, indexing="ij")
        pts = np.column_stack([
            np.full(yy.size, x),
            yy.ravel(),
            zz.ravel(),
        ])
        field, hc, mz, hr = dog_field(pts, kind)
        volume[i] = field.reshape(yy.shape)
    verts, faces, _, _ = marching_cubes(
        volume,
        level=0.0,
        spacing=(xs[1] - xs[0], ys[1] - ys[0], zs[1] - zs[0]),
    )
    verts = verts + np.array([xs[0], ys[0], zs[0]])
    verts = smooth_mesh(verts.astype(np.float64), faces.astype(np.int32), rounds=2)
    normals = face_normals(verts, faces)
    center = verts.mean(axis=0)
    if np.mean(np.sum(normals * (verts - center), axis=1)) < 0:
        faces = faces[:, ::-1]
        normals = -normals
    colors = colorize(verts, normals, kind, hc)
    return verts, faces, normals, colors, hc, mz, hr


def pack(name, parent, mesh, color, spec, t=(0, 0, 0), r=(0, 0, 0), s=(1, 1, 1), colors=None):
    part = {
        "name": name,
        "parent": parent,
        "t": [round(float(v), 4) for v in t],
        "r": [round(float(v), 4) for v in r],
        "s": [round(float(v), 4) for v in s],
        "color": [round(float(v), 4) for v in color],
        "spec": spec,
        "positions": [round(float(n), 4) for v in mesh[0] for n in v],
        "normals": [round(float(n), 4) for v in mesh[1] for n in v],
        "indices": [int(i) for i in mesh[2]],
    }
    if colors is not None:
        part["colors"] = [round(float(n), 4) for row in colors for n in row]
    return part


def face_parts(parts, hc, mz, hr, nose):
    head_i = len(parts)
    tiny = ellipsoid_mesh(4, 6)
    hidden = ([(x * 0.012, y * 0.012, z * 0.012) for x, y, z in tiny[0]], tiny[1], tiny[2])
    # Scale stays 1 so the eyes, which are children of the head, are not shrunk.
    parts.append(pack("head", 0, hidden, (0, 0, 0), 0.0, t=tuple(hc), s=(1, 1, 1)))
    eye_r = 0.046 * max(hr[0] / 0.12, 0.9)
    for side, x in (("L", -1), ("R", 1)):
        eye = hc + np.array((0.42 * x * hr[0], 0.015 * hr[1], hr[2] * 0.78))
        pupil = eye + np.array((0.0, -0.004, eye_r * 0.55))
        spark = eye + np.array((0.012 * x, 0.012, eye_r * 0.8))
        local = lambda q: tuple(q - hc)
        parts.append(pack(f"eye{side}", head_i, EYE, WHITE, 0.9, t=local(eye), s=(eye_r, eye_r * 1.15, eye_r * 0.55)))
        parts.append(pack(f"pupil{side}", head_i, EYE, DARK, 0.95, t=local(pupil), s=(eye_r * 0.48, eye_r * 0.55, eye_r * 0.3)))
        parts.append(pack(f"spark{side}", head_i, EYE, WHITE, 1.0, t=local(spark), s=(eye_r * 0.22, eye_r * 0.22, eye_r * 0.12)))
    tip = mz + np.array((0.0, 0.01, hr[2] * 0.15 + 0.06))
    parts.append(pack("nose", head_i, EYE, nose, 0.75, t=tuple(tip - hc), s=(0.036, 0.026, 0.024)))
    mouth = mz + np.array((0.0, -0.045, 0.02))
    parts.append(pack("mouth", head_i, EYE, (0.28, 0.10, 0.11), 0.2, t=tuple(mouth - hc), s=(0.04, 0.012, 0.02)))
    tongue = mouth + np.array((0.0, -0.01, 0.025))
    parts.append(pack("tongue", head_i, EYE, TONGUE, 0.4, t=tuple(tongue - hc), s=(0.022, 0.01, 0.03)))


def tail_parts(parts, body, coat):
    rump = np.array((0.0, 0.46, -0.36 * body))
    mid = rump + np.array((0.10, 0.10, -0.08))
    tip = mid + np.array((0.08, 0.12, 0.0))
    mesh = ellipsoid_mesh(6, 8)
    parts.append(pack("tail", 0, mesh, coat, 0.22, t=tuple((rump + mid) / 2), s=(0.045, 0.045, 0.12)))
    parts.append(pack("tailTip", 0, mesh, coat, 0.22, t=tuple((mid + tip) / 2), s=(0.032, 0.032, 0.09)))


def build_one(name, kind):
    verts, faces, normals, colors, hc, mz, hr = sculpt(kind)
    parts = [pack(
        "body", -1,
        (verts, normals, faces.reshape(-1)),
        kind["coat"], 0.18,
        colors=colors,
    )]
    face_parts(parts, hc, mz, hr, kind.get("nose", DARK))
    tail_parts(parts, kind["body"], kind["coat"])
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump({"parts": parts}, handle, separators=(",", ":"))
    print(f"{name:8} {len(verts):5} verts  {len(faces):5} tris")
    return verts, faces, colors


def preview(name, verts, faces, colors, path):
    eye = np.array([0.48, 0.50, 1.85])
    target = np.array([0.0, 0.36, 0.05])
    forward = target - eye
    forward /= np.linalg.norm(forward)
    right = np.cross(forward, np.array([0.0, 1.0, 0.0]))
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)
    rel = verts - eye
    cam = np.column_stack([rel @ right, rel @ up, rel @ forward])
    width, height = 320, 480
    aspect = width / height
    fov = math.tan(math.radians(28) / 2)
    sx = (cam[:, 0] / cam[:, 2]) / (fov * aspect)
    sy = (cam[:, 1] / cam[:, 2]) / fov
    px = (sx * 0.5 + 0.5) * (width - 1)
    py = (1 - (sy * 0.5 + 0.5)) * (height - 1)
    light = np.array([0.45, 0.85, 0.55])
    light /= np.linalg.norm(light)
    buf = np.zeros((height, width, 3), dtype=np.float32)
    buf[:] = (0.58, 0.74, 0.86)
    depth = np.full((height, width), -1e9, dtype=np.float32)
    normals = face_normals(verts, faces)
    for tri in faces:
        pts = np.stack([px[tri], py[tri], cam[tri, 2]], axis=1)
        if np.any(pts[:, 2] < 0.05):
            continue
        minx = max(0, int(pts[:, 0].min()))
        maxx = min(width - 1, int(pts[:, 0].max()) + 1)
        miny = max(0, int(pts[:, 1].min()))
        maxy = min(height - 1, int(pts[:, 1].max()) + 1)
        if minx >= maxx or miny >= maxy:
            continue
        area = (pts[1, 0] - pts[0, 0]) * (pts[2, 1] - pts[0, 1]) - (pts[2, 0] - pts[0, 0]) * (pts[1, 1] - pts[0, 1])
        if abs(area) < 1e-4:
            continue
        xs = np.arange(minx, maxx + 1)
        ys = np.arange(miny, maxy + 1)
        gx, gy = np.meshgrid(xs, ys)
        w0 = (pts[1, 0] - gx) * (pts[2, 1] - gy) - (pts[2, 0] - gx) * (pts[1, 1] - gy)
        w1 = (pts[2, 0] - gx) * (pts[0, 1] - gy) - (pts[0, 0] - gx) * (pts[2, 1] - gy)
        w2 = (pts[0, 0] - gx) * (pts[1, 1] - gy) - (pts[1, 0] - gx) * (pts[0, 1] - gy)
        if area < 0:
            w0, w1, w2 = -w0, -w1, -w2
            area = -area
        inside = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
        if not np.any(inside):
            continue
        z = (w0 * pts[0, 2] + w1 * pts[1, 2] + w2 * pts[2, 2]) / area
        better = inside & (z > depth[miny:maxy + 1, minx:maxx + 1])
        if not np.any(better):
            continue
        depth[miny:maxy + 1, minx:maxx + 1][better] = z[better]
        n = normals[tri].mean(axis=0)
        n /= np.linalg.norm(n) or 1
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(n, light)))
        rgb = colors[tri].mean(axis=0) * shade
        buf[miny:maxy + 1, minx:maxx + 1][better] = rgb
    image = (np.clip(buf, 0, 1) * 255).astype(np.uint8)
    with open(path, "wb") as handle:
        handle.write(f"P6\n{width} {height}\n255\n".encode())
        handle.write(image.tobytes())
    print("preview", name, path)


KINDS = {
    "pug": dict(body=0.82, head=1.28, snout=0.055, leg=0.82, ear="fold", coat=(0.86, 0.66, 0.40), belly=(0.93, 0.78, 0.58), ear_color=(0.55, 0.36, 0.24), mask=(0.32, 0.22, 0.16), nose=DARK),
    "pamuk": dict(body=0.78, head=1.12, snout=0.07, leg=0.72, ear="flop", coat=(0.96, 0.94, 0.90), belly=(0.98, 0.90, 0.88), ear_color=(0.90, 0.78, 0.76), nose=PINK),
    "boncuk": dict(body=0.70, head=1.05, snout=0.06, leg=0.62, ear="point", coat=(0.95, 0.68, 0.28), belly=(0.98, 0.86, 0.55), ear_color=(0.62, 0.36, 0.16), nose=DARK),
    "tombul": dict(body=1.15, head=1.05, snout=0.09, leg=0.70, ear="flop", coat=(0.55, 0.34, 0.20), belly=(0.82, 0.62, 0.42), ear_color=(0.36, 0.20, 0.12), nose=DARK),
    "bebek": dict(body=0.72, head=1.18, snout=0.05, leg=0.60, ear="flop", coat=(0.98, 0.84, 0.80), belly=(0.99, 0.93, 0.90), ear_color=(0.96, 0.70, 0.72), nose=PINK),
    "kalp": dict(body=0.92, head=1.02, snout=0.08, leg=0.95, ear="point", coat=(0.72, 0.28, 0.24), belly=(0.93, 0.62, 0.55), ear_color=(0.48, 0.14, 0.16), nose=DARK),
    "somun": dict(body=1.0, head=1.0, snout=0.09, leg=0.85, ear="flop", coat=(0.83, 0.58, 0.30), belly=(0.95, 0.86, 0.62), ear_color=(0.55, 0.32, 0.16), nose=DARK),
    "kahkaha": dict(body=1.05, head=1.0, snout=0.10, leg=1.0, ear="point", coat=(0.16, 0.13, 0.12), belly=(0.55, 0.40, 0.28), ear_color=(0.08, 0.07, 0.07), nose=DARK),
    "goz": dict(body=0.95, head=1.02, snout=0.08, leg=0.95, ear="point", coat=(0.94, 0.92, 0.88), belly=(0.97, 0.95, 0.90), ear_color=(0.72, 0.68, 0.64), nose=DARK),
    "ceket": dict(body=0.95, head=1.0, snout=0.08, leg=0.9, ear="fold", coat=(0.93, 0.90, 0.84), belly=(0.55, 0.62, 0.78), ear_color=(0.70, 0.66, 0.60), nose=DARK),
    "kangal": dict(body=1.18, head=0.98, snout=0.15, leg=1.12, ear="point", coat=(0.72, 0.48, 0.24), belly=(0.86, 0.66, 0.40), ear_color=(0.42, 0.26, 0.14), mask=(0.22, 0.14, 0.10), nose=DARK),
}


if __name__ == "__main__":
    root = os.path.join(os.path.dirname(__file__), "..", "dist")
    os.makedirs(root, exist_ok=True)
    show = {"pug", "kangal", "pamuk", "boncuk"}
    for name, kind in KINDS.items():
        verts, faces, colors = build_one(name, kind)
        if name in show:
            preview(name, verts, faces, colors, os.path.join(root, name + "_dog.ppm"))
