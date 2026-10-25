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

class FlappyDogActivity : AppCompatActivity() {
    private lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio.flap(this)
        val model = intent.getStringExtra(FoodDropActivity.EXTRA_MODEL) ?: "pug"
        setContentView(FlappyDogView(this, model, audio))
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
}

private class Gate(var x: Float, val gapTop: Float, var scored: Boolean = false)

private class FlapTheme(
    val birds: List<Bitmap>,
    val pipe: Bitmap,
    val lip: Bitmap,
    val background: Bitmap
)

/**
 * Tap to hop. Fly the dog through the gaps.
 */
private class FlappyDogView(
    context: Context,
    model: String,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val themes = (0..4).map { index ->
        FlapTheme(
            birds = listOf(
                PixelArt.load(context, "games/flap/t$index/bird0.png"),
                PixelArt.load(context, "games/flap/t$index/bird1.png"),
                PixelArt.load(context, "games/flap/t$index/bird2.png")
            ),
            pipe = PixelArt.load(context, "games/flap/t$index/pipe.png"),
            lip = PixelArt.load(context, "games/flap/t$index/lip.png"),
            background = PixelArt.load(context, "games/flap/t$index/bg.png")
        )
    }
    private var banner = ""
    private var bannerUntil = 0L
    private val src = android.graphics.Rect()
    private val tile = RectF()
    private val gates = ArrayList<Gate>()
    private val random = Random(SystemClock.uptimeMillis())

    private var dogY = 0f
    private var velocity = 0f
    private var started = false
    private var over = false
    private var score = 0
    private var best = GameScores.best(context, GameScores.BEST_FLAP)
    private var lastMs = 0L

    private val sky = Paint().apply { color = 0xFF8EC8E8.toInt() }
    private val pet = PixelArt.petStrip(context, model, "side", "games/flap/t0/bird0.png")
    private val pipe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF58CC02.toInt() }
    private val pipeLip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E8C12.toInt() }
    private val ground = Paint().apply { color = 0xFF5C8A3C.toInt() }
    private val groundEdge = Paint().apply { color = 0xFF3E6A28.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF173307.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2173307.toInt() }
    private val dogRect = RectF()

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
        if (dogY == 0f && h > 0) dogY = h * 0.42f
        if (gates.isEmpty() && w > 0 && h > 0) seedGates()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (event.x < dp(56f) && event.y < dp(56f)) {
            (context as? Activity)?.finish()
            return true
        }
        if (over) {
            restart()
            return true
        }
        started = true
        velocity = -dp(520f)
        audio.jump()
        return true
    }

    private fun restart() {
        gates.clear()
        seedGates()
        dogY = height * 0.42f
        velocity = 0f
        started = false
        over = false
        score = 0
        banner = ""
        lastMs = 0L
        postOnAnimation(frame)
    }

    private fun seedGates() {
        var x = width * 0.85f
        repeat(3) {
            gates.add(Gate(x, gapTop()))
            x += dp(230f)
        }
    }

    private fun gapTop(): Float {
        val groundTop = height - dp(72f)
        val gap = dp(168f)
        val min = dp(72f)
        val max = (groundTop - gap - dp(48f)).coerceAtLeast(min + 1f)
        return min + random.nextFloat() * (max - min)
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastMs == 0L) 0.016f else ((now - lastMs).coerceAtMost(34L)) / 1000f
        lastMs = now
        if (!over && width > 0) step(dt)
        invalidate()
        if (!over) postOnAnimation(frame)
    }

    private fun step(dt: Float) {
        if (!started) return
        velocity += dp(1500f) * dt
        dogY += velocity * dt
        val speed = dp(168f)
        val gap = dp(168f)
        val pipeW = dp(64f)
        val lip = dp(6f)
        val dogX = width * 0.28f
        val radius = birdSize() * 0.36f
        val groundTop = height - dp(72f)

        if (dogY - radius < dp(8f) || dogY + radius > groundTop) {
            endRound()
            return
        }

        for (gate in gates) {
            gate.x -= speed * dt
            if (!gate.scored && gate.x + pipeW < dogX) {
                gate.scored = true
                val previous = score
                score += 1
                audio.point()
                val reward = GameRewards.onScore(context, previous, score, 10, 6, 30, 8, "Uçuş")
                if (!reward.isNullOrBlank()) {
                    banner = reward
                    bannerUntil = SystemClock.uptimeMillis() + 1300L
                    audio.reward()
                }
                Achievements.celebrate(context, Achievements.FLAP, score)
            }
            if (hits(dogX, dogY, radius, gate.x - lip, pipeW + lip * 2f, gate.gapTop, gap, groundTop)) {
                endRound()
                return
            }
        }
        if (gates.isNotEmpty() && gates.first().x + pipeW < -dp(8f)) {
            val last = gates.last().x
            gates.removeAt(0)
            gates.add(Gate(last + dp(230f), gapTop()))
        }
    }

    private fun hits(
        cx: Float,
        cy: Float,
        radius: Float,
        pipeX: Float,
        pipeW: Float,
        gapTop: Float,
        gap: Float,
        groundTop: Float
    ): Boolean {
        return circleHitsRect(cx, cy, radius, pipeX, 0f, pipeX + pipeW, gapTop) ||
            circleHitsRect(cx, cy, radius, pipeX, gapTop + gap, pipeX + pipeW, groundTop)
    }

    private fun circleHitsRect(
        cx: Float,
        cy: Float,
        radius: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Boolean {
        val nearestX = cx.coerceIn(left, right)
        val nearestY = cy.coerceIn(top, bottom)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return dx * dx + dy * dy < radius * radius
    }

    private fun endRound() {
        if (over) return
        over = true
        audio.crash()
        best = GameScores.submit(context, GameScores.BEST_FLAP, score)
        if (score > 0) {
            PetProgress(context).snack(score.coerceAtMost(18))
            com.clipsmile.app.data.Den(context).bump(com.clipsmile.app.data.Den.Kind.FLAP, score)
        }
        Achievements.celebrate(context, Achievements.FLAP, score)
    }

    private fun birdSize() = dp(36f)

    private fun theme(): FlapTheme = themes[(score / 10) % themes.size]

    private fun drawLip(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, flipY: Boolean) {
        tile.set(left, top, right, bottom)
        val lipBmp = theme().lip
        if (!flipY) {
            canvas.drawBitmap(lipBmp, null, tile, PixelArt.paint)
            return
        }
        canvas.save()
        canvas.scale(1f, -1f, tile.centerX(), tile.centerY())
        canvas.drawBitmap(lipBmp, null, tile, PixelArt.paint)
        canvas.restore()
    }

    private fun tilePipe(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val pipeBmp = theme().pipe
        val tileH = (right - left).coerceAtLeast(1f)
        var y = top
        while (y < bottom - 0.5f) {
            val next = (y + tileH).coerceAtMost(bottom)
            tile.set(left, y, right, next)
            canvas.drawBitmap(pipeBmp, null, tile, PixelArt.paint)
            y = next
        }
    }

    override fun onDraw(canvas: Canvas) {
        val groundTop = height - dp(72f)
        tile.set(0f, 0f, width.toFloat(), groundTop)
        canvas.drawBitmap(theme().background, null, tile, PixelArt.paint)
        val pipeW = dp(64f)
        val gap = dp(168f)
        val lipH = dp(22f)
        for (gate in gates) {
            val left = gate.x
            val right = gate.x + pipeW
            tilePipe(canvas, left, 0f, right, gate.gapTop - lipH)
            drawLip(canvas, left - dp(6f), gate.gapTop - lipH, right + dp(6f), gate.gapTop, true)
            drawLip(canvas, left - dp(6f), gate.gapTop + gap, right + dp(6f), gate.gapTop + gap + lipH, false)
            tilePipe(canvas, left, gate.gapTop + gap + lipH, right, groundTop)
        }
        canvas.drawRect(0f, groundTop, width.toFloat(), height.toFloat(), ground)
        canvas.drawRect(0f, groundTop, width.toFloat(), groundTop + dp(8f), groundEdge)

        val dogX = if (width == 0) 0f else width * 0.28f
        val size = birdSize()
        dogRect.set(dogX - size / 2f, dogY - size / 2f, dogX + size / 2f, dogY + size / 2f)
        val frameW = pet.width / 6
        val aspect = frameW / pet.height.coerceAtLeast(1).toFloat()
        var petH = dp(50f)
        var petW = petH * aspect
        if (petW > dp(68f)) {
            petW = dp(68f)
            petH = petW / aspect.coerceAtLeast(0.25f)
        }
        if (petH > petW * 1.35f) petH = petW * 1.35f
        val frame = if (velocity < -dp(40f)) 2 else ((SystemClock.uptimeMillis() / 90L) % 6L).toInt()
        tile.set(dogX - petW / 2f, dogY - petH / 2f, dogX + petW / 2f, dogY + petH / 2f)
        canvas.save()
        canvas.rotate(velocity.coerceIn(-dp(700f), dp(900f)) / dp(40f), dogX, dogY)
        PixelArt.blitSmooth(canvas, pet, frame, frameW, tile, src)
        canvas.restore()

        ink.color = 0xFF173307.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        PixelArt.drawScore(canvas, context, score, width / 2f, dp(8f), ::dp)
        if (banner.isNotEmpty() && SystemClock.uptimeMillis() < bannerUntil) {
            PixelArt.drawBanner(canvas, banner, width / 2f, dp(60f), ::dp)
        }

        if (!started && !over) {
            ink.textSize = dp(16f)
            canvas.drawText("Dokun, zıpla", width / 2f, height * 0.58f, ink)
        }
        if (over) {
            val top = height * 0.32f
            canvas.drawRoundRect(dp(32f), top, width - dp(32f), top + dp(130f), dp(18f), dp(18f), card)
            ink.color = 0xFFF6E2B8.toInt()
            ink.textSize = dp(22f)
            canvas.drawText("Skor  $score", width / 2f, top + dp(46f), ink)
            ink.textSize = dp(15f)
            canvas.drawText("En iyi  $best", width / 2f, top + dp(74f), ink)
            canvas.drawText("Dokun, yeniden oyna", width / 2f, top + dp(102f), ink)
        }
    }

    private fun raster(res: Int, size: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, res)
            ?: return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
