package com.clipsmile.app.ui

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Looping skeletal clip for the first dog. The file is packed by
 * tools/import_textured_dog.py (little-endian, magic "DOG1").
 * Positions stay in the same fitted space as the sitting pose.
 */
internal class DogClip private constructor(
    private val duration: Float,
    private val scale: Float,
    private val fitX: Float,
    private val fitY: Float,
    private val fitZ: Float,
    private val parents: IntArray,
    private val useRest: BooleanArray,
    private val restT: FloatArray,
    private val restR: FloatArray,
    private val restS: FloatArray,
    private val restLocal: Array<FloatArray>,
    private val meshNode: Int,
    private val invMesh: FloatArray,
    private val jointNodes: IntArray,
    private val ibms: Array<FloatArray>,
    private val bindPos: FloatArray,
    private val bindNor: FloatArray,
    private val influences: IntArray,
    private val weights: FloatArray,
    private val animated: List<AnimatedNode>,
    val vertexCount: Int
) {
    private val locals = Array(parents.size) { FloatArray(16) }
    private val worlds = Array(parents.size) { FloatArray(16) }
    private val jointMats = Array(jointNodes.size) { FloatArray(16) }
    private val mulTmp = FloatArray(16)
    private val outPos = FloatArray(vertexCount * 3)
    private val outNor = FloatArray(vertexCount * 3)
    private val startPos = FloatArray(vertexCount * 3)
    private val startNor = FloatArray(vertexCount * 3)
    private var startReady = false
    private var lastTime = -1f

    fun write(seconds: Float, positions: FloatBuffer, normals: FloatBuffer) {
        if (positions.capacity() < vertexCount * 3 || normals.capacity() < vertexCount * 3) return
        var time = seconds % duration
        if (time < 0f) time += duration
        if (time < lastTime) rewind()
        lastTime = time
        skin(time, outPos, outNor)
        if (!startReady) {
            if (time == 0f) {
                System.arraycopy(outPos, 0, startPos, 0, startPos.size)
                System.arraycopy(outNor, 0, startNor, 0, startNor.size)
            } else {
                skin(0f, startPos, startNor)
                rewind()
                skin(time, outPos, outNor)
            }
            startReady = true
        }
        val blend = loopWindow()
        val fadeAt = duration - blend
        if (blend < duration && time > fadeAt) {
            val towardStart = (time - fadeAt) / blend
            val keep = 1f - towardStart
            blend(outPos, startPos, keep, towardStart)
            blend(outNor, startNor, keep, towardStart)
            normalizeNormals(outNor)
        }
        positions.position(0)
        positions.put(outPos, 0, outPos.size)
        positions.position(0)
        normals.position(0)
        normals.put(outNor, 0, outNor.size)
        normals.position(0)
    }

    private fun rewind() {
        for (node in animated) {
            node.translation?.cursor = 0
            node.rotation?.cursor = 0
            node.scale?.cursor = 0
        }
    }

    private fun skin(time: Float, dstPos: FloatArray, dstNor: FloatArray) {
        for (index in parents.indices) {
            if (useRest[index]) {
                System.arraycopy(restLocal[index], 0, locals[index], 0, 16)
            }
        }
        for (node in animated) {
            var tx = restT[node.index * 3]
            var ty = restT[node.index * 3 + 1]
            var tz = restT[node.index * 3 + 2]
            var qx = restR[node.index * 4]
            var qy = restR[node.index * 4 + 1]
            var qz = restR[node.index * 4 + 2]
            var qw = restR[node.index * 4 + 3]
            var sx = restS[node.index * 3]
            var sy = restS[node.index * 3 + 1]
            var sz = restS[node.index * 3 + 2]
            node.translation?.let {
                sample(it, time, sample3)
                tx = sample3[0]; ty = sample3[1]; tz = sample3[2]
            }
            node.rotation?.let {
                sample(it, time, sample4)
                qx = sample4[0]; qy = sample4[1]; qz = sample4[2]; qw = sample4[3]
            }
            node.scale?.let {
                sample(it, time, sample3)
                sx = sample3[0]; sy = sample3[1]; sz = sample3[2]
            }
            trs(tx, ty, tz, qx, qy, qz, qw, sx, sy, sz, locals[node.index])
        }
        for (index in parents.indices) {
            val parent = parents[index]
            if (parent < 0) {
                System.arraycopy(locals[index], 0, worlds[index], 0, 16)
            } else {
                mul(worlds[parent], locals[index], worlds[index])
            }
        }
        for (joint in jointNodes.indices) {
            mul(invMesh, worlds[jointNodes[joint]], mulTmp)
            mul(mulTmp, ibms[joint], jointMats[joint])
            val fitted = jointMats[joint]
            fitted[12] = fitted[12] * scale + fitX
            fitted[13] = fitted[13] * scale + fitY
            fitted[14] = fitted[14] * scale + fitZ
        }
        var vertex = 0
        var p = 0
        while (vertex < vertexCount) {
            val px = bindPos[p]
            val py = bindPos[p + 1]
            val pz = bindPos[p + 2]
            val nx = bindNor[p]
            val ny = bindNor[p + 1]
            val nz = bindNor[p + 2]
            var x = 0f
            var y = 0f
            var z = 0f
            var nxx = 0f
            var nyy = 0f
            var nzz = 0f
            val base = vertex * 4
            for (influence in 0 until 4) {
                val weight = weights[base + influence]
                if (weight <= 0f) continue
                val m = jointMats[influences[base + influence]]
                x += (m[0] * px + m[4] * py + m[8] * pz + m[12]) * weight
                y += (m[1] * px + m[5] * py + m[9] * pz + m[13]) * weight
                z += (m[2] * px + m[6] * py + m[10] * pz + m[14]) * weight
                nxx += (m[0] * nx + m[4] * ny + m[8] * nz) * weight
                nyy += (m[1] * nx + m[5] * ny + m[9] * nz) * weight
                nzz += (m[2] * nx + m[6] * ny + m[10] * nz) * weight
            }
            dstPos[p] = x
            dstPos[p + 1] = y
            dstPos[p + 2] = z
            val length = sqrt(nxx * nxx + nyy * nyy + nzz * nzz)
            if (length > 1e-8f) {
                dstNor[p] = nxx / length
                dstNor[p + 1] = nyy / length
                dstNor[p + 2] = nzz / length
            } else {
                dstNor[p] = 0f
                dstNor[p + 1] = 1f
                dstNor[p + 2] = 0f
            }
            vertex++
            p += 3
        }
    }

    private fun sample(channel: Channel, time: Float, into: FloatArray) {
        val times = channel.times
        val count = times.size
        val stride = channel.stride
        if (time <= times[0]) {
            copyKey(channel, 0, into)
            channel.cursor = 0
            return
        }
        if (time >= times[count - 1]) {
            copyKey(channel, count - 1, into)
            channel.cursor = count - 1
            return
        }
        var index = channel.cursor
        if (index >= count - 1 || times[index] > time) index = 0
        while (index + 1 < count && times[index + 1] <= time) index++
        channel.cursor = index
        val next = index + 1
        val span = times[next] - times[index]
        val blend = if (span <= 0f) 0f else (time - times[index]) / span
        val values = channel.values
        val a = index * stride
        val b = next * stride
        if (stride == 4) {
            var bx = values[b]
            var by = values[b + 1]
            var bz = values[b + 2]
            var bw = values[b + 3]
            val dot = values[a] * bx + values[a + 1] * by + values[a + 2] * bz + values[a + 3] * bw
            if (dot < 0f) {
                bx = -bx; by = -by; bz = -bz; bw = -bw
            }
            val keep = 1f - blend
            var x = values[a] * keep + bx * blend
            var y = values[a + 1] * keep + by * blend
            var z = values[a + 2] * keep + bz * blend
            var w = values[a + 3] * keep + bw * blend
            val length = sqrt(x * x + y * y + z * z + w * w)
            if (length > 1e-8f) {
                x /= length; y /= length; z /= length; w /= length
            }
            into[0] = x; into[1] = y; into[2] = z; into[3] = w
        } else {
            val keep = 1f - blend
            for (component in 0 until stride) {
                into[component] = values[a + component] * keep + values[b + component] * blend
            }
        }
    }

    private fun copyKey(channel: Channel, index: Int, into: FloatArray) {
        val start = index * channel.stride
        for (component in 0 until channel.stride) into[component] = channel.values[start + component]
    }

    private class Channel(val stride: Int, val times: FloatArray, val values: FloatArray, var cursor: Int)

    private class AnimatedNode(val index: Int) {
        var translation: Channel? = null
        var rotation: Channel? = null
        var scale: Channel? = null
    }

    private fun loopWindow(): Float = (duration * 0.12f).coerceIn(0.04f, 0.55f)

    companion object {
        private const val LOOP_BLEND = 0.55f
        private val sample3 = FloatArray(3)
        private val sample4 = FloatArray(4)

        fun open(stream: InputStream): DogClip {
            val bytes = stream.use { it.readBytes() }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4)
            buf.get(magic)
            if (magic[0] != 'D'.code.toByte() || magic[1] != 'O'.code.toByte() ||
                magic[2] != 'G'.code.toByte() || magic[3] != '1'.code.toByte()
            ) {
                error("dog clip")
            }
            val duration = buf.float
            val nodeCount = buf.int
            val scale = buf.float
            val fitX = buf.float
            val fitY = buf.float
            val fitZ = buf.float
            val parents = IntArray(nodeCount)
            val useRest = BooleanArray(nodeCount)
            val restT = FloatArray(nodeCount * 3)
            val restR = FloatArray(nodeCount * 4)
            val restS = FloatArray(nodeCount * 3)
            val restLocal = Array(nodeCount) { FloatArray(16) }
            for (index in 0 until nodeCount) {
                parents[index] = buf.int
                useRest[index] = buf.get().toInt() != 0
                readFloats(buf, restT, index * 3, 3)
                readFloats(buf, restR, index * 4, 4)
                readFloats(buf, restS, index * 3, 3)
                readFloats(buf, restLocal[index], 0, 16)
            }
            val meshNode = buf.int
            val invMesh = FloatArray(16)
            readFloats(buf, invMesh, 0, 16)
            val jointCount = buf.int
            val jointNodes = IntArray(jointCount)
            for (joint in 0 until jointCount) jointNodes[joint] = buf.int
            val ibms = Array(jointCount) { FloatArray(16) }
            for (joint in 0 until jointCount) readFloats(buf, ibms[joint], 0, 16)
            val vertexCount = buf.int
            val bindPos = FloatArray(vertexCount * 3)
            val bindNor = FloatArray(vertexCount * 3)
            readFloats(buf, bindPos, 0, bindPos.size)
            readFloats(buf, bindNor, 0, bindNor.size)
            val influences = IntArray(vertexCount * 4)
            for (i in influences.indices) influences[i] = buf.short.toInt() and 0xffff
            val weights = FloatArray(vertexCount * 4)
            readFloats(buf, weights, 0, weights.size)
            val channelCount = buf.int
            val byNode = HashMap<Int, AnimatedNode>()
            repeat(channelCount) {
                val node = buf.int
                val path = buf.get().toInt() and 0xff
                val keyCount = buf.short.toInt() and 0xffff
                val times = FloatArray(keyCount)
                readFloats(buf, times, 0, keyCount)
                val stride = if (path == 1) 4 else 3
                val values = FloatArray(keyCount * stride)
                readFloats(buf, values, 0, values.size)
                val animated = byNode.getOrPut(node) { AnimatedNode(node) }
                val channel = Channel(stride, times, values, 0)
                when (path) {
                    0 -> animated.translation = channel
                    1 -> animated.rotation = channel
                    else -> animated.scale = channel
                }
            }
            if (buf.remaining() != 0) error("dog clip trailer")
            return DogClip(
                duration, scale, fitX, fitY, fitZ,
                parents, useRest, restT, restR, restS, restLocal,
                meshNode, invMesh, jointNodes, ibms,
                bindPos, bindNor, influences, weights,
                byNode.values.sortedBy { it.index },
                vertexCount
            )
        }

        private fun readFloats(buf: ByteBuffer, dst: FloatArray, offset: Int, count: Int) {
            for (i in 0 until count) dst[offset + i] = buf.getFloat()
        }

        private fun blend(dst: FloatArray, toward: FloatArray, keep: Float, into: Float) {
            for (i in dst.indices) dst[i] = dst[i] * keep + toward[i] * into
        }

        private fun normalizeNormals(values: FloatArray) {
            var i = 0
            while (i < values.size) {
                val x = values[i]
                val y = values[i + 1]
                val z = values[i + 2]
                val length = sqrt(x * x + y * y + z * z)
                if (length > 1e-8f) {
                    values[i] = x / length
                    values[i + 1] = y / length
                    values[i + 2] = z / length
                }
                i += 3
            }
        }

        private fun trs(
            tx: Float, ty: Float, tz: Float,
            qx: Float, qy: Float, qz: Float, qw: Float,
            sx: Float, sy: Float, sz: Float,
            dst: FloatArray
        ) {
            val xx = qx * qx
            val yy = qy * qy
            val zz = qz * qz
            val xy = qx * qy
            val xz = qx * qz
            val yz = qy * qz
            val wx = qw * qx
            val wy = qw * qy
            val wz = qw * qz
            dst[0] = (1f - 2f * (yy + zz)) * sx
            dst[1] = (2f * (xy + wz)) * sx
            dst[2] = (2f * (xz - wy)) * sx
            dst[3] = 0f
            dst[4] = (2f * (xy - wz)) * sy
            dst[5] = (1f - 2f * (xx + zz)) * sy
            dst[6] = (2f * (yz + wx)) * sy
            dst[7] = 0f
            dst[8] = (2f * (xz + wy)) * sz
            dst[9] = (2f * (yz - wx)) * sz
            dst[10] = (1f - 2f * (xx + yy)) * sz
            dst[11] = 0f
            dst[12] = tx
            dst[13] = ty
            dst[14] = tz
            dst[15] = 1f
        }

        private fun mul(a: FloatArray, b: FloatArray, c: FloatArray) {
            for (col in 0 until 4) {
                val column = col * 4
                val b0 = b[column]
                val b1 = b[column + 1]
                val b2 = b[column + 2]
                val b3 = b[column + 3]
                c[column] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3
                c[column + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3
                c[column + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3
                c[column + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3
            }
        }
    }
}
