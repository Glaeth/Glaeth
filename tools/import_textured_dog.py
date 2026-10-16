#!/usr/bin/env python3
"""Bake LasquetiSpice's CC-BY dog and its looping clip.

Source: Sketchfab "Animated Dog Sits Rolls Over Shake Paw", CC BY 4.0.
The GLB used here is the same file SceneView redistributes with that credit:
https://github.com/sceneview/sceneview/raw/9c448f415f992b2093a2e2b8a6d77fc57838dede/samples/android-demo-assets/src/main/assets/models/animated_dog.glb

The mesh is skinned. pug.json keeps the sitting pose as a fallback. pug.anim
is the skeleton clip (sit, roll, shake) played in a loop by DogClip.
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
OUT_ANIM = os.path.join(ROOT, "app", "src", "main", "assets", "dogs", "pug.anim")
# Sitting, looking up. This is the pose on the model page, and the clip start.
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


def fit_frame(positions, center, scale, lift):
    posed = (positions - center) * scale
    posed[:, 1] += 0.02 + lift
    return posed


def fit_from_pose(positions):
    center = np.array([
        np.median(positions[:, 0]),
        positions[:, 1].min(),
        np.median(positions[:, 2]),
    ], dtype=np.float64)
    height = float((positions[:, 1] - center[1]).max()) or 1.0
    return center, 0.68 / height


def column_major(matrix):
    return np.asarray(matrix, dtype=np.float32).reshape(4, 4).T.reshape(-1)


def pack_clip(js, accessor, center, scale, lift):
    """Skeleton clip. See DogClip.kt for the little-endian layout."""
    nodes = js["nodes"]
    animation = js["animations"][0]
    animated = {channel["target"]["node"] for channel in animation["channels"]}
    mesh_node = next(i for i, node in enumerate(nodes) if node.get("mesh") == 0)
    worlds = world_matrices(js, {})
    inverse_mesh = np.linalg.inv(worlds[mesh_node])
    skin_def = js["skins"][0]
    inverse_bind = accessor(skin_def["inverseBindMatrices"]).reshape(-1, 4, 4)
    # Accessor floats are already column-major; keep that order in the file.
    prim = js["meshes"][0]["primitives"][0]
    attrs = prim["attributes"]
    positions = accessor(attrs["POSITION"]).astype(np.float32) * np.float32(scale)
    normals = accessor(attrs["NORMAL"]).astype(np.float32)
    weights = accessor(attrs["WEIGHTS_0"]).astype(np.float32)
    weight_sum = np.maximum(weights.sum(axis=1, keepdims=True), 1e-8)
    weights = weights / weight_sum
    joint_index = accessor(attrs["JOINTS_0"]).astype(np.uint16)
    duration = 0.0
    channels = []
    for channel in animation["channels"]:
        sampler = animation["samplers"][channel["sampler"]]
        if sampler.get("interpolation", "LINEAR") != "LINEAR":
            raise RuntimeError("only linear channels are packed")
        times = accessor(sampler["input"]).reshape(-1).astype(np.float32)
        values = accessor(sampler["output"]).astype(np.float32)
        path = {"translation": 0, "rotation": 1, "scale": 2}[channel["target"]["path"]]
        duration = max(duration, float(times[-1]))
        channels.append((channel["target"]["node"], path, times, values))
    fit_t = (-scale * center).astype(np.float64)
    fit_t[1] += 0.02 + lift
    blob = bytearray()
    blob += b"DOG1"
    blob += struct.pack(
        "<fIffff",
        np.float32(duration),
        len(nodes),
        np.float32(scale),
        np.float32(fit_t[0]),
        np.float32(fit_t[1]),
        np.float32(fit_t[2]),
    )
    parents = {child: index for index, node in enumerate(nodes) for child in node.get("children", [])}
    for index, node in enumerate(nodes):
        translation = node.get("translation", [0, 0, 0])
        rotation = node.get("rotation", [0, 0, 0, 1])
        node_scale = node.get("scale", [1, 1, 1])
        rest = local_matrix(node, None)
        blob += struct.pack(
            "<iB3f4f3f",
            parents.get(index, -1),
            0 if index in animated else 1,
            *np.asarray(translation, dtype=np.float32),
            *np.asarray(rotation, dtype=np.float32),
            *np.asarray(node_scale, dtype=np.float32),
        )
        blob += column_major(rest).tobytes()
    blob += struct.pack("<i", mesh_node)
    blob += column_major(inverse_mesh).tobytes()
    blob += struct.pack("<I", len(skin_def["joints"]))
    blob += np.asarray(skin_def["joints"], dtype=np.int32).tobytes()
    blob += inverse_bind.astype(np.float32).reshape(-1).tobytes()
    blob += struct.pack("<I", len(positions))
    blob += positions.astype(np.float32).reshape(-1).tobytes()
    blob += normals.astype(np.float32).reshape(-1).tobytes()
    blob += joint_index.reshape(-1).tobytes()
    blob += weights.astype(np.float32).reshape(-1).tobytes()
    blob += struct.pack("<I", len(channels))
    for node, path, times, values in channels:
        blob += struct.pack("<iBH", node, path, len(times))
        blob += times.astype(np.float32).tobytes()
        blob += values.astype(np.float32).reshape(-1).tobytes()
    return bytes(blob), float(duration)


def lowest_point(js, accessor, center, scale):
    lowest = 0.02
    for time in np.linspace(0, 35.7, 40):
        posed, _normals, _uvs, _faces, _png = skin(js, accessor, float(time))
        fitted = fit_frame(posed, center, scale, 0.0)
        lowest = min(lowest, float(fitted[:, 1].min()))
    return lowest


def read_clip(data):
    view = memoryview(data)
    off = 0

    def take(fmt):
        nonlocal off
        size = struct.calcsize(fmt)
        values = struct.unpack_from(fmt, view, off)
        off += size
        return values

    magic = bytes(take("<4s")[0])
    if magic != b"DOG1":
        raise RuntimeError(magic)
    duration, node_count = take("<fI")
    scale, fit_x, fit_y, fit_z = take("<4f")
    parents = np.empty(node_count, np.int32)
    use_rest = np.empty(node_count, np.uint8)
    rest_t = np.empty((node_count, 3), np.float32)
    rest_r = np.empty((node_count, 4), np.float32)
    rest_s = np.empty((node_count, 3), np.float32)
    rest_local = np.empty((node_count, 4, 4), np.float64)
    for index in range(node_count):
        parents[index], use_rest[index] = take("<iB")
        rest_t[index] = take("<3f")
        rest_r[index] = take("<4f")
        rest_s[index] = take("<3f")
        raw = np.array(take("<16f"), dtype=np.float64)
        rest_local[index] = raw.reshape(4, 4).T
    mesh_node = take("<i")[0]
    inv_mesh = np.array(take("<16f"), dtype=np.float64).reshape(4, 4).T
    joint_count = take("<I")[0]
    joint_nodes = np.array(take(f"<{joint_count}i"), dtype=np.int32)
    ibms = np.array(take(f"<{joint_count * 16}f"), dtype=np.float64).reshape(joint_count, 4, 4).transpose(0, 2, 1)
    vert_count = take("<I")[0]
    bind_pos = np.array(take(f"<{vert_count * 3}f"), dtype=np.float64).reshape(vert_count, 3)
    bind_nor = np.array(take(f"<{vert_count * 3}f"), dtype=np.float64).reshape(vert_count, 3)
    joints = np.array(take(f"<{vert_count * 4}H"), dtype=np.int32).reshape(vert_count, 4)
    weights = np.array(take(f"<{vert_count * 4}f"), dtype=np.float64).reshape(vert_count, 4)
    channel_count = take("<I")[0]
    channels = []
    for _ in range(channel_count):
        node, path, key_count = take("<iBH")
        times = np.array(take(f"<{key_count}f"), dtype=np.float64)
        comps = 4 if path == 1 else 3
        values = np.array(take(f"<{key_count * comps}f"), dtype=np.float64).reshape(key_count, comps)
        channels.append((node, path, times, values))
    if off != len(data):
        raise RuntimeError(f"trailer {len(data) - off}")
    return {
        "duration": duration,
        "scale": scale,
        "fit": np.array([fit_x, fit_y, fit_z]),
        "parents": parents,
        "use_rest": use_rest,
        "rest_t": rest_t,
        "rest_r": rest_r,
        "rest_s": rest_s,
        "rest_local": rest_local,
        "mesh_node": mesh_node,
        "inv_mesh": inv_mesh,
        "joint_nodes": joint_nodes,
        "ibms": ibms,
        "bind_pos": bind_pos,
        "bind_nor": bind_nor,
        "joints": joints,
        "weights": weights,
        "channels": channels,
    }


def _sample(times, values, time, rotate):
    if time <= times[0]:
        return values[0]
    if time >= times[-1]:
        return values[-1]
    index = int(np.searchsorted(times, time) - 1)
    index = max(0, min(index, len(times) - 2))
    span = times[index + 1] - times[index]
    blend = 0.0 if span <= 0 else (time - times[index]) / span
    start, end = values[index], values[index + 1]
    if rotate and np.dot(start, end) < 0:
        end = -end
    value = start * (1 - blend) + end * blend
    if rotate:
        value = value / np.linalg.norm(value)
    return value


def pose_clip(clip, time):
    """Same skinning path DogClip.kt uses, including the baked fit."""
    local = np.empty_like(clip["rest_local"])
    overrides = {}
    for node, path, times, values in clip["channels"]:
        overrides.setdefault(node, {})[path] = _sample(times, values, time, path == 1)
    for index in range(len(local)):
        if clip["use_rest"][index]:
            local[index] = clip["rest_local"][index]
            continue
        translation = clip["rest_t"][index].astype(np.float64)
        rotation = clip["rest_r"][index].astype(np.float64)
        scale = clip["rest_s"][index].astype(np.float64)
        chosen = overrides.get(index, {})
        if 0 in chosen:
            translation = chosen[0]
        if 1 in chosen:
            rotation = chosen[1]
        if 2 in chosen:
            scale = chosen[2]
        translate = np.eye(4)
        translate[:3, 3] = translation
        scale_m = np.eye(4)
        scale_m[0, 0], scale_m[1, 1], scale_m[2, 2] = scale
        local[index] = translate @ quat_matrix(rotation) @ scale_m
    worlds = [None] * len(local)
    for index, parent in enumerate(clip["parents"]):
        worlds[index] = local[index] if parent < 0 else worlds[parent] @ local[index]
    inv_mesh = clip["inv_mesh"]
    fit = clip["fit"]
    scale = float(clip["scale"])
    joints = []
    for node, ibm in zip(clip["joint_nodes"], clip["ibms"]):
        matrix = inv_mesh @ worlds[node] @ ibm
        matrix = matrix.copy()
        matrix[:3, 3] = matrix[:3, 3] * scale + fit
        joints.append(matrix)
    joints = np.stack(joints)
    positions = clip["bind_pos"]
    homogeneous = np.concatenate([positions, np.ones((len(positions), 1))], axis=1)
    posed = np.zeros_like(positions)
    for influence in range(4):
        matrices = joints[clip["joints"][:, influence]]
        posed += np.einsum("nij,nj->ni", matrices, homogeneous)[:, :3] * clip["weights"][:, influence, None]
    return posed


def main():
    if not os.path.exists(GLB):
        os.makedirs(os.path.dirname(GLB), exist_ok=True)
        urllib.request.urlretrieve(GLB_URL, GLB)
    js, accessor = load_glb(GLB)
    positions, normals, uvs, faces, png = skin(js, accessor, POSE_TIME)
    center, scale = fit_from_pose(positions)
    lowest = lowest_point(js, accessor, center, scale)
    lift = 0.0 if lowest >= 0.02 else 0.02 - lowest
    positions = fit_frame(positions, center, scale, lift)
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
    clip_bytes, duration = pack_clip(js, accessor, center, scale, lift)
    open(OUT_ANIM, "wb").write(clip_bytes)
    clip = read_clip(clip_bytes)
    worst = 0.0
    for time in (0.0, 4.0, 8.25, 14.0, 21.5, 32.0):
        posed, _normals, _uvs, _faces, _png = skin(js, accessor, time)
        expected = fit_frame(posed, center, scale, lift)
        played = pose_clip(clip, time)
        error = float(np.max(np.abs(expected - played)))
        worst = max(worst, error)
        print(f"t={time:.2f} max err {error:.6f} y {played[:,1].min():.3f}..{played[:,1].max():.3f}")
    print(
        f"pug {len(positions)} verts  {len(faces) // 3} tris  "
        f"json {os.path.getsize(OUT_JSON) / 1024:.0f} KB  "
        f"anim {len(clip_bytes) / 1024:.0f} KB  duration {duration:.2f}s  "
        f"lift {lift:.3f}  worst {worst:.6f}"
    )
    print("bounds", positions.min(0).round(3), positions.max(0).round(3))
    if worst > 1e-3:
        raise SystemExit("clip playback does not match the skinned mesh")


if __name__ == "__main__":
    main()
