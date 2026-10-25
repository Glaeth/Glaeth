package com.clipsmile.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
    private val plains = PixelArt.load(context, "farm/ground.png")
    private val pumpkins = PixelArt.load(context, "farm/pumpkin.png")
    private val tile = RectF()
    private val tileSrc = android.graphics.Rect()
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
    private val floor = Paint().apply { color = 0xFF4E8A45.toInt() }
    private val soil = Paint().apply { color = 0xFF6A4E32.toInt() }
    private val skyPaint = Paint()
    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7EBF6A.toInt() }
    private val hillFar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB7D7A4.toInt() }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF4FFFFFF.toInt() }
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
            spawnIn = (0.98f - score * 0.0045f).coerceAtLeast(0.55f)
            val margin = dp(30f)
            val x = margin + random.nextFloat() * (width - margin * 2)
            val kind = (score / 150) % foods.size
            val speed = (dp(155f) + score * dp(1.35f)).coerceAtMost(dp(330f))
            drops.add(Drop(x, -dp(16f), speed, kind))
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
        val floorTop = height - dp(36f)
        drawMeadow(canvas, floorTop)
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
        PixelArt.blitSmooth(
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
        PixelArt.drawScore(canvas, context, score, width / 2f, dp(6f), ::dp)
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

    private fun drawMeadow(canvas: Canvas, floorTop: Float) {
        val scene = (score / 50) % 6
        val skyTop = when (scene) {
            1 -> 0xFFB7E3FF.toInt()
            2 -> 0xFF7EB6D8.toInt()
            3 -> 0xFFE38B62.toInt()
            4 -> 0xFF1C2744.toInt()
            5 -> 0xFF9FD0E8.toInt()
            else -> 0xFF8ED0F2.toInt()
        }
        val skyBottom = when (scene) {
            1 -> 0xFFFFF1C9.toInt()
            2 -> 0xFFD7C4A2.toInt()
            3 -> 0xFF6A4A6A.toInt()
            4 -> 0xFF31406A.toInt()
            5 -> 0xFFE7C56A.toInt()
            else -> 0xFFE4F4D8.toInt()
        }
        skyPaint.shader = LinearGradient(0f, 0f, 0f, floorTop, skyTop, skyBottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), floorTop, skyPaint)
        if (scene != 4) {
            canvas.drawOval(RectF(width * 0.12f, dp(36f), width * 0.12f + dp(86f), dp(68f)), cloudPaint)
            canvas.drawOval(RectF(width * 0.62f, dp(22f), width * 0.62f + dp(70f), dp(48f)), cloudPaint)
        }
        canvas.drawCircle(width * 0.18f, floorTop - dp(28f), dp(70f), hillFar)
        canvas.drawCircle(width * 0.78f, floorTop - dp(18f), dp(90f), hillPaint)
        val cell = dp(28f)
        val source = when (scene) {
            2 -> 112 to 0
            4 -> 96 to 80
            5 -> 144 to 48
            else -> 96 to 48
        }
        var y = floorTop - cell * 3f
        while (y < floorTop) {
            var x = -cell
            while (x < width) {
                tileSrc.set(source.first, source.second, source.first + 16, source.second + 16)
                tile.set(x, y, x + cell, y + cell)
                canvas.drawBitmap(plains, tileSrc, tile, PixelArt.paint)
                x += cell
            }
            y += cell
        }
        if (scene == 5) {
            val plant = dp(34f)
            for (index in 0 until 4) {
                val left = width * (0.12f + index * 0.22f)
                tileSrc.set(14 * 16, 0, 15 * 16, pumpkins.height.coerceAtMost(32))
                tile.set(left, floorTop - plant * 1.6f, left + plant, floorTop - dp(4f))
                canvas.drawBitmap(pumpkins, tileSrc, tile, PixelArt.paint)
            }
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
