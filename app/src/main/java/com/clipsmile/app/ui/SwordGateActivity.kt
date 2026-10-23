package com.clipsmile.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.clipsmile.app.R
import com.clipsmile.app.data.Den
import com.clipsmile.app.data.PetProgress
import kotlin.random.Random

/**
 * A lane in front of the blue castle. Tap to swing.
 * Red warriors walk in; red archers stop and loose arrows.
 * Pixel Frog's free Tiny Swords pack supplies the pictures.
 */
class SwordGateActivity : AppCompatActivity() {
    private lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio.swords(this)
        setContentView(SwordGateView(this, audio))
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

private class Foe(
    var x: Float,
    val archer: Boolean,
    var shooting: Boolean = false,
    var shootIn: Float = 0.8f
)

private class Bolt(var x: Float, var y: Float)

private class Puff(var x: Float, var y: Float, var life: Float = 0.35f)

private class SwordGateView(
    context: Context,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val idle = PixelArt.load(context, "games/swords/warrior_idle.png")
    private val run = PixelArt.load(context, "games/swords/warrior_run.png")
    private val attack = PixelArt.load(context, "games/swords/warrior_attack.png")
    private val foeRun = PixelArt.load(context, "games/swords/foe_run.png")
    private val archerRun = PixelArt.load(context, "games/swords/archer_run.png")
    private val archerShoot = PixelArt.load(context, "games/swords/archer_shoot.png")
    private val arrow = PixelArt.load(context, "games/swords/arrow.png")
    private val grass = PixelArt.load(context, "games/swords/grass.png")
    private val castle = PixelArt.load(context, "games/swords/castle.png")
    private val tree = PixelArt.load(context, "games/swords/tree.png")
    private val cloud = PixelArt.load(context, "games/swords/cloud.png")
    private val dust = PixelArt.load(context, "games/swords/dust.png")

    private val foes = ArrayList<Foe>()
    private val bolts = ArrayList<Bolt>()
    private val puffs = ArrayList<Puff>()
    private val random = Random(SystemClock.uptimeMillis())
    private val src = Rect()
    private val dest = RectF()

    private var started = false
    private var over = false
    private var score = 0
    private var best = GameScores.best(context, GameScores.BEST_SWORD)
    private var attackLeft = 0f
    private var spawnIn = 0.4f
    private var scroll = 0f
    private var clock = 0f
    private var lastMs = 0L

    private val sky = Paint().apply { color = 0xFF8EC8E8.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF173307.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2173307.toInt() }
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
        swing()
        return true
    }

    private fun swing() {
        started = true
        if (attackLeft > 0f) return
        attackLeft = 0.36f
        audio.jump()
    }

    private fun restart() {
        foes.clear()
        bolts.clear()
        puffs.clear()
        started = false
        over = false
        score = 0
        attackLeft = 0f
        spawnIn = 0.4f
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

    private fun step(dt: Float) {
        scroll += dp(28f) * dt
        if (attackLeft > 0f) attackLeft -= dt
        for (puff in puffs) puff.life -= dt
        puffs.removeAll { it.life <= 0f }
        if (!started) return

        spawnIn -= dt
        if (spawnIn <= 0f) {
            spawnIn = (1.25f - score * 0.02f).coerceAtLeast(0.62f)
            val archer = score >= 2 && random.nextFloat() < 0.38f
            foes.add(Foe(width + cell(), archer))
        }

        val pace = dp(78f) + score * dp(1.6f)
        val stop = width * 0.70f
        val hero = heroRect()
        val slash = if (attackLeft in 0.08f..0.24f) slashRect(hero) else null
        val dead = ArrayList<Foe>()
        for (foe in foes) {
            if (foe.archer && foe.x <= stop) {
                foe.shooting = true
                foe.x = stop
                foe.shootIn -= dt
                if (foe.shootIn <= 0f) {
                    foe.shootIn = 1.35f
                    val body = foeRect(foe)
                    bolts.add(Bolt(body.left, body.centerY()))
                }
            } else {
                foe.x -= pace * dt
            }
            val body = foeRect(foe)
            if (slash != null && RectF.intersects(slash, body)) {
                dead.add(foe)
                puffs.add(Puff(body.centerX(), body.centerY()))
                score += 1
                audio.point()
                Achievements.celebrate(context, Achievements.SWORD, score)
            } else if (body.left < hero.centerX()) {
                endRound()
                return
            }
        }
        foes.removeAll(dead)

        val spent = ArrayList<Bolt>()
        for (bolt in bolts) {
            bolt.x -= dp(220f) * dt
            val box = boltRect(bolt)
            if (slash != null && RectF.intersects(slash, box)) {
                spent.add(bolt)
                puffs.add(Puff(box.centerX(), box.centerY()))
                audio.point()
            } else if (RectF.intersects(box, heroInset(hero))) {
                endRound()
                return
            } else if (bolt.x < -dp(20f)) {
                spent.add(bolt)
            }
        }
        bolts.removeAll(spent)
    }

    private fun endRound() {
        if (over) return
        over = true
        audio.crash()
        best = GameScores.submit(context, GameScores.BEST_SWORD, score)
        if (score > 0) {
            PetProgress(context).snack(score.coerceAtMost(12))
            Den(context).bump(Den.Kind.SWORD, score)
        }
        Achievements.celebrate(context, Achievements.SWORD, score)
    }

    private fun cell() = height * 0.22f

    private fun groundTop() = height - cell() * 0.55f

    private fun heroRect(): RectF {
        val size = cell()
        val left = width * 0.16f
        val bottom = groundTop()
        return RectF(left, bottom - size, left + size, bottom)
    }

    private fun slashRect(hero: RectF): RectF {
        return RectF(
            hero.centerX(),
            hero.top + hero.height() * 0.28f,
            hero.right + hero.width() * 0.28f,
            hero.bottom - hero.height() * 0.18f
        )
    }

    private fun heroInset(hero: RectF): RectF {
        val inset = hero.width() * 0.28f
        return RectF(hero.left + inset, hero.top + inset, hero.right - inset * 0.4f, hero.bottom - inset * 0.2f)
    }

    private fun foeRect(foe: Foe): RectF {
        val size = cell() * 0.92f
        return RectF(foe.x, groundTop() - size, foe.x + size, groundTop())
    }

    private fun boltRect(bolt: Bolt): RectF {
        val w = cell() * 0.22f
        return RectF(bolt.x, bolt.y - w * 0.35f, bolt.x + w, bolt.y + w * 0.35f)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sky)
        val ground = if (height == 0) 0f else groundTop()
        drawClouds(canvas)
        drawTrees(canvas, ground)
        drawSheet(canvas, castle, 0, castle.width, RectF(
            width * 0.02f,
            ground - cell() * 1.55f,
            width * 0.02f + cell() * 1.7f,
            ground
        ))
        drawGrass(canvas, ground)
        val stepFrame = (clock * 10f).toInt()
        for (foe in foes) {
            val sheet: Bitmap
            val frames: Int
            if (foe.archer && foe.shooting) {
                sheet = archerShoot
                frames = 192
            } else if (foe.archer) {
                sheet = archerRun
                frames = 192
            } else {
                sheet = foeRun
                frames = 192
            }
            drawSheet(canvas, sheet, stepFrame, frames, foeRect(foe))
        }
        for (bolt in bolts) drawSheet(canvas, arrow, 0, arrow.width, boltRect(bolt))
        for (puff in puffs) {
            val size = cell() * 0.4f
            dest.set(puff.x - size / 2f, puff.y - size / 2f, puff.x + size / 2f, puff.y + size / 2f)
            drawSheet(canvas, dust, ((0.35f - puff.life) * 20f).toInt(), 64, dest)
        }
        val hero = heroRect()
        val sheet = when {
            attackLeft > 0f -> attack
            started -> run
            else -> idle
        }
        val frameW = if (sheet === attack) 192 else 192
        val index = if (attackLeft > 0f) ((0.36f - attackLeft) * 12f).toInt() else stepFrame
        drawSheet(canvas, sheet, index, frameW, hero)

        ink.color = 0xFF173307.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        ink.textAlign = Paint.Align.CENTER
        ink.textSize = dp(32f)
        canvas.drawText(score.toString(), width / 2f, dp(52f), ink)
        if (!started && !over) {
            ink.textSize = dp(16f)
            canvas.drawText(context.getString(R.string.game_sword_hint), width / 2f, height * 0.58f, ink)
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

    private fun drawClouds(canvas: Canvas) {
        val w = width * 0.42f
        val h = w * cloud.height / cloud.width
        val shift = scroll * 0.15f % (width + w)
        dest.set(-shift, dp(18f), -shift + w, dp(18f) + h)
        canvas.drawBitmap(cloud, null, dest, PixelArt.paint)
        dest.offset(width * 0.7f, dp(36f))
        canvas.drawBitmap(cloud, null, dest, PixelArt.paint)
    }

    private fun drawTrees(canvas: Canvas, ground: Float) {
        val h = cell() * 1.15f
        val w = h * (192f / 256f)
        val gap = w * 2.4f
        val shift = scroll * 0.35f % gap
        var x = -shift
        var index = 0
        while (x < width + w) {
            dest.set(x, ground - h, x + w, ground)
            PixelArt.blit(canvas, tree, index, 192, dest, src)
            x += gap
            index += 1
        }
    }

    private fun drawGrass(canvas: Canvas, ground: Float) {
        val tile = cell() * 0.42f
        val shift = scroll % tile
        var x = -shift
        while (x < width) {
            dest.set(x, ground - tile * 0.35f, x + tile, height.toFloat())
            canvas.drawBitmap(grass, null, dest, PixelArt.paint)
            x += tile
        }
    }

    private fun drawSheet(canvas: Canvas, sheet: Bitmap, index: Int, frameWidth: Int, at: RectF) {
        PixelArt.blit(canvas, sheet, index, frameWidth, at, src)
    }
}
