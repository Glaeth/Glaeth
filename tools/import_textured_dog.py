#!/usr/bin/env python3
"""Bake LasquetiSpice's CC-BY dog into the first pet.

Source: Sketchfab "Animated Dog Sits Rolls Over Shake Paw", CC BY 4.0.
The GLB used here is the same file SceneView redistributes with that credit:
https://github.com/sceneview/sceneview/raw/9c448f415f992b2093a2e2b8a6d77fc57838dede/samples/android-demo-assets/src/main/assets/models/animated_dog.glb
"""

import json
import os
import struct
import urllib.request

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
GLB = os.path.join(ROOT, "tools", "models", "animated_dog.glb")
GLB_URL = (
    "https://github.com/sceneview/sceneview/raw/"
    "9c448f415f992b2093a2e2b8a6d77fc57838dede/"
    "samples/android-demo-assets/src/main/assets/models/animated_dog.glb"
)
OUT_JSON = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "pug.json")
OUT_PNG = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "pug.png")


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
        ncomp = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[acc["type"]]
        dtype = {5126: np.float32, 5125: np.uint32, 5123: np.uint16, 5121: np.uint8}[acc["componentType"]]
        count = acc["count"]
        item = np.dtype(dtype).itemsize
        stride = view.get("byteStride", item * ncomp)
        if stride == item * ncomp:
            arr = np.frombuffer(blob, dtype=dtype, count=count * ncomp, offset=start)
            return arr.reshape(count, ncomp).astype(np.float64)
        raise RuntimeError("unexpected stride")

    prim = js["meshes"][0]["primitives"][0]
    attrs = prim["attributes"]
    pos = accessor(attrs["POSITION"])
    nor = accessor(attrs["NORMAL"])
    uv = accessor(attrs["TEXCOORD_0"])
    faces = accessor(prim["indices"]).astype(np.int64).reshape(-1)
    image = js["bufferViews"][js["images"][0]["bufferView"]]
    png = blob[image.get("byteOffset", 0):image.get("byteOffset", 0) + image["byteLength"]]
    return pos, nor, uv, faces, png


def fit(pos):
    # The skeleton node scales the mesh by 0.01. Length runs along Z, nose toward +Z.
    pos = pos * 0.01
    pos[:, 0] -= np.median(pos[:, 0])
    pos[:, 2] -= np.median(pos[:, 2])
    pos[:, 1] -= pos[:, 1].min()
    height = float(pos[:, 1].max()) or 1.0
    pos *= 0.68 / height
    pos[:, 1] += 0.02
    return pos


def main():
    if not os.path.exists(GLB):
        os.makedirs(os.path.dirname(GLB), exist_ok=True)
        urllib.request.urlretrieve(GLB_URL, GLB)
    pos, nor, uv, faces, png = load_glb(GLB)
    pos = fit(pos)
    # Outward normals. A flipped shell would light the inside of the coat.
    center = pos.mean(axis=0)
    if np.mean(np.sum(nor * (pos - center), axis=1)) < 0:
        nor = -nor
        faces = faces.reshape(-1, 3)[:, ::-1].reshape(-1)
    part = {
        "name": "body",
        "parent": -1,
        "t": [0, 0, 0],
        "r": [0, 0, 0],
        "s": [1, 1, 1],
        "color": [1, 1, 1],
        "spec": 0.08,
        "texture": "dogs/pug.png",
        "positions": [round(float(v), 4) for v in pos.reshape(-1)],
        "normals": [round(float(v), 4) for v in nor.reshape(-1)],
        "uv": [round(float(v), 5) for v in uv.reshape(-1)],
        "indices": [int(v) for v in faces],
    }
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, "w", encoding="utf-8") as handle:
        json.dump({"parts": [part]}, handle, separators=(",", ":"))
    open(OUT_PNG, "wb").write(png)
    print(
        f"pug {len(pos)} verts  {len(faces)//3} tris  "
        f"json {os.path.getsize(OUT_JSON)/1024:.0f} KB  "
        f"png {os.path.getsize(OUT_PNG)/1024:.0f} KB"
    )
    print("bounds", pos.min(0).round(3), pos.max(0).round(3))


if __name__ == "__main__":
    main()
