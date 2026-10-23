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

    /** Dark pill so the score stays readable on pixel art. */
    fun drawScore(canvas: Canvas, score: Int, centerX: Float, top: Float, dp: (Float) -> Float) {
        val number = score.toString()
        val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFE7A3.toInt()
            textAlign = Paint.Align.CENTER
            textSize = dp(11f)
            isFakeBoldText = true
        }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF8E8.toInt()
            textAlign = Paint.Align.CENTER
            textSize = dp(26f)
            isFakeBoldText = true
        }
        val width = maxOf(value.measureText(number), caption.measureText("SKOR")) + dp(28f)
        val rect = RectF(centerX - width / 2f, top, centerX + width / 2f, top + dp(48f))
        canvas.drawRoundRect(rect, dp(14f), dp(14f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xF218120C.toInt()
        })
        canvas.drawRoundRect(rect, dp(14f), dp(14f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFC857.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
        })
        canvas.drawText("SKOR", centerX, top + dp(16f), caption)
        canvas.drawText(number, centerX, top + dp(40f), value)
    }
}
