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
}
