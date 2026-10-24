package com.clipsmile.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
        val model = intent.getStringExtra(FoodDropActivity.EXTRA_MODEL) ?: "pug"
        setContentView(SwordGateView(this, model, audio))
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
    model: String,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val pet = PixelArt.petStrip(context, model, "side", "games/swords/warrior_run.png")
    private val foeRun = PixelArt.load(context, "games/swords/foe_run.png")
    private val archerRun = PixelArt.load(context, "games/swords/archer_run.png")
    private val archerShoot = PixelArt.load(context, "games/swords/archer_shoot.png")
    private val arrow = PixelArt.load(context, "games/swords/arrow.png")
    private val grass = PixelArt.load(context, "games/swords/grass.png")
    private val castle = PixelArt.load(context, "games/swords/castle.png")
    private val tree = PixelArt.load(context, "games/swords/tree.png")
    private val cloud = PixelArt.load(context, "games/swords/cloud.png")
    private val dust = PixelArt.load(context, "games/swords/dust.png")
    private val grounds = (0..4).map { PixelArt.load(context, "games/swords/maps/ground$it.png") }
    private val jumpBtn = PixelArt.load(context, "games/ui/jump.png")

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
    private var hp = 3
    private var immune = 0f
    private var lift = 0f
    private var jumpV = 0f
    private var banner = ""
    private var bannerUntil = 0L

    private val sky = Paint().apply { color = 0xFF8EC8E8.toInt() }
    private val hillFar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9FCB86.toInt() }
    private val hillNear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E9A48.toInt() }
    private val hill = Path()
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
        if (jumpRect().contains(event.x, event.y)) hop() else swing()
        return true
    }

    private fun hop() {
        started = true
        if (lift > dp(4f)) return
        jumpV = dp(980f)
        audio.jump()
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
        hp = 3
        immune = 0f
        lift = 0f
        jumpV = 0f
        banner = ""
        scroll = 0f
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
        if (started) scroll += dp(42f) * dt
        if (attackLeft > 0f) attackLeft -= dt
        if (immune > 0f) immune -= dt
        jumpV -= dp(1900f) * dt
        lift = (lift + jumpV * dt).coerceAtLeast(0f)
        if (lift == 0f && jumpV < 0f) jumpV = 0f
        for (puff in puffs) puff.life -= dt
        puffs.removeAll { it.life <= 0f }
        if (!started) return

        spawnIn -= dt
        if (spawnIn <= 0f) {
            spawnIn = (1.25f - score * 0.02f).coerceAtLeast(0.62f)
            val archer = score >= 2 && random.nextFloat() < 0.38f
            foes.add(Foe(width + cell() * 1.4f, archer))
        }

        val pace = dp(78f) + score * dp(1.6f)
        val hero = heroRect()
        val heroBody = heroBody(hero)
        val foeSize = cell() * 0.92f
        val stop = heroBody.right + dp(46f) - foeSize * 0.32f
        val slash = if (attackLeft in 0.08f..0.24f) slashRect(heroBody) else null
        val dead = ArrayList<Foe>()
        val armed = clock > 0.55f
        for (foe in foes) {
            if (foe.archer && foe.x <= stop) {
                foe.shooting = true
                foe.x = stop
                foe.shootIn -= dt
                if (foe.shootIn <= 0f) {
                    foe.shootIn = 1.35f
                    val chest = foeBody(foe)
                    bolts.add(Bolt(chest.left, chest.centerY()))
                }
            } else {
                foe.x -= pace * dt
            }
            val body = foeBody(foe)
            val beside = body.left < heroBody.right + dp(22f) &&
                body.left > heroBody.right - dp(10f) &&
                body.bottom > heroBody.top &&
                body.top < heroBody.bottom
            val touching = body.left < heroBody.right - dp(4f) &&
                body.right > heroBody.left + dp(8f) &&
                body.bottom > heroBody.top + dp(6f) &&
                body.top < heroBody.bottom - dp(4f)
            if (slash != null && beside) {
                dead.add(foe)
                puffs.add(Puff(body.centerX(), body.centerY()))
                val previous = score
                score += 1
                audio.point()
                val reward = GameRewards.onScore(context, previous, score, 150, 8, 300, 12, "Kılıç")
                if (!reward.isNullOrBlank()) {
                    banner = reward
                    bannerUntil = SystemClock.uptimeMillis() + 1300L
                    audio.reward()
                }
                Achievements.celebrate(context, Achievements.SWORD, score)
            } else if (armed && !foe.archer && touching) {
                if (wound()) return
            }
        }
        foes.removeAll(dead)

        val spent = ArrayList<Bolt>()
        for (bolt in bolts) {
            bolt.x -= dp(240f) * dt
            val box = boltRect(bolt)
            if (slash != null && RectF.intersects(slash, box)) {
                spent.add(bolt)
                puffs.add(Puff(box.centerX(), box.centerY()))
                audio.point()
            } else if (armed && RectF.intersects(box, heroBody)) {
                if (wound()) return
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
        val frameW = pet.width / 6f
        val aspect = frameW / pet.height.coerceAtLeast(1)
        val maxH = cell() * 0.92f
        val maxW = cell() * 1.05f
        var h = maxH
        var w = h * aspect
        if (w > maxW) {
            w = maxW
            h = w / aspect.coerceAtLeast(0.25f)
        }
        if (h > w * 1.35f) h = w * 1.35f
        val bottom = groundTop() - lift
        val left = width * 0.16f
        return RectF(left, bottom - h, left + w, bottom)
    }

    private fun heroBody(hero: RectF): RectF {
        return RectF(
            hero.left + hero.width() * 0.22f,
            hero.top + hero.height() * 0.22f,
            hero.right - hero.width() * 0.12f,
            hero.bottom - hero.height() * 0.04f
        )
    }

    /** The drawn warrior has wide empty padding. Hits use the opaque middle. */
    private fun foeBody(foe: Foe): RectF {
        val full = foeRect(foe)
        val feet = full.bottom - full.height() * 0.286f
        val inset = full.width() * 0.32f
        return RectF(full.left + inset, feet - full.height() * 0.40f, full.right - inset * 0.75f, feet)
    }

    private fun wound(): Boolean {
        if (immune > 0f) return false
        hp -= 1
        immune = 0.75f
        if (hp <= 0) {
            endRound()
            return true
        }
        audio.hurt()
        return false
    }

    private fun planted(left: Float, size: Float, stand: Float): RectF {
        val bottom = stand + size * 0.286f
        return RectF(left, bottom - size, left + size, bottom)
    }

    private fun jumpRect(): RectF {
        val w = dp(112f)
        val h = dp(42f)
        return RectF(width - w - dp(14f), height - h - dp(16f), width - dp(14f), height - dp(16f))
    }

    private fun slashRect(heroBody: RectF): RectF {
        return RectF(
            heroBody.right - dp(8f),
            heroBody.top + heroBody.height() * 0.34f,
            heroBody.right + dp(18f),
            heroBody.bottom - heroBody.height() * 0.22f
        )
    }

    private fun heroInset(hero: RectF): RectF {
        val inset = hero.width() * 0.28f
        return RectF(hero.left + inset, hero.top + inset, hero.right - inset * 0.4f, hero.bottom - inset * 0.2f)
    }

    private fun foeRect(foe: Foe): RectF {
        return planted(foe.x, cell() * 0.92f, groundTop())
    }

    private fun boltRect(bolt: Bolt): RectF {
        val w = cell() * 0.22f
        return RectF(bolt.x, bolt.y - w * 0.35f, bolt.x + w, bolt.y + w * 0.35f)
    }

    override fun onDraw(canvas: Canvas) {
        val phase = (score % 100) / 100f
        val night = kotlin.math.sin(phase * Math.PI).toFloat()
        sky.color = blend(0xFF8EC8E8.toInt(), 0xFF1B2438.toInt(), night)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sky)
        val ground = if (height == 0) 0f else groundTop()
        drawClouds(canvas)
        drawHills(canvas, ground)
        drawTrees(canvas, ground)
        val castleH = cell() * 1.85f
        val castleW = castleH * castle.width / castle.height.coerceAtLeast(1)
        val castleBottom = ground + castleH * 0.06f
        val castleLeft = dp(10f) - scroll
        drawSheet(canvas, castle, 0, castle.width, RectF(
            castleLeft,
            castleBottom - castleH,
            castleLeft + castleW,
            castleBottom
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
            val at = foeRect(foe)
            canvas.save()
            canvas.scale(-1f, 1f, at.centerX(), at.centerY())
            drawSheet(canvas, sheet, stepFrame, frames, at)
            canvas.restore()
        }
        for (bolt in bolts) {
            val box = boltRect(bolt)
            canvas.save()
            canvas.scale(-1f, 1f, box.centerX(), box.centerY())
            drawSheet(canvas, arrow, 0, arrow.width, box)
            canvas.restore()
        }
        for (puff in puffs) {
            val size = cell() * 0.4f
            dest.set(puff.x - size / 2f, puff.y - size / 2f, puff.x + size / 2f, puff.y + size / 2f)
            drawSheet(canvas, dust, ((0.35f - puff.life) * 20f).toInt(), 64, dest)
        }
        val hero = heroRect()
        val index = if (attackLeft > 0f) 4 else stepFrame
        drawSheet(canvas, pet, index, pet.width / 6, hero)
        if (night > 0.04f) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                color = (night * 110f).toInt().shl(24)
            })
        }

        ink.color = 0xFF173307.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        PixelArt.drawScore(canvas, score, width / 2f, dp(8f), ::dp)
        PixelArt.drawHealth(canvas, hp, 3, width - dp(12f), dp(18f), ::dp)
        if (banner.isNotEmpty() && SystemClock.uptimeMillis() < bannerUntil) {
            PixelArt.drawBanner(canvas, banner, width / 2f, dp(60f), ::dp)
        }
        val button = jumpRect()
        canvas.drawBitmap(jumpBtn, null, button, PixelArt.paint)
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

    private fun drawHills(canvas: Canvas, ground: Float) {
        val far = ground - cell() * 0.42f
        hill.reset()
        hill.moveTo(-dp(20f), ground)
        var x = -dp(40f) - (scroll * 0.08f % dp(180f))
        while (x < width + dp(200f)) {
            hill.quadTo(x + dp(50f), far - dp(18f), x + dp(100f), far)
            hill.quadTo(x + dp(150f), far + dp(26f), x + dp(200f), far - dp(6f))
            x += dp(200f)
        }
        hill.lineTo(width + dp(40f), ground + dp(8f))
        hill.close()
        canvas.drawPath(hill, hillFar)
        hill.reset()
        hill.moveTo(-dp(20f), ground)
        x = -dp(20f) - (scroll * 0.18f % dp(160f))
        val near = ground - cell() * 0.16f
        while (x < width + dp(180f)) {
            hill.quadTo(x + dp(40f), near - dp(10f), x + dp(80f), near)
            hill.quadTo(x + dp(120f), near + dp(16f), x + dp(160f), near)
            x += dp(160f)
        }
        hill.lineTo(width + dp(40f), height.toFloat())
        hill.lineTo(-dp(20f), height.toFloat())
        hill.close()
        canvas.drawPath(hill, hillNear)
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
        val map = grounds[(score / 100) % grounds.size]
        val tile = cell() * 0.42f
        val shift = scroll % tile
        var x = -shift
        while (x < width) {
            dest.set(x, ground, x + tile, ground + tile)
            canvas.drawBitmap(map, null, dest, PixelArt.paint)
            dest.set(x, ground - tile * 0.28f, x + tile, ground + tile * 0.2f)
            canvas.drawBitmap(grass, null, dest, PixelArt.paint)
            x += tile
        }
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        fun ch(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * u).toInt()
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0) or 0xFF000000.toInt()
    }

    private fun drawSheet(canvas: Canvas, sheet: Bitmap, index: Int, frameWidth: Int, at: RectF) {
        PixelArt.blit(canvas, sheet, index, frameWidth, at, src)
    }
}
