package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import java.util.Random

/**
 * Hearts that float off the dog when it is loved.
 */
class HeartBurst @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Heart(val x: Float, var y: Float, val size: Float, val speed: Float, val drift: Float, var life: Float)

    private val hearts = ArrayList<Heart>()
    private val random = Random()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE23B4A.toInt() }
    private val path = Path()
    private var running = false
    private var elapsed = 0f
    private var last = 0L
    private val tick = Runnable { step() }

    fun burst() {
        hearts.clear()
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
        hearts.clear()
    }

    private fun step() {
        if (!running) return
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else ((now - last).coerceAtMost(34L)) / 1000f
        last = now
        elapsed += dt
        if (elapsed < 0.45f && hearts.size < 8 && width > 0) {
            hearts.add(
                Heart(
                    x = width * (0.28f + random.nextFloat() * 0.44f),
                    y = height * 0.62f,
                    size = 14f + random.nextFloat() * 12f,
                    speed = 140f + random.nextFloat() * 80f,
                    drift = (random.nextFloat() - 0.5f) * 40f,
                    life = 1f
                )
            )
        }
        for (heart in hearts) {
            heart.y -= heart.speed * dt
            heart.life -= dt * 0.7f
        }
        invalidate()
        if (elapsed < 1.5f) postDelayed(tick, 16) else stop()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        if (!running) return
        for (heart in hearts) {
            if (heart.life <= 0f) continue
            fill.alpha = (heart.life.coerceIn(0f, 1f) * 230).toInt()
            heartPath(heart.x, heart.y, heart.size)
            canvas.drawPath(path, fill)
        }
    }

    private fun heartPath(cx: Float, cy: Float, size: Float) {
        path.reset()
        val r = size * 0.28f
        path.addCircle(cx - r, cy - r * 0.2f, r, Path.Direction.CW)
        path.addCircle(cx + r, cy - r * 0.2f, r, Path.Direction.CW)
        path.moveTo(cx - size * 0.5f, cy)
        path.lineTo(cx, cy + size * 0.55f)
        path.lineTo(cx + size * 0.5f, cy)
        path.close()
    }
}
