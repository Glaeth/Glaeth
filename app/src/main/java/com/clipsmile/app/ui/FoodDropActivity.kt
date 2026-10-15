package com.clipsmile.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.clipsmile.app.R
import com.clipsmile.app.data.PetProgress
import kotlin.random.Random

class FoodDropActivity : AppCompatActivity() {
    private lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio.drop(this)
        val portrait = intent.getIntExtra(EXTRA_PORTRAIT, R.drawable.ic_pug_icon)
        val food = intent.getIntExtra(EXTRA_FOOD, R.drawable.ic_doner)
        setContentView(FoodDropView(this, portrait, food, audio))
    }

    override fun onResume() {
        super.onResume()
        audio.start()
    }

    override fun onPause() {
        audio.pause()
        super.onPause()
    }

    override fun onDestroy() {
        audio.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PORTRAIT = "portrait"
        const val EXTRA_FOOD = "food"
    }
}

private class Drop(var x: Float, var y: Float, val speed: Float)

/**
 * Food falls. Drag the dog and catch it. Three misses end the round.
 */
private class FoodDropView(
    context: Context,
    portraitRes: Int,
    foodRes: Int,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val dogBmp = raster(portraitRes, dp(108f).toInt())
    private val foodBmp = raster(foodRes, dp(52f).toInt())
    private val drops = ArrayList<Drop>()
    private val random = Random(SystemClock.uptimeMillis())

    private var dogX = 0f
    private var placed = false
    private var score = 0
    private var lives = 3
    private var spawnIn = 0.45f
    private var over = false
    private var lastMs = 0L
    private var best = GameScores.best(context, GameScores.BEST_DROP)

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        isFakeBoldText = true
    }
    private val floor = Paint().apply { color = 0xFFE7C98A.toInt() }
    private val life = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE35D5D.toInt() }
    private val lifeGone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66A8783C }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF23B2918.toInt() }

    private val frame = Runnable { tick() }

    init {
        keepScreenOn = true
        setBackgroundColor(0xFFF6E2B8.toInt())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (!placed && w > 0) {
            dogX = w / 2f
            placed = true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && event.x < dp(56f) && event.y < dp(56f)) {
            (context as? Activity)?.finish()
            return true
        }
        if (over) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        if (width > 0) {
            val half = dogBmp.width / 2f
            dogX = event.x.coerceIn(half, width - half)
        }
        return true
    }

    private fun restart() {
        drops.clear()
        score = 0
        lives = 3
        spawnIn = 0.4f
        over = false
        lastMs = 0L
        postOnAnimation(frame)
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastMs == 0L) 0.016f else ((now - lastMs).coerceAtMost(34L)) / 1000f
        lastMs = now
        if (!over) step(dt)
        invalidate()
        if (!over) postOnAnimation(frame)
    }

    private fun step(dt: Float) {
        if (width == 0) return
        spawnIn -= dt
        if (spawnIn <= 0f) {
            spawnIn = (0.92f - score * 0.018f).coerceAtLeast(0.38f)
            val margin = dp(30f)
            val x = margin + random.nextFloat() * (width - margin * 2)
            drops.add(Drop(x, -dp(16f), dp(210f) + score * dp(9f)))
        }
        val dogTop = height - dp(40f) - dogBmp.height
        val left = dogX - dogBmp.width / 2f
        val right = dogX + dogBmp.width / 2f
        val mouthTop = dogTop + dogBmp.height * 0.22f
        val mouthBottom = dogTop + dogBmp.height * 0.72f
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.y += drop.speed * dt
            if (drop.y in mouthTop..mouthBottom && drop.x in left..right) {
                iterator.remove()
                score += 1
                audio.catchFood()
                Achievements.celebrate(context, Achievements.DROP, score)
            } else if (drop.y > height + dp(8f)) {
                iterator.remove()
                lives -= 1
                if (lives <= 0) endRound() else audio.miss()
            }
        }
    }

    private fun endRound() {
        if (over) return
        over = true
        audio.crash()
        best = GameScores.submit(context, GameScores.BEST_DROP, score)
        if (score > 0) PetProgress(context).snack(score.coerceAtMost(18))
        Achievements.celebrate(context, Achievements.DROP, score)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, height - dp(26f), width.toFloat(), height.toFloat(), floor)
        for (drop in drops) {
            canvas.drawBitmap(foodBmp, drop.x - foodBmp.width / 2f, drop.y - foodBmp.height / 2f, null)
        }
        canvas.drawBitmap(dogBmp, dogX - dogBmp.width / 2f, height - dp(40f) - dogBmp.height, null)

        ink.color = 0xFF3B2918.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(38f), ink)
        canvas.drawText(score.toString(), dp(58f), dp(38f), ink)

        val radius = dp(7f)
        var lifeX = width - dp(18f) - radius
        for (index in 2 downTo 0) {
            canvas.drawCircle(lifeX, dp(28f), radius, if (index < lives) life else lifeGone)
            lifeX -= dp(20f)
        }

        if (over) {
            val top = height * 0.30f
            val bottom = height * 0.58f
            canvas.drawRoundRect(dp(32f), top, width - dp(32f), bottom, dp(18f), dp(18f), card)
            ink.color = 0xFFF6E2B8.toInt()
            ink.textAlign = Paint.Align.CENTER
            ink.textSize = dp(22f)
            canvas.drawText("Skor  $score", width / 2f, top + dp(46f), ink)
            ink.textSize = dp(15f)
            canvas.drawText("En iyi  $best", width / 2f, top + dp(74f), ink)
            canvas.drawText("Dokun, yeniden oyna", width / 2f, top + dp(102f), ink)
        }
    }

    private fun raster(res: Int, size: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, res) ?: return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
