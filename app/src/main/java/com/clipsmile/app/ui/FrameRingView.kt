package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * A picture frame around the pet, with a lip, corner studs and a top gem.
 */
class FrameRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var look: FrameLook = FrameLooks.all.first()
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val box = RectF()

    fun setFrame(value: FrameLook) {
        look = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val outer = (10f + look.tier * 2f) * density
        val margin = 8f * density
        box.set(margin, margin, width - margin, height - margin)
        stroke.strokeWidth = outer
        stroke.color = look.ring
        canvas.drawRoundRect(box, 28f * density, 28f * density, stroke)

        box.inset(outer * 0.72f, outer * 0.72f)
        stroke.strokeWidth = (3.2f + look.tier) * density
        stroke.color = look.inner
        canvas.drawRoundRect(box, 18f * density, 18f * density, stroke)

        val gem = min(width, height) * (0.018f + look.tier * 0.004f)
        fill.color = look.inner
        val corners = arrayOf(
            margin + outer to margin + outer,
            width - margin - outer to margin + outer,
            margin + outer to height - margin - outer,
            width - margin - outer to height - margin - outer
        )
        for ((x, y) in corners) canvas.drawCircle(x, y, gem, fill)
        if (look.tier >= 1) {
            fill.color = look.ring
            canvas.drawCircle(width / 2f, margin + outer * 0.15f, gem * 1.35f, fill)
            fill.color = look.inner
            canvas.drawCircle(width / 2f, margin + outer * 0.15f, gem * 0.62f, fill)
        }
        if (look.tier >= 2) {
            fill.color = look.ring
            canvas.drawCircle(width / 2f, height - margin - outer * 0.15f, gem, fill)
        }
        stroke.strokeWidth = (1.4f + look.tier * 0.3f) * density
        stroke.color = look.inner
        val arm = gem * 2.4f
        for ((x, y) in corners) {
            canvas.drawLine(x - arm, y, x + arm, y, stroke)
            canvas.drawLine(x, y - arm, x, y + arm, stroke)
        }
    }
}
