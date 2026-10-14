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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val portrait = intent.getIntExtra(FoodDropActivity.EXTRA_PORTRAIT, R.drawable.ic_pug_icon)
        setContentView(FlappyDogView(this, portrait))
    }
}

private class Gate(var x: Float, val gapTop: Float, var scored: Boolean = false)

/**
 * Tap to hop. Fly the dog through the gaps.
 */
private class FlappyDogView(
    context: Context,
    portraitRes: Int
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val dogBmp = raster(portraitRes, dp(64f).toInt())
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
    private val pipe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF58CC02.toInt() }
    private val pipeLip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E8C12.toInt() }
    private val ground = Paint().apply { color = 0xFFE7C98A.toInt() }
    private val groundEdge = Paint().apply { color = 0xFFC9843A.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF173307.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2173307.toInt() }
    private val dogRect = RectF()

    private val frame = Runnable { tick() }

    init {
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
        val pipeW = dp(68f)
        val dogX = width * 0.28f
        val radius = dogBmp.width * 0.36f
        val groundTop = height - dp(72f)

        if (dogY - radius < dp(8f) || dogY + radius > groundTop) {
            endRound()
            return
        }

        for (gate in gates) {
            gate.x -= speed * dt
            if (!gate.scored && gate.x + pipeW < dogX) {
                gate.scored = true
                score += 1
            }
            if (hits(dogX, dogY, radius, gate.x, pipeW, gate.gapTop, gap, groundTop)) {
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
        best = GameScores.submit(context, GameScores.BEST_FLAP, score)
        if (score > 0) PetProgress(context).snack(score.coerceAtMost(18))
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sky)
        val groundTop = height - dp(72f)
        val pipeW = dp(68f)
        val gap = dp(168f)
        val lip = dp(16f)
        for (gate in gates) {
            canvas.drawRect(gate.x, 0f, gate.x + pipeW, gate.gapTop, pipe)
            canvas.drawRect(gate.x - dp(4f), gate.gapTop - lip, gate.x + pipeW + dp(4f), gate.gapTop, pipeLip)
            canvas.drawRect(gate.x, gate.gapTop + gap, gate.x + pipeW, groundTop, pipe)
            canvas.drawRect(
                gate.x - dp(4f),
                gate.gapTop + gap,
                gate.x + pipeW + dp(4f),
                gate.gapTop + gap + lip,
                pipeLip
            )
        }
        canvas.drawRect(0f, groundTop, width.toFloat(), height.toFloat(), ground)
        canvas.drawRect(0f, groundTop, width.toFloat(), groundTop + dp(8f), groundEdge)

        val dogX = if (width == 0) 0f else width * 0.28f
        dogRect.set(
            dogX - dogBmp.width / 2f,
            dogY - dogBmp.height / 2f,
            dogX + dogBmp.width / 2f,
            dogY + dogBmp.height / 2f
        )
        canvas.save()
        canvas.rotate(velocity.coerceIn(-dp(700f), dp(900f)) / dp(40f), dogX, dogY)
        canvas.drawBitmap(dogBmp, dogRect.left, dogRect.top, null)
        canvas.restore()

        ink.color = 0xFF173307.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        ink.textAlign = Paint.Align.CENTER
        ink.textSize = dp(32f)
        canvas.drawText(score.toString(), width / 2f, dp(52f), ink)

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
