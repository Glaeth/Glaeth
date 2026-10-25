#!/usr/bin/env python3
"""Bake Omabuarts' CC-BY toon cat with the same skinning path as the dog.

Source GLB: the Sketchfab file "Toon Cat FREE" redistributed with credit at
https://github.com/vivganes/brownfield-code-surgeon (packages/ui/public/models).
License: CC BY 4.0. Author: Omabuarts Studio.
"""

import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import import_textured_dog as imp

ROOT = os.path.join(os.path.dirname(__file__), "..")
GLB = os.path.join(ROOT, "tools", "models", "toon_cat_free.glb")
OUT_JSON = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "cat.json")
OUT_PNG = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "cat.png")
OUT_ANIM = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "cat.anim")
PREVIEW = "/tmp/pets"


def duration_of(js, accessor):
    animation = js["animations"][0]
    longest = 0.0
    for channel in animation["channels"]:
        sampler = animation["samplers"][channel["sampler"]]
        times = accessor(sampler["input"]).reshape(-1)
        longest = max(longest, float(times[-1]))
    return longest


def parents_ok(js):
    parents = {child: index for index, node in enumerate(js["nodes"]) for child in node.get("children", [])}
    for child, parent in parents.items():
        if parent >= child:
            raise SystemExit(f"parent {parent} is not before child {child}")


def raster(positions, normals, uvs, faces, image, flip_v, path):
    h, w = 720, 480
    eye = np.array([0.22, 0.64, 1.62])
    target = np.array([0.0, 0.38, 0.02])
    up = np.array([0.0, 1.0, 0.0])
    back = eye - target
    back = back / np.linalg.norm(back)
    right = np.cross(up, back)
    right = right / np.linalg.norm(right)
    cam_up = np.cross(back, right)
    rel = positions - eye
    cx = rel @ right
    cy = rel @ cam_up
    cz = rel @ back
    fov = np.deg2rad(32)
    focal = 1.0 / np.tan(fov / 2)
    aspect = w / h
    ndc_x = (cx / -cz) * focal / aspect
    ndc_y = (cy / -cz) * focal
    sx = (ndc_x * 0.5 + 0.5) * w
    sy = (1.0 - (ndc_y * 0.5 + 0.5)) * h
    light = np.array([0.22, 0.88, 0.55])
    light = light / np.linalg.norm(light)
    color = np.zeros((h, w, 3), np.float32)
    depth = np.full((h, w), -1e9, np.float32)
    tex = np.asarray(image.convert("RGB"), dtype=np.float32) / 255.0
    th, tw = tex.shape[:2]
    faces = faces.reshape(-1, 3)
    for tri in faces:
        z = cz[tri]
        if np.all(z >= -0.05):
            continue
        pts = np.stack([sx[tri], sy[tri]], axis=1)
        minx = max(int(np.floor(pts[:, 0].min())), 0)
        maxx = min(int(np.ceil(pts[:, 0].max())), w - 1)
        miny = max(int(np.floor(pts[:, 1].min())), 0)
        maxy = min(int(np.ceil(pts[:, 1].max())), h - 1)
        if minx > maxx or miny > maxy:
            continue
        area = (pts[1, 0] - pts[0, 0]) * (pts[2, 1] - pts[0, 1]) - (pts[2, 0] - pts[0, 0]) * (pts[1, 1] - pts[0, 1])
        if abs(area) < 1e-6:
            continue
        ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
        v0x, v0y = pts[0]
        w1 = ((xs - v0x) * (pts[2, 1] - v0y) - (pts[2, 0] - v0x) * (ys - v0y)) / area
        w2 = ((pts[1, 0] - v0x) * (ys - v0y) - (xs - v0x) * (pts[1, 1] - v0y)) / area
        w0 = 1 - w1 - w2
        mask = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
        if not np.any(mask):
            continue
        zz = w0 * z[0] + w1 * z[1] + w2 * z[2]
        better = mask & (zz > depth[miny:maxy + 1, minx:maxx + 1])
        if not np.any(better):
            continue
        uv = w0[..., None] * uvs[tri[0]] + w1[..., None] * uvs[tri[1]] + w2[..., None] * uvs[tri[2]]
        uu = np.clip(uv[..., 0], 0, 1)
        vv = np.clip(uv[..., 1], 0, 1)
        if flip_v:
            vv = 1 - vv
        tx = np.clip((uu * (tw - 1)).astype(np.int32), 0, tw - 1)
        ty = np.clip((vv * (th - 1)).astype(np.int32), 0, th - 1)
        nor = w0[..., None] * normals[tri[0]] + w1[..., None] * normals[tri[1]] + w2[..., None] * normals[tri[2]]
        length = np.linalg.norm(nor, axis=-1, keepdims=True)
        length[length == 0] = 1
        nor = nor / length
        shade = np.clip(np.sum(nor * light, axis=-1), 0, 1) * 0.45 + 0.55
        rgb = tex[ty, tx] * shade[..., None]
        view = depth[miny:maxy + 1, minx:maxx + 1]
        paint = color[miny:maxy + 1, minx:maxx + 1]
        view[better] = zz[better]
        paint[better] = rgb[better]
    Image.fromarray(np.clip(color * 255, 0, 255).astype(np.uint8)).save(path)


def main():
    imp.GLB = GLB
    js, accessor = imp.load_glb(GLB)
    parents_ok(js)
    dur = duration_of(js, accessor)
    print("duration", round(dur, 4), "nodes", len(js["nodes"]))
    os.makedirs(PREVIEW, exist_ok=True)
    jpeg = imp.png_bytes(js)
    image = Image.open(__import__("io").BytesIO(jpeg))
    print("texture", image.size, image.mode)
    image.save(os.path.join(PREVIEW, "cat_tex.png"))
    posed0, nor0, uv0, faces, _ = imp.skin(js, accessor, 0.0)
    center, scale = imp.fit_from_pose(posed0)
    lowest = 0.02
    for time in np.linspace(0, dur, 12):
        posed, *_ = imp.skin(js, accessor, float(time))
        fitted = imp.fit_frame(posed, center, scale, 0.0)
        lowest = min(lowest, float(fitted[:, 1].min()))
    lift = 0.0 if lowest >= 0.02 else 0.02 - lowest
    positions = imp.fit_frame(posed0, center, scale, lift)
    mean = positions.mean(axis=0)
    if np.mean(np.sum(nor0 * (positions - mean), axis=1)) < 0:
        nor0 = -nor0
        faces = faces.reshape(-1, 3)[:, ::-1].reshape(-1)
    print("bounds", positions.min(0).round(3), positions.max(0).round(3), "lift", round(lift, 3), "scale", round(scale, 5))
    raster(positions, nor0, uv0, faces, image, False, os.path.join(PREVIEW, "cat_t0_noflip.png"))
    raster(positions, nor0, uv0, faces, image, True, os.path.join(PREVIEW, "cat_t0_flip.png"))
    mid, nmid, uvmid, fmid, _ = imp.skin(js, accessor, dur * 0.5)
    mid = imp.fit_frame(mid, center, scale, lift)
    raster(mid, nmid, uvmid, fmid, image, False, os.path.join(PREVIEW, "cat_mid_noflip.png"))
    if "--bake" not in sys.argv:
        return
    # Shader keeps v as-is and GL v=0 is the bitmap's top row. A standard
    # glTF coat (v=0 at the bottom) needs the PNG flipped, not the UVs.
    flip = "--flip" in sys.argv
    if flip:
        image = image.transpose(Image.FLIP_TOP_BOTTOM)
    part = {
        "name": "body",
        "parent": -1,
        "t": [0, 0, 0],
        "r": [0, 180, 0],
        "s": [1, 1, 1],
        "color": [1, 1, 1],
        "spec": 0.08,
        "texture": "dogs/cat.png",
        "positions": [round(float(v), 4) for v in positions.reshape(-1)],
        "normals": [round(float(v), 4) for v in nor0.reshape(-1)],
        "uv": [round(float(v), 5) for v in uv0.reshape(-1)],
        "indices": [int(v) for v in faces],
    }
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    import json
    with open(OUT_JSON, "w", encoding="utf-8") as handle:
        json.dump({"parts": [part]}, handle, separators=(",", ":"))
    image.save(OUT_PNG)
    # Fit center used by the clip is the pre-fit center. UVs in the clip are not stored;
    # the shader samples the png, so a vertical png flip must match uv 1-v baked above.
    clip_bytes, packed_duration = imp.pack_clip(js, accessor, center, scale, lift)
    open(OUT_ANIM, "wb").write(clip_bytes)
    clip = imp.read_clip(clip_bytes)
    worst = 0.0
    for time in (0.0, dur * 0.25, dur * 0.5, max(0.0, dur - 0.02)):
        posed, *_ = imp.skin(js, accessor, float(time))
        expected = imp.fit_frame(posed, center, scale, lift)
        played = imp.pose_clip(clip, float(time))
        error = float(np.max(np.abs(expected - played)))
        worst = max(worst, error)
        print(f"t={time:.3f} err {error:.6f} y {played[:,1].min():.3f}..{played[:,1].max():.3f}")
    print(
        f"cat {len(positions)} verts {len(faces)//3} tris "
        f"json {os.path.getsize(OUT_JSON)/1024:.0f}KB anim {len(clip_bytes)/1024:.0f}KB "
        f"dur {packed_duration:.3f} worst {worst:.6f} flip {flip}"
    )
    if worst > 1e-3:
        raise SystemExit("clip does not match")


if __name__ == "__main__":
    main()
