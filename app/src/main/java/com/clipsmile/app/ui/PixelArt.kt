package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/** Nearest-neighbor sprites, so the pixel packs stay crisp when scaled. */
object PixelArt {
    val paint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    /** The pet strips are shaded meshes, so they stay smooth when scaled. */
    val smooth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }

    fun petStrip(context: Context, model: String, kind: String, fallback: String): Bitmap {
        return try {
            load(context, "games/pets/${model}_$kind.png")
        } catch (_: Exception) {
            load(context, fallback)
        }
    }

    fun load(context: Context, path: String): Bitmap {
        val options = BitmapFactory.Options().apply { inScaled = false }
        return context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    fun blit(canvas: Canvas, sheet: Bitmap, frame: Int, frameWidth: Int, dest: RectF, src: Rect) {
        val count = (sheet.width / frameWidth).coerceAtLeast(1)
        val index = Math.floorMod(frame, count)
        src.set(index * frameWidth, 0, index * frameWidth + frameWidth, sheet.height)
        canvas.drawBitmap(sheet, src, dest, paint)
    }

    fun blitSmooth(canvas: Canvas, sheet: Bitmap, frame: Int, frameWidth: Int, dest: RectF, src: Rect) {
        val count = (sheet.width / frameWidth).coerceAtLeast(1)
        val index = Math.floorMod(frame, count)
        src.set(index * frameWidth, 0, index * frameWidth + frameWidth, sheet.height)
        canvas.drawBitmap(sheet, src, dest, smooth)
    }

    private var scoreBar: Bitmap? = null

    private fun scoreBar(context: Context): Bitmap {
        val cached = scoreBar
        if (cached != null && !cached.isRecycled) return cached
        return load(context, "hud/bar_bg.png").also { scoreBar = it }
    }

    /** Ornate status bar from the pixel HUD pack. The number sits in the dark trough. */
    fun drawScore(canvas: Canvas, context: Context, score: Int, centerX: Float, top: Float, dp: (Float) -> Float) {
        val bar = scoreBar(context)
        val barW = dp(214f)
        val barH = barW * bar.height / bar.width.toFloat()
        val left = centerX - barW / 2f
        canvas.drawBitmap(bar, null, RectF(left, top, left + barW, top + barH), paint)
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF1C9.toInt()
            textAlign = Paint.Align.CENTER
            textSize = dp(16f)
            isFakeBoldText = true
        }
        canvas.drawText(score.toString(), left + barW * 0.58f, top + barH * 0.70f, value)
    }

    /** Segment bar. Filled segments sit on the left. */
    fun drawHealth(canvas: Canvas, filled: Int, total: Int, right: Float, top: Float, dp: (Float) -> Float) {
        val gap = dp(4f)
        val w = dp(22f)
        val h = dp(10f)
        var x = right - total * w - (total - 1) * gap
        for (index in 0 until total) {
            val rect = RectF(x, top, x + w, top + h)
            canvas.drawRoundRect(rect, dp(3f), dp(3f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (index < filled) 0xFFE35D5D.toInt() else 0x663B2918
            })
            x += w + gap
        }
    }

    fun drawBanner(canvas: Canvas, text: String, centerX: Float, top: Float, dp: (Float) -> Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF8E8.toInt()
            textAlign = Paint.Align.CENTER
            textSize = dp(16f)
            isFakeBoldText = true
        }
        val width = paint.measureText(text) + dp(22f)
        val rect = RectF(centerX - width / 2f, top, centerX + width / 2f, top + dp(28f))
        canvas.drawRoundRect(rect, dp(10f), dp(10f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xF23B2918.toInt()
        })
        canvas.drawText(text, centerX, top + dp(19f), paint)
    }
}
