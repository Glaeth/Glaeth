package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Oval frame. Higher tiers add an inner ring and corner studs.
 */
class FrameRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var look: FrameLook = FrameLooks.all.first()
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val oval = RectF()

    fun setFrame(value: FrameLook) {
        look = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val widthPx = look.widthDp * density
        val margin = widthPx / 2f + 8f * density
        oval.set(margin, margin, width - margin, height - margin)
        stroke.strokeWidth = widthPx
        stroke.color = look.ring
        canvas.drawOval(oval, stroke)

        if (look.tier >= 1) {
            val gap = widthPx * 0.55f + 3f * density
            oval.inset(gap, gap)
            stroke.strokeWidth = (2.2f * density).coerceAtLeast(2f)
            stroke.color = look.inner
            canvas.drawOval(oval, stroke)
            oval.inset(-gap, -gap)
        }

        if (look.tier >= 2) {
            fill.color = if (look.tier >= 3) look.inner else look.ring
            val cx = width / 2f
            val cy = height / 2f
            val rx = width / 2f - margin
            val ry = height / 2f - margin
            val gem = min(width, height) * 0.035f + density
            canvas.drawCircle(cx, cy - ry, gem, fill)
            canvas.drawCircle(cx, cy + ry, gem, fill)
            canvas.drawCircle(cx - rx, cy, gem, fill)
            canvas.drawCircle(cx + rx, cy, gem, fill)
            if (look.tier >= 3) {
                fill.color = look.ring
                val midX = rx * 0.72f
                val midY = ry * 0.72f
                canvas.drawCircle(cx - midX, cy - midY, gem * 0.7f, fill)
                canvas.drawCircle(cx + midX, cy - midY, gem * 0.7f, fill)
                canvas.drawCircle(cx - midX, cy + midY, gem * 0.7f, fill)
                canvas.drawCircle(cx + midX, cy + midY, gem * 0.7f, fill)
            }
        }
    }
}
