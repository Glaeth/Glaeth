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
import kotlin.math.sin
import kotlin.random.Random

/**
 * Endless run: the dog keeps moving and a tap jumps over park obstacles.
 * Same idea as the offline runner, drawn as this app's own dog.
 */
class RunnerActivity : AppCompatActivity() {
    private lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio.run(this)
        val portrait = intent.getIntExtra(FoodDropActivity.EXTRA_PORTRAIT, R.drawable.ic_pug_icon)
        setContentView(RunnerView(this, portrait, audio))
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

private class Hurdle(
    var x: Float,
    val kind: Int,
    val width: Float,
    val height: Float,
    val lift: Float
)

private class RunnerView(
    context: Context,
    portraitRes: Int,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val dogBmp = raster(portraitRes, dp(78f).toInt())
    private val hurdles = ArrayList<Hurdle>()
    private val random = Random(SystemClock.uptimeMillis())

    private var feet = 0f
    private var velocity = 0f
    private var onGround = true
    private var started = false
    private var over = false
    private var score = 0
    private var best = GameScores.best(context, GameScores.BEST_RUN)
    private var travel = 0f
    private var spawnIn = 0.7f
    private var clock = 0f
    private var lastMs = 0L

    private val skyPaint = Paint()
    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8FCB6A.toInt() }
    private val hillFar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB7D98A.toInt() }
    private val groundPaint = Paint().apply { color = 0xFFE7C98A.toInt() }
    private val groundEdge = Paint().apply { color = 0xFFC9843A.toInt() }
    private val tickPaint = Paint().apply { color = 0xFFD7B56A.toInt() }
    private val bushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E8C12.toInt() }
    private val bushDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2F6C0E.toInt() }
    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B2918.toInt() }
    private val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF6E2B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
    }
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD56A.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF23B2918.toInt() }
    private val oval = RectF()

    private val frame = Runnable { tick() }

    init {
        keepScreenOn = true
        setBackgroundColor(0xFFB9E3F6.toInt())
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
        if (!started && h > 0) feet = groundTop()
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
        jump()
        return true
    }

    private fun groundTop() = height - dp(28f)

    private fun jump() {
        if (!onGround && started) return
        started = true
        onGround = false
        velocity = -dp(860f)
        audio.jump()
    }

    private fun restart() {
        hurdles.clear()
        feet = groundTop()
        velocity = 0f
        onGround = true
        started = false
        over = false
        score = 0
        travel = 0f
        spawnIn = 0.7f
        lastMs = 0L
        postOnAnimation(frame)
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastMs == 0L) 0.016f else ((now - lastMs).coerceAtMost(34L)) / 1000f
        lastMs = now
        clock += dt
        if (!over && width > 0 && height > 0) step(dt)
        invalidate()
        if (!over) postOnAnimation(frame)
    }

    private fun speed(): Float = (dp(340f) + score * dp(0.42f)).coerceAtMost(dp(640f))

    private fun step(dt: Float) {
        if (!started) return
        val ground = groundTop()
        velocity += dp(3100f) * dt
        feet += velocity * dt
        if (feet >= ground) {
            feet = ground
            velocity = 0f
            onGround = true
        }
        val pace = speed()
        travel += pace * dt
        val previous = score
        score = (travel / dp(16f)).toInt()
        if (score / 100 > previous / 100) audio.point()
        if (crossed(previous, score, 1) || crossed(previous, score, 100) || crossed(previous, score, 500)) {
            Achievements.celebrate(context, Achievements.RUN, score)
        }

        spawnIn -= dt
        if (spawnIn <= 0f) {
            spawnIn = (1.15f - score * 0.0014f).coerceAtLeast(0.72f)
            spawn(pace)
        }
        val dogHit = dogBox()
        for (hurdle in hurdles) {
            hurdle.x -= pace * dt
            if (hits(dogHit, hurdle, ground)) {
                endRound()
                return
            }
        }
        hurdles.removeAll { it.x + it.width < -dp(8f) }
    }

    private fun crossed(previous: Int, now: Int, mark: Int) = previous < mark && now >= mark

    private fun spawn(pace: Float) {
        val roll = random.nextFloat()
        val hurdle = when {
            score > 180 && roll < 0.16f -> Hurdle(width + dp(8f), 1, dp(46f), dp(22f), dp(102f))
            score > 70 && roll < 0.32f -> Hurdle(width + dp(8f), 1, dp(44f), dp(26f), dp(6f))
            roll < 0.45f -> Hurdle(width + dp(8f), 0, dp(34f), dp(46f), 0f)
            else -> Hurdle(width + dp(8f), 0, dp(58f), dp(62f), 0f)
        }
        val last = hurdles.lastOrNull()
        if (last != null && last.x > width - pace * 0.45f) {
            hurdle.x = last.x + pace * 0.85f
        }
        hurdles.add(hurdle)
    }

    private fun dogBox(): RectF {
        val width = dogBmp.width * 0.42f
        val height = dogBmp.height * 0.58f
        val left = dogX() - width / 2f
        val bottom = feet - dp(4f)
        return RectF(left, bottom - height, left + width, bottom)
    }

    private fun hits(dog: RectF, hurdle: Hurdle, ground: Float): Boolean {
        val box = RectF(
            hurdle.x + hurdle.width * 0.16f,
            ground - hurdle.lift - hurdle.height,
            hurdle.x + hurdle.width * 0.84f,
            ground - hurdle.lift - hurdle.height * 0.08f
        )
        return dog.intersects(box)
    }

    private fun RectF.intersects(other: RectF): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    private fun dogX() = if (width == 0) 0f else width * 0.24f

    private fun endRound() {
        if (over) return
        over = true
        audio.crash()
        best = GameScores.submit(context, GameScores.BEST_RUN, score)
        if (score > 0) {
            PetProgress(context).snack((score / 25).coerceIn(1, 18))
            com.clipsmile.app.data.Den(context).bump(com.clipsmile.app.data.Den.Kind.RUN, score)
        }
        Achievements.celebrate(context, Achievements.RUN, score)
    }

    override fun onDraw(canvas: Canvas) {
        val night = ((score - 220) / 300f).coerceIn(0f, 1f)
        skyPaint.color = blend(0xFFB9E3F6, 0xFF1C2433, night)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)
        sunPaint.color = blend(0xFFFFD56A, 0xFFF4F1E8, night)
        canvas.drawCircle(width * 0.82f, dp(64f), dp(22f), sunPaint)

        val ground = if (height == 0) 0f else groundTop()
        val period = (width * 0.85f).coerceAtLeast(dp(180f))
        val shift = if (period == 0f) 0f else (travel * 0.18f) % period
        hillFar.color = blend(0xFFC6E3A4, 0xFF2C3A32, night)
        hillPaint.color = blend(0xFF8FCB6A, 0xFF243028, night)
        for (index in -1..3) {
            val center = index * period - shift
            canvas.drawCircle(center + dp(20f), ground + dp(16f), dp(100f), hillFar)
            canvas.drawCircle(center + period * 0.48f, ground + dp(24f), dp(78f), hillPaint)
        }

        groundPaint.color = blend(0xFFE7C98A, 0xFF8C7048, night)
        groundEdge.color = blend(0xFFC9843A, 0xFF5C4632, night)
        canvas.drawRect(0f, ground, width.toFloat(), height.toFloat(), groundPaint)
        canvas.drawRect(0f, ground, width.toFloat(), ground + dp(7f), groundEdge)
        val gap = dp(24f)
        var tick = -(travel % gap)
        while (tick < width) {
            canvas.drawRect(tick, ground + dp(16f), tick + dp(10f), ground + dp(20f), tickPaint)
            tick += gap
        }

        for (hurdle in hurdles) {
            if (hurdle.kind == 0) drawBush(canvas, hurdle, ground) else drawBird(canvas, hurdle, ground)
        }

        val bob = when {
            !onGround -> 0f
            started -> sin(clock * 16f) * dp(2.5f)
            else -> sin(clock * 3f) * dp(2f)
        }
        val dx = dogX()
        val top = feet - dogBmp.height + bob
        canvas.save()
        canvas.rotate((velocity / dp(48f)).coerceIn(-26f, 22f), dx, top + dogBmp.height / 2f)
        canvas.drawBitmap(dogBmp, dx - dogBmp.width / 2f, top, null)
        canvas.restore()

        ink.color = if (night > 0.55f) 0xFFF6E2B8.toInt() else 0xFF3B2918.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        ink.textAlign = Paint.Align.CENTER
        ink.textSize = dp(28f)
        canvas.drawText(score.toString(), width / 2f, dp(48f), ink)

        if (!started && !over) {
            ink.textSize = dp(16f)
            canvas.drawText(context.getString(R.string.game_run_hint), width / 2f, height * 0.62f, ink)
        }
        if (over) {
            val topCard = height * 0.32f
            canvas.drawRoundRect(dp(32f), topCard, width - dp(32f), topCard + dp(130f), dp(18f), dp(18f), card)
            ink.color = 0xFFF6E2B8.toInt()
            ink.textSize = dp(22f)
            canvas.drawText("Skor  $score", width / 2f, topCard + dp(46f), ink)
            ink.textSize = dp(15f)
            canvas.drawText("En iyi  $best", width / 2f, topCard + dp(74f), ink)
            canvas.drawText("Dokun, yeniden oyna", width / 2f, topCard + dp(102f), ink)
        }
    }

    private fun drawBush(canvas: Canvas, hurdle: Hurdle, ground: Float) {
        val left = hurdle.x
        val right = hurdle.x + hurdle.width
        val top = ground - hurdle.height
        canvas.drawCircle(left + hurdle.width * 0.38f, top + hurdle.height * 0.48f, hurdle.height * 0.42f, bushPaint)
        canvas.drawCircle(left + hurdle.width * 0.68f, top + hurdle.height * 0.58f, hurdle.height * 0.32f, bushDark)
        canvas.drawRoundRect(left, ground - hurdle.height * 0.34f, right, ground, dp(6f), dp(6f), bushDark)
    }

    private fun drawBird(canvas: Canvas, hurdle: Hurdle, ground: Float) {
        val bodyTop = ground - hurdle.lift - hurdle.height
        oval.set(hurdle.x, bodyTop, hurdle.x + hurdle.width, bodyTop + hurdle.height)
        canvas.drawOval(oval, birdPaint)
        canvas.drawCircle(hurdle.x + hurdle.width * 0.78f, bodyTop + hurdle.height * 0.35f, hurdle.height * 0.28f, birdPaint)
        val flap = sin(clock * 18f) * hurdle.height * 0.55f
        canvas.drawLine(
            hurdle.x + hurdle.width * 0.4f,
            bodyTop + hurdle.height * 0.4f,
            hurdle.x + hurdle.width * 0.05f,
            bodyTop + hurdle.height * 0.2f + flap,
            wingPaint
        )
    }

    private fun blend(from: Long, to: Long, amount: Float): Int {
        val start = from.toInt()
        val end = to.toInt()
        val inverse = 1f - amount
        val a = ((start ushr 24) * inverse + (end ushr 24) * amount).toInt()
        val r = (((start shr 16) and 0xFF) * inverse + ((end shr 16) and 0xFF) * amount).toInt()
        val g = (((start shr 8) and 0xFF) * inverse + ((end shr 8) and 0xFF) * amount).toInt()
        val b = ((start and 0xFF) * inverse + (end and 0xFF) * amount).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
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
