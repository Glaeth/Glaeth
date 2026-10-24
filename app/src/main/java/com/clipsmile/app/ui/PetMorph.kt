package com.clipsmile.app.ui

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * A short loop of fitted poses. Positions are int16 values divided by 8000.
 */
internal class PetMorph private constructor(
    private val duration: Float,
    private val frames: Array<FloatArray>
) {
    fun write(seconds: Float, positions: FloatBuffer) {
        if (frames.isEmpty() || positions.capacity() < frames[0].size) return
        val span = if (duration <= 0f) 1f else duration
        val scaled = (seconds % span) / span * frames.size
        val index = scaled.toInt() % frames.size
        val next = (index + 1) % frames.size
        val blend = scaled - scaled.toInt()
        val keep = 1f - blend
        val a = frames[index]
        val b = frames[next]
        positions.position(0)
        for (i in a.indices) positions.put(a[i] * keep + b[i] * blend)
        positions.position(0)
    }

    companion object {
        fun open(stream: InputStream): PetMorph? {
            val bytes = stream.readBytes()
            if (bytes.size < 12 || bytes.copyOfRange(0, 4).decodeToString() != "FRM1") return null
            val view = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            view.position(4)
            val count = view.short.toInt() and 0xFFFF
            val verts = view.short.toInt() and 0xFFFF
            val duration = view.float
            val frames = Array(count) { FloatArray(verts * 3) }
            for (frame in 0 until count) {
                val values = frames[frame]
                for (i in values.indices) values[i] = view.short / 8000f
            }
            return PetMorph(duration, frames)
        }
    }
}
