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
    var shootIn: Float = 0.8f,
    var slowLeft: Float = 0f,
    var rootLeft: Float = 0f
)

private class Bolt(var x: Float, var y: Float)

private class Spark(
    var x: Float,
    var y: Float,
    var vx: Float,
    val skill: String,
    var life: Float,
    var hits: Int
)

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

    private val foes = ArrayList<Foe>()
    private val bolts = ArrayList<Bolt>()
    private val puffs = ArrayList<Puff>()
    private val sparks = ArrayList<Spark>()
    private val pending = ArrayList<Foe>()
    private val skills = SwordSkills.load(context)
    private val cool = HashMap<String, Float>()
    private val coolMax = HashMap<String, Float>()
    private val icons = HashMap<String, Bitmap>()
    private val fxSheets = HashMap<String, Bitmap>()
    private val slotBmp = PixelArt.load(context, "hud/slot.png")
    private val barBg = PixelArt.load(context, "hud/bar_bg.png")
    private val barHp = PixelArt.load(context, "hud/bar_hp.png")
    private val xpBg = PixelArt.load(context, "hud/xp_bg.png")
    private val xpFill = PixelArt.load(context, "hud/xp_fill.png")
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
    private var menu = false
    private var heldSlot = -1
    private var surge = 0f
    private var surgeLeft = 0f
    private var wide = 0f

    private val sky = Paint().apply { color = 0xFF8EC8E8.toInt() }
    private val hillFar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9FCB86.toInt() }
    private val hillNear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E9A48.toInt() }
    private val hill = Path()
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF173307.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2173307.toInt() }
    private val jumpFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF3D7A1.toInt() }
    private val jumpEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
    }
    private val jumpInk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF173307.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFF6D8.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(6f)
    }
    private val jumpArrow = Path()
    private val frame = Runnable { tick() }

    init {
        keepScreenOn = true
        setBackgroundColor(0xFF8EC8E8.toInt())
        for (skill in SwordSkills.all) {
            icons[skill.id] = PixelArt.load(context, skill.icon)
            if (skill.fx !in fxSheets) fxSheets[skill.fx] = PixelArt.load(context, skill.fx)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        SwordSkills.save(context, skills)
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (!menu && event.x < dp(48f) && event.y < dp(40f)) {
            (context as? Activity)?.finish()
            return true
        }
        if (menuButton().contains(event.x, event.y)) {
            menu = !menu
            heldSlot = -1
            if (over) postOnAnimation(frame)
            return true
        }
        if (menu) {
            pokeTree(event.x, event.y)
            return true
        }
        val slot = hotSlot(event.x, event.y)
        if (slot >= 0) {
            cast(slot)
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
        sparks.clear()
        pending.clear()
        surge = 0f
        surgeLeft = 0f
        wide = 0f
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
        if (!over && !menu && width > 0 && height > 0) step(dt)
        invalidate()
        if (!over || menu) postOnAnimation(frame)
    }

    private fun step(dt: Float) {
        if (started) scroll += dp(42f) * dt
        if (attackLeft > 0f) attackLeft -= dt
        if (immune > 0f) immune -= dt
        if (wide > 0f) wide -= dt
        if (surgeLeft > 0f) {
            surgeLeft -= dt
            surge = dp(86f) * (surgeLeft / 0.2f).coerceIn(0f, 1f)
        } else {
            surge = 0f
        }
        for (id in cool.keys.toList()) cool[id] = (cool[id] ?: 0f) - dt
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
        val armed = clock > 0.55f
        for (foe in foes) {
            if (foe.rootLeft > 0f) {
                foe.rootLeft -= dt
            } else if (foe.archer && foe.x <= stop) {
                foe.shooting = true
                foe.x = stop
                foe.shootIn -= dt
                if (foe.shootIn <= 0f) {
                    foe.shootIn = 1.35f
                    val chest = foeBody(foe)
                    bolts.add(Bolt(chest.left, chest.centerY()))
                }
            } else {
                val drag = if (foe.slowLeft > 0f) 0.42f else 1f
                if (foe.slowLeft > 0f) foe.slowLeft -= dt
                foe.x -= pace * drag * dt
            }
            val body = foeBody(foe)
            // Archers hold still out of sword reach. The swing reaches that spot;
            // warriors still have to walk into the short blade.
            val extra = if (wide > 0f) dp(18f) + skills.rankOf("slash") * dp(12f) else 0f
            val reach = (if (foe.archer) dp(72f) else dp(22f)) + extra
            val beside = body.left < heroBody.right + reach &&
                body.left > heroBody.right - dp(10f) &&
                body.bottom > heroBody.top &&
                body.top < heroBody.bottom
            val touching = body.left < heroBody.right - dp(4f) &&
                body.right > heroBody.left + dp(8f) &&
                body.bottom > heroBody.top + dp(6f) &&
                body.top < heroBody.bottom - dp(4f)
            val dashed = surge > dp(8f) &&
                body.left < heroBody.right + dp(8f) &&
                body.right > heroBody.left &&
                body.bottom > heroBody.top &&
                body.top < heroBody.bottom
            if ((slash != null && beside) || dashed) {
                fell(foe, body)
            } else if (armed && !foe.archer && touching && surge <= dp(8f)) {
                if (wound()) {
                    foes.removeAll(pending)
                    pending.clear()
                    return
                }
            }
        }
        foes.removeAll(pending)
        pending.clear()

        val spent = ArrayList<Bolt>()
        for (bolt in bolts) {
            bolt.x -= dp(240f) * dt
            val box = boltRect(bolt)
            if (slash != null && RectF.intersects(slash, box)) {
                spent.add(bolt)
                puffs.add(Puff(box.centerX(), box.centerY()))
                audio.point()
            } else if (armed && RectF.intersects(box, heroBody)) {
                if (wound()) {
                    foes.removeAll(pending)
                    pending.clear()
                    return
                }
            } else if (bolt.x < -dp(20f)) {
                spent.add(bolt)
            }
        }
        bolts.removeAll(spent)
        val gone = ArrayList<Spark>()
        for (spark in sparks) {
            spark.x += spark.vx * dt
            spark.life -= dt
            if (spark.life <= 0f || spark.x > width + dp(20f)) {
                gone.add(spark)
                continue
            }
            for (foe in foes) {
                if (foe in pending) continue
                val body = foeBody(foe)
                if (spark.x < body.left || spark.x > body.right || spark.y < body.top || spark.y > body.bottom) continue
                if (spark.skill == "ice") {
                    foe.slowLeft = 1.8f + skills.rankOf("ice") * 0.7f
                    puffs.add(Puff(body.centerX(), body.centerY()))
                } else {
                    fell(foe, body)
                }
                spark.hits -= 1
                if (spark.hits <= 0) {
                    gone.add(spark)
                    break
                }
            }
        }
        sparks.removeAll(gone)
        foes.removeAll(pending)
        pending.clear()
    }

    private fun fell(foe: Foe, body: RectF) {
        if (foe in pending) return
        pending.add(foe)
        puffs.add(Puff(body.centerX(), body.centerY()))
        val previous = score
        score += 1
        val leveled = skills.addKill()
        SwordSkills.save(context, skills)
        audio.point()
        val reward = GameRewards.onScore(context, previous, score, 150, 8, 300, 12, "Kılıç")
        banner = when {
            leveled -> "Seviye ${skills.level()}  +1 puan"
            !reward.isNullOrBlank() -> reward
            else -> banner
        }
        if (leveled || !reward.isNullOrBlank()) {
            bannerUntil = SystemClock.uptimeMillis() + 1300L
            if (!reward.isNullOrBlank()) audio.reward()
        }
        Achievements.celebrate(context, Achievements.SWORD, score)
    }

    private fun cast(slot: Int) {
        if (over) return
        val id = skills.bar[slot]
        val rank = skills.rankOf(id)
        if (rank <= 0 || (cool[id] ?: 0f) > 0f) return
        val wait = (5.4f - rank * 0.7f).coerceAtLeast(2.2f)
        cool[id] = wait
        coolMax[id] = wait
        started = true
        val hero = if (width > 0) heroRect() else return
        when (id) {
            "fire" -> sparks.add(Spark(hero.right, hero.centerY(), dp(460f), id, 0.9f, rank))
            "ice" -> sparks.add(Spark(hero.right, hero.centerY(), dp(400f), id, 0.9f, 1))
            "heal" -> {
                hp = (hp + if (rank >= 3) 2 else 1).coerceAtMost(3)
                puffs.add(Puff(hero.centerX(), hero.centerY(), 0.45f))
            }
            "ward" -> {
                immune = maxOf(immune, 0.85f + rank * 0.4f)
                puffs.add(Puff(hero.centerX(), hero.centerY(), 0.45f))
            }
            "bolt" -> {
                val target = foes.maxByOrNull { if (it.archer) it.x + 10000f else it.x }
                if (target != null) {
                    fell(target, foeBody(target))
                    if (rank >= 3) {
                        val other = foes.filter { it !in pending }.maxByOrNull { it.x }
                        if (other != null) fell(other, foeBody(other))
                    }
                    foes.removeAll(pending)
                    pending.clear()
                }
            }
            "vine" -> {
                val target = foes.filter { !it.archer }.minByOrNull { it.x }
                if (target != null) target.rootLeft = 1.4f + rank * 0.45f
            }
            "slash" -> wide = 0.55f
            "dash" -> {
                surgeLeft = 0.2f
                surge = dp(86f)
            }
        }
        audio.jump()
        banner = SwordSkills.skill(id)?.name ?: id
        bannerUntil = SystemClock.uptimeMillis() + 700L
    }

    private fun pokeTree(x: Float, y: Float) {
        val top = dp(108f)
        val rowH = dp(46f)
        SwordSkills.all.forEachIndexed { index, skill ->
            val row = RectF(dp(12f), top + index * rowH, width - dp(12f), top + (index + 1) * rowH)
            if (!row.contains(x, y)) return@forEachIndexed
            val raise = RectF(row.right - dp(86f), row.top + dp(6f), row.right - dp(4f), row.bottom - dp(6f))
            if (raise.contains(x, y)) {
                banner = skills.raise(skill.id)
                bannerUntil = SystemClock.uptimeMillis() + 1000L
                SwordSkills.save(context, skills)
            } else if (heldSlot >= 0) {
                banner = skills.assign(heldSlot, skill.id)
                bannerUntil = SystemClock.uptimeMillis() + 1000L
                heldSlot = -1
                SwordSkills.save(context, skills)
            }
        }
        for (slot in 0 until 3) {
            if (!hotRect(slot).contains(x, y)) continue
            if (heldSlot < 0) {
                heldSlot = slot
            } else if (heldSlot == slot) {
                skills.clear(slot)
                heldSlot = -1
                SwordSkills.save(context, skills)
            } else {
                skills.swap(heldSlot, slot)
                heldSlot = -1
                SwordSkills.save(context, skills)
            }
        }
    }

    private fun menuButton() = RectF(dp(8f), dp(48f), dp(64f), dp(104f))

    private fun hotRect(index: Int): RectF {
        val side = dp(48f)
        val left = dp(8f) + index * (side + dp(6f))
        val top = height - side - dp(12f)
        return RectF(left, top, left + side, top + side)
    }

    private fun hotSlot(x: Float, y: Float): Int {
        for (index in 0 until 3) if (hotRect(index).contains(x, y)) return index
        return -1
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
        val maxH = cell() * 0.50f
        val maxW = cell() * 0.56f
        var h = maxH
        var w = h * aspect
        if (w > maxW) {
            w = maxW
            h = w / aspect.coerceAtLeast(0.25f)
        }
        if (h > w * 1.35f) h = w * 1.35f
        val bottom = groundTop() - lift
        val left = width * 0.16f + surge
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
        PixelArt.blitSmooth(canvas, pet, index, pet.width / 6, hero, src)
        for (spark in sparks) {
            val sheet = fxSheets[SwordSkills.skill(spark.skill)?.fx] ?: continue
            val size = dp(34f)
            dest.set(spark.x - size / 2f, spark.y - size / 2f, spark.x + size / 2f, spark.y + size / 2f)
            blitCell(canvas, sheet, ((0.9f - spark.life) * 12f).toInt(), dest)
        }
        if (attackLeft in 0.05f..0.28f) {
            val reach = dp(72f)
            dest.set(
                hero.right - dp(4f),
                hero.centerY() - reach * 0.42f,
                hero.right + reach,
                hero.centerY() + reach * 0.42f
            )
            canvas.drawArc(dest, -32f, 64f, false, slashPaint)
        }
        if (night > 0.04f) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                color = (night * 110f).toInt().shl(24)
            })
        }

        ink.color = 0xFF173307.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(16f), dp(40f), ink)
        PixelArt.drawScore(canvas, context, score, width / 2f, dp(4f), ::dp)
        drawBars(canvas)
        drawTreeButton(canvas)
        drawHotbar(canvas)
        if (banner.isNotEmpty() && SystemClock.uptimeMillis() < bannerUntil) {
            PixelArt.drawBanner(canvas, banner, width / 2f, dp(108f), ::dp)
        }
        drawJump(canvas, jumpRect())
        ink.textAlign = Paint.Align.CENTER
        ink.color = 0xFF173307.toInt()
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
        if (menu) drawTree(canvas)
    }

    private fun drawBars(canvas: Canvas) {
        val barW = dp(118f)
        val barH = dp(16f)
        val right = width - dp(8f)
        val top = dp(52f)
        dest.set(right - barW, top, right, top + barH)
        canvas.drawBitmap(barBg, null, dest, PixelArt.paint)
        val hpRatio = (hp / 3f).coerceIn(0f, 1f)
        if (hpRatio > 0f) {
            src.set(0, 0, (barHp.width * hpRatio).toInt().coerceAtLeast(1), barHp.height)
            val fill = RectF(
                dest.left + dp(3f),
                dest.top + dp(3f),
                dest.left + dp(3f) + (dest.width() - dp(6f)) * hpRatio,
                dest.bottom - dp(3f)
            )
            canvas.drawBitmap(barHp, src, fill, PixelArt.paint)
        }
        val xpTop = top + barH + dp(3f)
        dest.set(right - barW, xpTop, right, xpTop + barH)
        canvas.drawBitmap(xpBg, null, dest, PixelArt.paint)
        val xpRatio = skills.progress().coerceIn(0f, 1f)
        if (xpRatio > 0f) {
            src.set(0, 0, (xpFill.width * xpRatio).toInt().coerceAtLeast(1), xpFill.height)
            val fill = RectF(
                dest.left + dp(4f),
                dest.top + dp(3f),
                dest.left + dp(4f) + (dest.width() - dp(8f)) * xpRatio,
                dest.bottom - dp(3f)
            )
            canvas.drawBitmap(xpFill, src, fill, PixelArt.paint)
        }
        ink.textAlign = Paint.Align.RIGHT
        ink.textSize = dp(11f)
        ink.color = 0xFF173307.toInt()
        canvas.drawText("Sv ${skills.level()}   puan ${skills.points()}", right, xpTop + barH + dp(13f), ink)
    }

    private fun drawTreeButton(canvas: Canvas) {
        val button = menuButton()
        canvas.drawBitmap(slotBmp, null, button, PixelArt.paint)
        ink.textAlign = Paint.Align.CENTER
        ink.color = 0xFF3B2918.toInt()
        ink.textSize = dp(9f)
        canvas.drawText("Yetenek", button.centerX(), button.bottom - dp(6f), ink)
    }

    private fun drawHotbar(canvas: Canvas) {
        for (index in 0 until 3) {
            val rect = hotRect(index)
            canvas.drawBitmap(slotBmp, null, rect, PixelArt.paint)
            val id = skills.bar[index]
            val icon = icons[id]
            if (icon != null) {
                val pad = dp(8f)
                dest.set(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
                canvas.drawBitmap(icon, null, dest, PixelArt.paint)
                val max = coolMax[id] ?: 1f
                val left = ((cool[id] ?: 0f) / max).coerceIn(0f, 1f)
                if (left > 0f) {
                    canvas.drawRect(rect.left, rect.top, rect.right, rect.top + rect.height() * left, Paint().apply {
                        color = 0x88000000.toInt()
                    })
                }
            }
            if (heldSlot == index) canvas.drawRect(rect, markPaint())
        }
    }

    private fun drawTree(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = 0xCC1B140C.toInt() })
        ink.textAlign = Paint.Align.LEFT
        ink.color = 0xFFFFF6E4.toInt()
        ink.textSize = dp(16f)
        canvas.drawText("Yetenekler   Sv ${skills.level()}   puan ${skills.points()}", dp(16f), dp(78f), ink)
        ink.textSize = dp(11f)
        canvas.drawText("Yuvayı seç, yeteneğe dokun. Aynı yuva boşaltır, iki yuva yer değiştirir.", dp(16f), dp(98f), ink)
        val top = dp(108f)
        val rowH = dp(46f)
        SwordSkills.all.forEachIndexed { index, skill ->
            val row = RectF(dp(12f), top + index * rowH, width - dp(12f), top + (index + 1) * rowH - dp(3f))
            canvas.drawRoundRect(row, dp(8f), dp(8f), jumpFill)
            val icon = icons[skill.id]
            if (icon != null) {
                dest.set(row.left + dp(6f), row.top + dp(6f), row.left + dp(36f), row.bottom - dp(6f))
                canvas.drawBitmap(icon, null, dest, PixelArt.smooth)
            }
            ink.color = 0xFF3B2918.toInt()
            ink.textAlign = Paint.Align.LEFT
            ink.textSize = dp(13f)
            val rank = skills.rankOf(skill.id)
            canvas.drawText("${skill.name}  ${if (rank == 0) "kilit" else "sv $rank"}", row.left + dp(42f), row.top + dp(18f), ink)
            ink.textSize = dp(10f)
            canvas.drawText(skill.blurb, row.left + dp(42f), row.top + dp(34f), ink)
            val button = RectF(row.right - dp(86f), row.top + dp(8f), row.right - dp(6f), row.bottom - dp(8f))
            canvas.drawRoundRect(button, dp(6f), dp(6f), jumpFill)
            ink.textAlign = Paint.Align.CENTER
            ink.textSize = dp(11f)
            val label = when {
                rank >= SwordSkills.MAX_RANK -> "Dolu"
                skills.canRaise(skill.id) && rank == 0 -> "Aç"
                skills.canRaise(skill.id) -> "Yükselt"
                else -> "Kilit"
            }
            canvas.drawText(label, button.centerX(), button.centerY() + dp(4f), ink)
        }
        drawHotbar(canvas)
        drawTreeButton(canvas)
    }

    private fun markPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFE08A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private fun blitCell(canvas: Canvas, sheet: Bitmap, index: Int, at: RectF) {
        val cols = (sheet.width / 16).coerceAtLeast(1)
        val rows = (sheet.height / 16).coerceAtLeast(1)
        val cell = Math.floorMod(index, cols * rows)
        val col = cell % cols
        val row = cell / cols
        src.set(col * 16, row * 16, col * 16 + 16, row * 16 + 16)
        canvas.drawBitmap(sheet, src, at, PixelArt.paint)
    }

    private fun drawJump(canvas: Canvas, button: RectF) {
        canvas.drawRoundRect(button, dp(12f), dp(12f), jumpFill)
        canvas.drawRoundRect(button, dp(12f), dp(12f), jumpEdge)
        val cx = button.centerX()
        val tip = button.top + dp(10f)
        jumpArrow.reset()
        jumpArrow.moveTo(cx, tip)
        jumpArrow.lineTo(cx + dp(9f), tip + dp(11f))
        jumpArrow.lineTo(cx - dp(9f), tip + dp(11f))
        jumpArrow.close()
        canvas.drawPath(jumpArrow, jumpInk)
        jumpInk.textSize = dp(13f)
        canvas.drawText("Zıpla", cx, button.bottom - dp(6f), jumpInk)
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
