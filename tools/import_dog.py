#!/usr/bin/env python3
"""Turn Quaternius' CC0 dog into the pet meshes.

The previous dogs were a single smooth blob, so they read as clay. This keeps
a real dog silhouette, softens the low-poly facets, and paints each coat.
Source mesh: tools/models/Dog.glb (Quaternius, CC0).
"""

import json
import math
import os
import struct

import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..")
GLB = os.path.join(ROOT, "tools", "models", "Dog.glb")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "dogs")

WHITE = (0.98, 0.97, 0.95)
DARK = (0.10, 0.07, 0.06)
PINK = (0.95, 0.55, 0.62)
TONGUE = (0.90, 0.42, 0.48)
TAIL_JOINTS = {14, 15, 16, 17}

# Same coats as the old roster, drawn on one real dog.
KINDS = {
    "pug": dict(scale=0.96, wide=1.08, coat=(0.86, 0.66, 0.40), belly=(0.96, 0.86, 0.72), ear=(0.45, 0.28, 0.18), mask=(0.28, 0.18, 0.13), nose=DARK),
    "pamuk": dict(scale=0.90, wide=1.0, coat=(0.97, 0.95, 0.92), belly=(0.99, 0.93, 0.90), ear=(0.90, 0.78, 0.76), nose=PINK),
    "boncuk": dict(scale=0.84, wide=1.0, coat=(0.95, 0.68, 0.28), belly=(0.98, 0.90, 0.62), ear=(0.55, 0.32, 0.14), nose=DARK),
    "tombul": dict(scale=1.02, wide=1.16, coat=(0.55, 0.34, 0.20), belly=(0.86, 0.66, 0.46), ear=(0.32, 0.18, 0.11), nose=DARK),
    "bebek": dict(scale=0.78, wide=1.04, coat=(0.98, 0.84, 0.80), belly=(0.99, 0.94, 0.92), ear=(0.96, 0.70, 0.72), nose=PINK),
    "kalp": dict(scale=0.98, wide=1.0, coat=(0.74, 0.28, 0.24), belly=(0.95, 0.66, 0.58), ear=(0.46, 0.14, 0.16), nose=DARK),
    "somun": dict(scale=1.02, wide=1.06, coat=(0.83, 0.58, 0.30), belly=(0.96, 0.88, 0.66), ear=(0.50, 0.30, 0.15), nose=DARK),
    "kahkaha": dict(scale=1.04, wide=1.0, coat=(0.16, 0.13, 0.12), belly=(0.62, 0.48, 0.36), ear=(0.08, 0.07, 0.07), nose=DARK),
    "goz": dict(scale=1.0, wide=1.0, coat=(0.94, 0.92, 0.88), belly=(0.98, 0.96, 0.93), ear=(0.62, 0.58, 0.54), nose=DARK),
    "ceket": dict(scale=1.0, wide=1.0, coat=(0.93, 0.90, 0.84), belly=(0.55, 0.64, 0.80), ear=(0.55, 0.50, 0.46), nose=DARK),
    "kangal": dict(scale=1.16, wide=1.0, coat=(0.74, 0.50, 0.26), belly=(0.90, 0.72, 0.46), ear=(0.40, 0.24, 0.13), mask=(0.22, 0.14, 0.10), nose=DARK),
}


def load_glb(path):
    data = open(path, "rb").read()
    off = 12
    chunks = []
    while off < len(data):
        clen, ctype = struct.unpack_from("<II", data, off)
        chunks.append(data[off + 8:off + 8 + clen])
        off += 8 + clen
    js = json.loads(chunks[0])
    blob = chunks[1]

    def accessor(index):
        acc = js["accessors"][index]
        view = js["bufferViews"][acc["bufferView"]]
        start = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
        ncomp = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}[acc["type"]]
        fmt = {5126: "f", 5123: "H", 5121: "B", 5125: "I"}[acc["componentType"]]
        size = struct.calcsize("<" + fmt)
        stride = view.get("byteStride", size * ncomp)
        rows = [
            struct.unpack_from("<" + fmt * ncomp, blob, start + i * stride)
            for i in range(acc["count"])
        ]
        dtype = np.float64 if acc["componentType"] == 5126 else np.int32
        return np.asarray(rows, dtype=dtype)

    prim = js["meshes"][0]["primitives"][0]
    attrs = prim["attributes"]
    pos = accessor(attrs["POSITION"])
    joints = accessor(attrs["JOINTS_0"]).astype(np.int32)
    weights = accessor(attrs["WEIGHTS_0"])
    faces = accessor(prim["indices"]).reshape(-1, 3)
    dominant = joints[np.arange(len(joints)), np.argmax(weights, axis=1)]
    return pos, faces, np.isin(dominant, list(TAIL_JOINTS))


def weld(pos, faces, tail):
    keys = np.round(pos, 5)
    mapping = {}
    remap = np.empty(len(pos), dtype=np.int32)
    fresh = []
    votes = []
    counts = []
    for i, key in enumerate(map(tuple, keys)):
        slot = mapping.get(key)
        if slot is None:
            slot = len(fresh)
            mapping[key] = slot
            fresh.append(pos[i])
            votes.append(1 if tail[i] else 0)
            counts.append(1)
        else:
            votes[slot] += 1 if tail[i] else 0
            counts[slot] += 1
        remap[i] = slot
    faces = remap[faces]
    keep = (faces[:, 0] != faces[:, 1]) & (faces[:, 1] != faces[:, 2]) & (faces[:, 2] != faces[:, 0])
    labels = np.array([votes[i] * 2 >= counts[i] for i in range(len(fresh))])
    return np.asarray(fresh, dtype=np.float64), faces[keep], labels


def loop_subdivide(verts, faces, tail):
    edge_faces = {}
    for fi, tri in enumerate(faces):
        a, b, c = int(tri[0]), int(tri[1]), int(tri[2])
        for u, v in ((a, b), (b, c), (c, a)):
            edge_faces.setdefault((u, v) if u < v else (v, u), []).append(fi)
    edge_id = {}
    edge_pos = []
    edge_tail = []
    n = len(verts)
    for (u, v), owners in edge_faces.items():
        if len(owners) == 2:
            opps = []
            for fi in owners:
                opps.append(next(int(x) for x in faces[fi] if int(x) != u and int(x) != v))
            point = 0.375 * (verts[u] + verts[v]) + 0.125 * (verts[opps[0]] + verts[opps[1]])
        else:
            point = 0.5 * (verts[u] + verts[v])
        edge_id[(u, v)] = n + len(edge_pos)
        edge_pos.append(point)
        edge_tail.append(bool(tail[u] and tail[v]))
    neighbors = [[] for _ in range(n)]
    for u, v in edge_faces:
        neighbors[u].append(v)
        neighbors[v].append(u)
    updated = verts.copy()
    for i, nbrs in enumerate(neighbors):
        k = len(nbrs)
        if k < 3:
            continue
        beta = 3.0 / (8.0 * k) if k > 3 else 3.0 / 16.0
        updated[i] = (1.0 - k * beta) * verts[i] + beta * verts[nbrs].sum(axis=0)
    new_faces = []
    for a, b, c in faces:
        a, b, c = int(a), int(b), int(c)
        ab = edge_id[(a, b) if a < b else (b, a)]
        bc = edge_id[(b, c) if b < c else (c, b)]
        ca = edge_id[(c, a) if c < a else (a, c)]
        new_faces.extend(((a, ab, ca), (b, bc, ab), (c, ca, bc), (ab, bc, ca)))
    out_v = np.vstack([updated, np.asarray(edge_pos)])
    out_t = np.concatenate([tail, np.asarray(edge_tail, dtype=bool)])
    return out_v, np.asarray(new_faces, dtype=np.int32), out_t


def face_normals(verts, faces):
    a, b, c = verts[faces[:, 0]], verts[faces[:, 1]], verts[faces[:, 2]]
    n = np.cross(b - a, c - a)
    acc = np.zeros_like(verts)
    for k in range(3):
        np.add.at(acc, faces[:, k], n)
    length = np.linalg.norm(acc, axis=1)
    length[length < 1e-8] = 1.0
    return acc / length[:, None]


def relax(verts, faces, rounds=3, blend=0.28):
    edges = np.vstack([faces[:, [0, 1]], faces[:, [1, 2]], faces[:, [2, 0]]])
    acc = np.zeros_like(verts)
    cnt = np.zeros(len(verts))
    for _ in range(rounds):
        acc[:] = 0
        cnt[:] = 0
        np.add.at(acc, edges[:, 0], verts[edges[:, 1]])
        np.add.at(cnt, edges[:, 0], 1)
        avg = acc / np.maximum(cnt, 1)[:, None]
        verts = verts * (1 - blend) + avg * blend
    return verts


def orient(pos, faces):
    # -90 degrees around Y: head (+X) turns toward the camera (+Z).
    turned = np.column_stack([-pos[:, 2], pos[:, 1], pos[:, 0]])
    turned = relax(turned, faces)
    turned[:, 1] -= turned[:, 1].min()
    turned[:, 0] -= np.median(turned[:, 0])
    turned[:, 2] -= np.median(turned[:, 2])
    height = float(turned[:, 1].max()) or 1.0
    turned *= 0.70 / height
    turned[:, 1] += 0.02
    return turned


def paint(verts, normals, kind):
    coat = np.array(kind["coat"])
    belly = np.array(kind["belly"])
    ear = np.array(kind["ear"])
    cols = np.tile(coat, (len(verts), 1))
    span_z = float(np.ptp(verts[:, 2])) + 1e-6
    zmax = float(verts[:, 2].max())
    yn = (verts[:, 1] - verts[:, 1].min()) / (np.ptp(verts[:, 1]) + 1e-6)
    x = np.abs(verts[:, 0] - np.median(verts[:, 0]))
    under = np.clip(-normals[:, 1] * 1.6, 0, 1)
    belly_w = np.clip((0.34 - yn) / 0.22, 0, 1) * under
    chest = np.clip((yn - 0.16) / 0.12, 0, 1) * np.clip((0.52 - yn) / 0.16, 0, 1)
    chest *= np.clip((verts[:, 2] - (zmax - 0.42 * span_z)) / (0.16 * span_z), 0, 1)
    chest *= np.clip(1 - x / (0.10 * span_z), 0, 1)
    paws = np.clip((0.10 - yn) / 0.06, 0, 1)
    w = np.clip(np.maximum(np.maximum(belly_w, chest), paws * 0.85), 0, 1)
    cols = cols * (1 - w[:, None]) + belly * w[:, None]
    ear_w = np.clip((yn - 0.78) / 0.12, 0, 1) * np.clip((x - np.quantile(x, 0.78)) / 0.04, 0, 1)
    ear_w *= np.clip(1 - (verts[:, 2] - (zmax - 0.12 * span_z)) / (0.12 * span_z), 0, 1)
    cols = cols * (1 - ear_w[:, None]) + ear * ear_w[:, None]
    front = np.clip((verts[:, 2] - (zmax - 0.07 * span_z)) / (0.05 * span_z), 0, 1)
    front *= np.clip((yn - 0.55) / 0.2, 0, 1)
    if kind.get("mask"):
        mask = np.array(kind["mask"])
        cols = cols * (1 - front[:, None] * 0.92) + mask * front[:, None] * 0.92
    else:
        cols = cols * (1 - front[:, None] * 0.55) + belly * front[:, None] * 0.55
    cols *= (0.94 + np.clip(normals[:, 1], 0, 1) * 0.08)[:, None]
    return np.clip(cols, 0, 1)


def compact(verts, normals, colors, faces):
    used = np.unique(faces.reshape(-1))
    remap = np.full(len(verts), -1, dtype=np.int32)
    remap[used] = np.arange(len(used))
    return verts[used], normals[used], colors[used], remap[faces]


def ellipsoid():
    stacks, slices = 8, 10
    verts, norms = [], []
    for i in range(stacks + 1):
        phi = i / stacks * math.pi
        for j in range(slices + 1):
            theta = j / slices * math.tau
            nx = math.sin(phi) * math.cos(theta)
            ny = math.cos(phi)
            nz = math.sin(phi) * math.sin(theta)
            verts.append((nx, ny, nz))
            norms.append((nx, ny, nz))
    idx = []
    for i in range(stacks):
        for j in range(slices):
            a = i * (slices + 1) + j
            b = a + slices + 1
            idx += [a, b, a + 1, b, b + 1, a + 1]
    return verts, norms, idx


EYE = ellipsoid()


def pack(name, parent, verts, normals, indices, color, spec, t=(0, 0, 0), s=(1, 1, 1), colors=None):
    part = {
        "name": name,
        "parent": parent,
        "t": [round(float(v), 4) for v in t],
        "r": [0, 0, 0],
        "s": [round(float(v), 4) for v in s],
        "color": [round(float(v), 4) for v in color],
        "spec": spec,
        "positions": [round(float(n), 4) for v in verts for n in v],
        "normals": [round(float(n), 4) for v in normals for n in v],
        "indices": [int(i) for i in np.asarray(indices).reshape(-1)],
    }
    if colors is not None:
        part["colors"] = [round(float(n), 3) for row in colors for n in row]
    return part


def face_anchors(verts):
    """Eyes sit on either side of the muzzle, just behind the nose."""
    tip = verts[int(np.argmax(verts[:, 2]))]
    height = float(np.ptp(verts[:, 1])) + 1e-6
    back = 0.045 * height
    eye_y = float(tip[1] - 0.002)
    band = verts[(np.abs(verts[:, 2] - (tip[2] - back)) < 0.015) & (np.abs(verts[:, 1] - eye_y) < 0.03)]
    half = float(np.quantile(np.abs(band[:, 0]), 0.8)) if len(band) else 0.04
    eye_x = max(half * 1.05, 0.034)

    def surface(x, y):
        dist = (verts[:, 0] - x) ** 2 + (verts[:, 1] - y) ** 2
        near = dist < 0.02 ** 2
        if np.any(near):
            return float(verts[near, 2].max())
        return float(tip[2] - back)

    z = min(surface(eye_x, eye_y) + 0.003, float(tip[2]) - 0.008)
    left = np.array((-eye_x, eye_y, z))
    right = np.array((eye_x, eye_y, z))
    nose = np.array((float(tip[0]), float(tip[1] - 0.004), float(tip[2] + 0.005)))
    return left, right, nose


def darken_sockets(verts, colors, left, right):
    for eye in (left, right):
        d = np.linalg.norm(verts - eye, axis=1)
        w = np.clip(1.0 - d / 0.026, 0, 1) ** 2 * 0.28
        colors *= (1 - w)[:, None]
    return np.clip(colors, 0, 1)


def build_base():
    pos, faces, tail = load_glb(GLB)
    pos, faces, tail = weld(pos, faces, tail)
    for _ in range(2):
        pos, faces, tail = loop_subdivide(pos, faces, tail)
    pos = orient(pos, faces)
    normals = face_normals(pos, faces)
    center = pos.mean(axis=0)
    if np.mean(np.sum(normals * (pos - center), axis=1)) < 0:
        faces = faces[:, ::-1]
        normals = -normals
    print(f"base {len(pos)} verts {len(faces)} tris  bounds {pos.min(0).round(3)} {pos.max(0).round(3)}")
    return pos, faces, normals, tail


def breed_mesh(base_pos, faces, base_normals, tail, kind):
    pos = base_pos.copy()
    pos[:, 0] *= kind["wide"]
    pos[:, 2] *= kind["wide"]
    pos *= kind["scale"]
    pos[:, 1] -= pos[:, 1].min()
    pos[:, 1] += 0.02
    normals = base_normals.copy()
    # Non-uniform scale bends normals; rebuild them.
    normals = face_normals(pos, faces)
    colors = paint(pos, normals, kind)
    tail_faces = faces[tail[faces].all(axis=1)]
    body_faces = faces[~tail[faces].all(axis=1)]
    shared = np.intersect1d(tail_faces.reshape(-1), body_faces.reshape(-1)) if len(tail_faces) else np.array([], dtype=np.int32)
    pivot = pos[shared].mean(axis=0) if len(shared) else np.zeros(3)
    bv, bn, bc, bf = compact(pos, normals, colors, body_faces)
    left, right, nose = face_anchors(bv)
    bc = darken_sockets(bv, bc, left, right)
    parts = [pack("body", -1, bv, bn, bf, kind["coat"], 0.16, colors=bc)]
    if len(tail_faces):
        tv, tn, tc, tf = compact(pos, normals, colors, tail_faces)
        parts.append(pack("tail", 0, tv - pivot, tn, tf, kind["coat"], 0.16, t=tuple(pivot), colors=tc))
    # Eyes stay on the skull. A hidden head pivot would swing them off the face.
    eye_r = max(0.012, min(0.020 * kind["scale"], abs(right[0] - left[0]) * 0.24))
    for side, eye in (("L", left), ("R", right)):
        pupil = eye + np.array((0.0, -0.001, eye_r * 0.55))
        spark = eye + np.array(((0.32 if side == "R" else -0.32) * eye_r, 0.38 * eye_r, eye_r * 0.62))
        parts.append(pack(f"eye{side}", 0, EYE[0], EYE[1], EYE[2], WHITE, 0.95, t=tuple(eye), s=(eye_r, eye_r * 1.08, eye_r * 0.78)))
        parts.append(pack(f"pupil{side}", 0, EYE[0], EYE[1], EYE[2], DARK, 0.9, t=tuple(pupil), s=(eye_r * 0.48, eye_r * 0.56, eye_r * 0.32)))
        parts.append(pack(f"spark{side}", 0, EYE[0], EYE[1], EYE[2], WHITE, 1.0, t=tuple(spark), s=(eye_r * 0.16, eye_r * 0.16, eye_r * 0.1)))
    parts.append(pack("nose", 0, EYE[0], EYE[1], EYE[2], kind["nose"], 0.85, t=tuple(nose), s=(0.018 * kind["scale"], 0.013 * kind["scale"], 0.012 * kind["scale"])))
    mouth = nose + np.array((0.0, -0.020 * kind["scale"], -0.002))
    parts.append(pack("mouth", 0, EYE[0], EYE[1], EYE[2], (0.28, 0.10, 0.11), 0.15, t=tuple(mouth), s=(0.020 * kind["scale"], 0.006 * kind["scale"], 0.010 * kind["scale"])))
    tongue = mouth + np.array((0.0, -0.006 * kind["scale"], 0.008 * kind["scale"]))
    parts.append(pack("tongue", 0, EYE[0], EYE[1], EYE[2], TONGUE, 0.35, t=tuple(tongue), s=(0.016 * kind["scale"], 0.006 * kind["scale"], 0.014 * kind["scale"])))
    return parts, (left, right, nose)


def save(name, parts):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump({"parts": parts}, handle, separators=(",", ":"))
    size = os.path.getsize(path)
    verts = len(parts[0]["positions"]) // 3
    print(f"{name:8} body {verts:5} verts  file {size/1024:.0f} KB")


def preview(parts, path):
    eye = np.array([0.48, 0.52, 1.85])
    target = np.array([0.0, 0.34, 0.02])
    forward = target - eye
    forward /= np.linalg.norm(forward)
    right = np.cross(forward, np.array([0.0, 1.0, 0.0]))
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)
    width, height = 360, 640
    aspect = width / height
    fov = math.tan(math.radians(32) / 2)
    light = np.array([0.35, 0.92, 0.75])
    light /= np.linalg.norm(light)
    buf = np.full((height, width, 3), (0.58, 0.74, 0.86), dtype=np.float32)
    depth = np.full((height, width), 1e9, dtype=np.float32)

    def blit(verts, faces, colors):
        rel = verts - eye
        cam = np.column_stack([rel @ right, rel @ up, rel @ forward])
        sx = (cam[:, 0] / np.maximum(cam[:, 2], 1e-4)) / (fov * aspect)
        sy = (cam[:, 1] / np.maximum(cam[:, 2], 1e-4)) / fov
        px = (sx * 0.5 + 0.5) * (width - 1)
        py = (1 - (sy * 0.5 + 0.5)) * (height - 1)
        normals = face_normals(verts, faces)
        for tri in faces:
            if np.any(cam[tri, 2] < 0.05):
                continue
            pts = np.stack([px[tri], py[tri], cam[tri, 2]], axis=1)
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
            better = inside & (z < depth[miny:maxy + 1, minx:maxx + 1]) & (z > 0.05)
            if not np.any(better):
                continue
            depth[miny:maxy + 1, minx:maxx + 1][better] = z[better]
            n = normals[tri].mean(axis=0)
            n = n / (np.linalg.norm(n) or 1)
            shade = 0.58 + 0.42 * max(0.0, float(np.dot(n, light)))
            rgb = colors[tri].mean(axis=0) * shade
            buf[miny:maxy + 1, minx:maxx + 1][better] = rgb

    for part in parts:
        pos = np.asarray(part["positions"], dtype=np.float64).reshape(-1, 3)
        scale = np.asarray(part["s"], dtype=np.float64)
        pos = pos * scale + np.asarray(part["t"], dtype=np.float64)
        faces = np.asarray(part["indices"], dtype=np.int32).reshape(-1, 3)
        if "colors" in part:
            colors = np.asarray(part["colors"], dtype=np.float64).reshape(-1, 3)
        else:
            colors = np.tile(np.asarray(part["color"], dtype=np.float64), (len(pos), 1))
        blit(pos, faces, colors)
    image = (np.clip(buf, 0, 1) * 255).astype(np.uint8)
    with open(path, "wb") as handle:
        handle.write(f"P6\n{width} {height}\n255\n".encode())
        handle.write(image.tobytes())
    print("preview", path)


def main():
    base_pos, faces, normals, tail = build_base()
    show = {"pug", "kangal", "pamuk", "boncuk"}
    os.makedirs("/tmp/dogmodel", exist_ok=True)
    for name, kind in KINDS.items():
        parts, anchors = breed_mesh(base_pos, faces, normals, tail, kind)
        save(name, parts)
        if name in show:
            print(name, "anchors", [a.round(3) for a in anchors])
            preview(parts, f"/tmp/dogmodel/fit_{name}.ppm")


if __name__ == "__main__":
    main()
