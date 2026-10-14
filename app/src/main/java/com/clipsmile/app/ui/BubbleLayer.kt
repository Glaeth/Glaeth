package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import java.util.Random

/**
 * Foam and bubbles drawn over the dog for the bath animation.
 */
class BubbleLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Dot(val x: Float, var y: Float, val r: Float, val speed: Float)

    private val dots = ArrayList<Dot>()
    private val random = Random()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6F7FBFF.toInt() }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7EB6D8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val foam = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDDF7FBFF.toInt() }

    private var running = false
    private var elapsed = 0f
    private var last = 0L
    private var end: (() -> Unit)? = null
    private val tick = Runnable { step() }

    fun burst(onEnd: () -> Unit) {
        end = onEnd
        dots.clear()
        elapsed = 0f
        last = 0L
        running = true
        visibility = VISIBLE
        removeCallbacks(tick)
        post(tick)
    }

    fun stop() {
        running = false
        removeCallbacks(tick)
        visibility = INVISIBLE
        end = null
    }

    private fun step() {
        if (!running) return
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else ((now - last).coerceAtMost(34L)) / 1000f
        last = now
        elapsed += dt
        if (elapsed < 1.6f && dots.size < 16 && random.nextFloat() < 0.55f && width > 0) {
            dots.add(
                Dot(
                    x = width * (0.16f + random.nextFloat() * 0.68f),
                    y = height * 0.78f,
                    r = 12f + random.nextFloat() * 18f,
                    speed = 110f + random.nextFloat() * 90f
                )
            )
        }
        for (dot in dots) dot.y -= dot.speed * dt
        invalidate()
        if (elapsed < 2.2f) {
            postDelayed(tick, 16)
        } else {
            finish()
        }
    }

    private fun finish() {
        running = false
        visibility = INVISIBLE
        val callback = end
        end = null
        callback?.invoke()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        if (!running || width == 0) return
        val grow = (elapsed / 0.35f).coerceAtMost(1f)
        val top = height * (0.78f - 0.16f * grow)
        canvas.drawOval(width * 0.1f, top, width * 0.9f, height * 0.92f, foam)
        for (dot in dots) {
            if (dot.y < -dot.r) continue
            canvas.drawCircle(dot.x, dot.y, dot.r, fill)
            canvas.drawCircle(dot.x, dot.y, dot.r, rim)
        }
    }
}
