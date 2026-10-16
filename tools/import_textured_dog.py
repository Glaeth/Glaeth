#!/usr/bin/env python3
"""Bake LasquetiSpice's CC-BY dog into the first pet, in the sitting pose.

Source: Sketchfab "Animated Dog Sits Rolls Over Shake Paw", CC BY 4.0.
The GLB used here is the same file SceneView redistributes with that credit:
https://github.com/sceneview/sceneview/raw/9c448f415f992b2093a2e2b8a6d77fc57838dede/samples/android-demo-assets/src/main/assets/models/animated_dog.glb

The mesh is skinned. The raw positions are a bind pose whose coat only lines
up after the skeleton is posed. Frame 0 of the clip is the sitting, look-up
pose shown on the Sketchfab page.
"""

import json
import os
import struct
import urllib.request

import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..")
GLB = os.path.join(ROOT, "tools", "models", "animated_dog.glb")
GLB_URL = (
    "https://github.com/sceneview/sceneview/raw/"
    "9c448f415f992b2093a2e2b8a6d77fc57838dede/"
    "samples/android-demo-assets/src/main/assets/models/animated_dog.glb"
)
OUT_JSON = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "pug.json")
OUT_PNG = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "pug.png")
# Sitting, looking up. This is the pose on the model page.
POSE_TIME = 0.0


def load_glb(path):
    data = open(path, "rb").read()
    off = 12
    chunks = []
    while off < len(data):
        clen, _ctype = struct.unpack_from("<II", data, off)
        chunks.append(data[off + 8:off + 8 + clen])
        off += 8 + clen
    js = json.loads(chunks[0])
    blob = chunks[1]

    def accessor(index):
        acc = js["accessors"][index]
        view = js["bufferViews"][acc["bufferView"]]
        start = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
        ncomp = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}[acc["type"]]
        dtype = {5126: np.float32, 5125: np.uint32, 5123: np.uint16, 5121: np.uint8}[acc["componentType"]]
        count = acc["count"]
        item = np.dtype(dtype).itemsize
        stride = view.get("byteStride", item * ncomp)
        if stride != item * ncomp:
            raise RuntimeError(f"unexpected stride on accessor {index}")
        arr = np.frombuffer(blob, dtype=dtype, count=count * ncomp, offset=start).copy()
        return arr.reshape(count, ncomp).astype(np.float64)

    return js, accessor


def quat_matrix(q):
    x, y, z, w = q
    xx, yy, zz = x * x, y * y, z * z
    xy, xz, yz = x * y, x * z, y * z
    wx, wy, wz = w * x, w * y, w * z
    return np.array([
        [1 - 2 * (yy + zz), 2 * (xy - wz), 2 * (xz + wy), 0],
        [2 * (xy + wz), 1 - 2 * (xx + zz), 2 * (yz - wx), 0],
        [2 * (xz - wy), 2 * (yz + wx), 1 - 2 * (xx + yy), 0],
        [0, 0, 0, 1],
    ], dtype=np.float64)


def local_matrix(node, override):
    if "matrix" in node and not override:
        return np.array(node["matrix"], dtype=np.float64).reshape(4, 4).T
    translation = list(node.get("translation", [0, 0, 0]))
    rotation = list(node.get("rotation", [0, 0, 0, 1]))
    scale = list(node.get("scale", [1, 1, 1]))
    if override:
        translation = override.get("translation", translation)
        rotation = override.get("rotation", rotation)
        scale = override.get("scale", scale)
    translate = np.eye(4)
    translate[:3, 3] = translation
    scale_m = np.eye(4)
    scale_m[0, 0], scale_m[1, 1], scale_m[2, 2] = scale
    return translate @ quat_matrix(rotation) @ scale_m


def pose_overrides(js, accessor, time):
    overrides = {}
    animation = js["animations"][0]
    for channel in animation["channels"]:
        sampler = animation["samplers"][channel["sampler"]]
        times = accessor(sampler["input"]).reshape(-1)
        values = accessor(sampler["output"])
        path = channel["target"]["path"]
        if time <= times[0]:
            value = values[0]
        elif time >= times[-1]:
            value = values[-1]
        else:
            index = int(np.searchsorted(times, time) - 1)
            index = max(0, min(index, len(times) - 2))
            span = times[index + 1] - times[index]
            blend = 0.0 if span <= 0 else (time - times[index]) / span
            if path == "rotation":
                start, end = values[index], values[index + 1]
                if np.dot(start, end) < 0:
                    end = -end
                value = start * (1 - blend) + end * blend
                value = value / np.linalg.norm(value)
            else:
                value = values[index] * (1 - blend) + values[index + 1] * blend
        overrides.setdefault(channel["target"]["node"], {})[path] = np.asarray(value).tolist()
    return overrides


def world_matrices(js, overrides):
    nodes = js["nodes"]
    worlds = [None] * len(nodes)

    def walk(index, parent):
        worlds[index] = parent @ local_matrix(nodes[index], overrides.get(index))
        for child in nodes[index].get("children", []):
            walk(child, worlds[index])

    for root in js["scenes"][js["scene"]]["nodes"]:
        walk(root, np.eye(4))
    return worlds


def skin(js, accessor, time):
    overrides = pose_overrides(js, accessor, time)
    worlds = world_matrices(js, overrides)
    mesh_node = next(i for i, node in enumerate(js["nodes"]) if node.get("mesh") == 0)
    inverse_mesh = np.linalg.inv(worlds[mesh_node])
    skin_def = js["skins"][0]
    inverse_bind = accessor(skin_def["inverseBindMatrices"]).reshape(-1, 4, 4).transpose(0, 2, 1)
    joints = np.stack([
        inverse_mesh @ worlds[node] @ inverse_bind[joint]
        for joint, node in enumerate(skin_def["joints"])
    ])
    prim = js["meshes"][0]["primitives"][0]
    attrs = prim["attributes"]
    positions = accessor(attrs["POSITION"])
    normals = accessor(attrs["NORMAL"])
    uvs = accessor(attrs["TEXCOORD_0"])
    weights = accessor(attrs["WEIGHTS_0"])
    weights = weights / np.maximum(weights.sum(axis=1, keepdims=True), 1e-8)
    joint_index = accessor(attrs["JOINTS_0"]).astype(np.int32)
    faces = accessor(prim["indices"]).astype(np.int64).reshape(-1)
    png = png_bytes(js)
    homogeneous = np.concatenate([positions, np.ones((len(positions), 1))], axis=1)
    posed = np.zeros_like(positions)
    posed_normals = np.zeros_like(normals)
    for influence in range(4):
        matrices = joints[joint_index[:, influence]]
        weight = weights[:, influence, None]
        posed += np.einsum("nij,nj->ni", matrices, homogeneous)[:, :3] * weight
        posed_normals += np.einsum("nij,nj->ni", matrices[:, :3, :3], normals) * weight
    length = np.linalg.norm(posed_normals, axis=1, keepdims=True)
    length[length == 0] = 1
    posed_normals /= length
    return posed, posed_normals, uvs, faces, png


def png_bytes(js):
    # Called only after load_glb has parsed the file; reopen to slice the image.
    data = open(GLB, "rb").read()
    off = 12
    while off < len(data):
        clen, ctype = struct.unpack_from("<II", data, off)
        if ctype == 0x004E4942:  # BIN
            blob = data[off + 8:off + 8 + clen]
            image = js["bufferViews"][js["images"][0]["bufferView"]]
            start = image.get("byteOffset", 0)
            return blob[start:start + image["byteLength"]]
        off += 8 + clen
    raise RuntimeError("glb has no image")


def fit(positions):
    positions = positions.copy()
    positions[:, 0] -= np.median(positions[:, 0])
    positions[:, 2] -= np.median(positions[:, 2])
    positions[:, 1] -= positions[:, 1].min()
    height = float(positions[:, 1].max()) or 1.0
    positions *= 0.68 / height
    positions[:, 1] += 0.02
    return positions


def main():
    if not os.path.exists(GLB):
        os.makedirs(os.path.dirname(GLB), exist_ok=True)
        urllib.request.urlretrieve(GLB_URL, GLB)
    js, accessor = load_glb(GLB)
    positions, normals, uvs, faces, png = skin(js, accessor, POSE_TIME)
    positions = fit(positions)
    center = positions.mean(axis=0)
    if np.mean(np.sum(normals * (positions - center), axis=1)) < 0:
        normals = -normals
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
        "positions": [round(float(v), 4) for v in positions.reshape(-1)],
        "normals": [round(float(v), 4) for v in normals.reshape(-1)],
        "uv": [round(float(v), 5) for v in uvs.reshape(-1)],
        "indices": [int(v) for v in faces],
    }
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, "w", encoding="utf-8") as handle:
        json.dump({"parts": [part]}, handle, separators=(",", ":"))
    open(OUT_PNG, "wb").write(png)
    print(
        f"pug {len(positions)} verts  {len(faces) // 3} tris  "
        f"json {os.path.getsize(OUT_JSON) / 1024:.0f} KB  "
        f"png {os.path.getsize(OUT_PNG) / 1024:.0f} KB"
    )
    print("bounds", positions.min(0).round(3), positions.max(0).round(3))


if __name__ == "__main__":
    main()
