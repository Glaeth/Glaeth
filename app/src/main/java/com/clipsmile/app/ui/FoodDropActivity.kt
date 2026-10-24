package com.clipsmile.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
        val model = intent.getStringExtra(EXTRA_MODEL) ?: "pug"
        setContentView(FoodDropView(this, portrait, food, model, audio))
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
        const val EXTRA_MODEL = "model"
    }
}

private class Drop(var x: Float, var y: Float, val speed: Float, val kind: Int)

/**
 * Food falls. Drag the dog and catch it. Three misses end the round.
 */
private class FoodDropView(
    context: Context,
    portraitRes: Int,
    foodRes: Int,
    model: String,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val dogBmp = loadPet(model, portraitRes)
    private val foods = loadFoods(foodRes)
    private val backgrounds = (0..4).map { PixelArt.load(context, "games/flap/t$it/bg.png") }
    private val tile = RectF()
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
    private var banner = ""
    private var bannerUntil = 0L

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        isFakeBoldText = true
    }
    private val floor = Paint().apply { color = 0xFF5C8A3C.toInt() }
    private val soil = Paint().apply { color = 0xFF6A4E32.toInt() }
    private val petSrc = android.graphics.Rect()
    private val life = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE35D5D.toInt() }
    private val lifeGone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66A8783C }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF23B2918.toInt() }

    private val frame = Runnable { tick() }

    init {
        keepScreenOn = true
        setBackgroundColor(0xFF8EC8E8.toInt())
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
            val half = dogSize().first / 2f
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
        banner = ""
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
            val kind = (score / 150) % foods.size
            drops.add(Drop(x, -dp(16f), dp(210f) + score * dp(9f), kind))
        }
        val floorTop = height - dp(36f)
        val (dogW, dogH) = dogSize()
        val dogTop = floorTop - dogH
        val left = dogX - dogW / 2f
        val right = dogX + dogW / 2f
        val mouthTop = dogTop + dogH * 0.08f
        val mouthBottom = floorTop
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.y += drop.speed * dt
            if (drop.y in mouthTop..mouthBottom && drop.x in left..right) {
                iterator.remove()
                val previous = score
                score += 1
                audio.catchFood()
                showReward(GameRewards.onScore(context, previous, score, 150, 8, 300, 12, "Düşen yemek"))
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
        if (score > 0) {
            PetProgress(context).snack(score.coerceAtMost(18))
            com.clipsmile.app.data.Den(context).bump(com.clipsmile.app.data.Den.Kind.DROP, score)
        }
        Achievements.celebrate(context, Achievements.DROP, score)
    }

    override fun onDraw(canvas: Canvas) {
        val stage = (score / 150) % backgrounds.size
        val floorTop = height - dp(36f)
        tile.set(0f, 0f, width.toFloat(), floorTop)
        canvas.drawBitmap(backgrounds[stage], null, tile, PixelArt.paint)
        canvas.drawRect(0f, floorTop, width.toFloat(), height.toFloat(), soil)
        canvas.drawRect(0f, floorTop, width.toFloat(), floorTop + dp(10f), floor)
        for (drop in drops) {
            val foodBmp = foods[drop.kind % foods.size]
            val size = dp(40f)
            canvas.drawBitmap(
                foodBmp,
                null,
                android.graphics.RectF(drop.x - size / 2f, drop.y - size / 2f, drop.x + size / 2f, drop.y + size / 2f),
                PixelArt.paint
            )
        }
        val (dogW, dogH) = dogSize()
        val frame = ((SystemClock.uptimeMillis() / 150L) % petFrames).toInt()
        PixelArt.blit(
            canvas,
            dogBmp,
            frame,
            dogBmp.width / petFrames,
            android.graphics.RectF(dogX - dogW / 2f, floorTop - dogH, dogX + dogW / 2f, floorTop),
            petSrc
        )

        ink.color = 0xFF3B2918.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(38f), ink)
        PixelArt.drawScore(canvas, score, width / 2f, dp(6f), ::dp)
        if (banner.isNotEmpty() && SystemClock.uptimeMillis() < bannerUntil) {
            PixelArt.drawBanner(canvas, banner, width / 2f, dp(58f), ::dp)
        }

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

    private val petFrames = if (dogBmp.width > dogBmp.height * 2) 4 else 1

    private fun dogSize(): Pair<Float, Float> {
        val dogH = dp(132f)
        val frameW = dogBmp.width / petFrames
        val dogW = dogH * frameW / dogBmp.height.coerceAtLeast(1)
        return dogW to dogH
    }

    private fun showReward(text: String?) {
        if (text.isNullOrBlank()) return
        banner = text
        bannerUntil = SystemClock.uptimeMillis() + 1300L
        audio.reward()
    }

    private fun loadPet(model: String, portraitRes: Int): Bitmap {
        return try {
            PixelArt.load(context, "games/pets/${model}_front.png")
        } catch (_: Exception) {
            try {
                PixelArt.load(context, "games/drop/$model.png")
            } catch (_: Exception) {
                raster(portraitRes, dp(108f).toInt())
            }
        }
    }

    private fun loadFoods(foodRes: Int): List<Bitmap> {
        val packed = (0..3).map { PixelArt.load(context, "games/drop/food/f$it.png") }
        return listOf(raster(foodRes, dp(52f).toInt())) + packed
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
