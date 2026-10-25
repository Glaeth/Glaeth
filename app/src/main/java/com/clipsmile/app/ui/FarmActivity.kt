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
import com.clipsmile.app.farm.FarmWorld
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A vertical farm day. The pet works the plot Nine Elma left behind.
 */
class FarmActivity : AppCompatActivity() {
    private lateinit var audio: GameAudio
    private var board: FarmView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio.farm(this)
        val model = intent.getStringExtra(FoodDropActivity.EXTRA_MODEL) ?: "pug"
        val view = FarmView(this, model, audio)
        board = view
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        audio.start()
    }

    override fun onPause() {
        board?.save()
        audio.pause()
        super.onPause()
    }

    override fun onDestroy() {
        audio.release()
        super.onDestroy()
    }
}

private class FarmView(
    context: Context,
    model: String,
    private val audio: GameAudio
) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val prefs = context.getSharedPreferences("clipsmile_pet", Context.MODE_PRIVATE)
    private val world = FarmWorld(prefs.getString(KEY, null))
    private val pet = PixelArt.petStrip(context, model, "front", "games/drop/pug.png")
    private val ground = PixelArt.load(context, "farm/ground.png")
    private val house = PixelArt.load(context, "farm/house.png")
    private val crops = PixelArt.load(context, "farm/pumpkin.png")
    private val furniture = PixelArt.load(context, "farm/furniture.png")
    private val barBg = PixelArt.load(context, "hud/bar_bg.png")
    private val barHp = PixelArt.load(context, "hud/bar_hp.png")
    private val src = Rect()
    private val dest = RectF()
    private val buttons = LinkedHashMap<String, RectF>()

    private var panel = Panel.NONE
    private var flash = world.notice
    private var flashUntil = SystemClock.uptimeMillis() + 2400L
    private var clock = 0f
    private var lastMs = 0L
    private val frame = Runnable { tick() }
    private var stickPointer = -1
    private var stickHeld = false
    private var stickCx = 0f
    private var stickCy = 0f
    private var stickR = 1f
    private var knobX = 0f
    private var knobY = 0f
    private var walkClock = 0f
    private var wasWalking = false

    private val sky = Paint().apply { color = 0xFF8EC8E8.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        isFakeBoldText = true
    }
    private val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFF6E4.toInt() }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B2918.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFE08A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    init {
        keepScreenOn = true
    }

    fun save() {
        prefs.edit().putString(KEY, world.export()).apply()
        GameScores.submit(context, GameScores.BEST_FARM, world.day)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        save()
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width > 0) layoutButtons()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                val hit = buttons.entries.firstOrNull { it.value.contains(x, y) }?.key
                if (hit != null) press(hit)
                else if (panel == Panel.NONE && hypot(x - stickCx, y - stickCy) <= stickR * 1.2f) {
                    stickPointer = event.getPointerId(index)
                    stickHeld = true
                    moveStick(x, y)
                } else if (panel != Panel.NONE) {
                    panel = Panel.NONE
                }
            }
            MotionEvent.ACTION_MOVE -> if (stickHeld) {
                val index = event.findPointerIndex(stickPointer)
                if (index >= 0) moveStick(event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val index = event.actionIndex
                if (event.getPointerId(index) == stickPointer || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    releaseStick()
                }
            }
        }
        invalidate()
        return true
    }

    private fun press(hit: String) {
        when {
            hit == "back" -> (context as? Activity)?.finish()
            hit == "close" -> panel = Panel.NONE
            hit == "shop" -> panel = if (panel == Panel.SHOP) Panel.NONE else Panel.SHOP
            hit == "bag" -> panel = if (panel == Panel.BAG) Panel.NONE else Panel.BAG
            hit == "sleep" -> act(world.sleep())
            hit == "use" -> if (panel == Panel.NONE) act(world.use())
            hit == "sellall" -> act(world.sellAllCrops())
            hit.startsWith("tool_") -> world.tool = when (hit.removePrefix("tool_")) {
                "can" -> FarmWorld.Tool.CAN
                "seed" -> FarmWorld.Tool.SEED
                "hand" -> FarmWorld.Tool.HAND
                else -> FarmWorld.Tool.HOE
            }
            hit.startsWith("furn_") -> holdFurniture(hit.removePrefix("furn_"))
            hit.startsWith("buy_") -> act(world.buy(hit.removePrefix("buy_")))
            hit.startsWith("eat_") -> act(world.eat(hit.removePrefix("eat_")))
            hit.startsWith("pick_") -> pick(hit.removePrefix("pick_"))
        }
    }

    private fun holdFurniture(item: String) {
        world.furnish = item
        world.tool = FarmWorld.Tool.HAND
        val label = FarmWorld.label(item)
        flash(
            if (world.count(item) > 0) "$label seçildi. Evde önündeki kareye Kullan."
            else "$label çantada yok. Sandıktan al."
        )
    }

    private fun moveStick(x: Float, y: Float) {
        var dx = x - stickCx
        var dy = y - stickCy
        val mag = hypot(dx, dy)
        val max = stickR * 0.72f
        if (mag > max && mag > 0f) {
            dx *= max / mag
            dy *= max / mag
        }
        knobX = stickCx + dx
        knobY = stickCy + dy
    }

    private fun releaseStick() {
        stickHeld = false
        stickPointer = -1
        knobX = stickCx
        knobY = stickCy
        wasWalking = false
        walkClock = 0f
    }

    private fun step(dx: Int, dy: Int) {
        if (panel != Panel.NONE) return
        val message = world.move(dx, dy)
        if (message.isNotEmpty()) flash(message)
        else passTime(1)
    }

    private fun act(message: String) {
        if (message.isNotEmpty()) flash(message)
        val worked = message.startsWith("Toprak") ||
            message.startsWith("Sulandı") ||
            message.startsWith("Ekildi") ||
            message.contains("toplandı") ||
            message.startsWith("Eşya") ||
            message.contains("altın.") ||
            message.contains("alındı") ||
            message.startsWith("Enerji") ||
            message.startsWith("Sandık")
        if (message.contains("toplandı")) {
            Achievements.celebrate(context, Achievements.FARM, world.harvested)
        }
        if (message.startsWith("Gün doğdu") || message.contains("bayıldın")) {
            Achievements.celebrate(context, Achievements.FARM_DAY, world.day)
            Achievements.celebrate(context, Achievements.FARM_LV, world.level())
            save()
            return
        }
        if (!worked) return
        passTime(10)
        save()
    }

    private fun pick(item: String) {
        when {
            item.endsWith("_seed") -> {
                world.seed = item.removeSuffix("_seed")
                world.tool = FarmWorld.Tool.SEED
                flash("${FarmWorld.label(item)} seçildi.")
            }
            item in FarmWorld.FURNITURE.values -> {
                world.furnish = item
                world.tool = FarmWorld.Tool.HAND
                flash("${FarmWorld.label(item)} ele alındı.")
            }
            item in FarmWorld.SELL -> act(world.sell(item))
        }
    }

    private fun passTime(extra: Int) {
        val day = world.day
        if (extra > 0) world.advance(extra)
        if (world.day != day) {
            flash(world.notice)
            Achievements.celebrate(context, Achievements.FARM_DAY, world.day)
            Achievements.celebrate(context, Achievements.FARM_LV, world.level())
            save()
        }
    }

    private fun flash(text: String) {
        flash = text
        flashUntil = SystemClock.uptimeMillis() + 2200L
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastMs == 0L) 0.016f else ((now - lastMs).coerceAtMost(40L)) / 1000f
        lastMs = now
        if (stickHeld && panel == Panel.NONE && width > 0) {
            val dx = knobX - stickCx
            val dy = knobY - stickCy
            val walking = hypot(dx, dy) > stickR * 0.32f
            if (walking) {
                if (!wasWalking) walkClock = 0.2f
                walkClock += dt
                if (walkClock >= 0.2f) {
                    walkClock = 0f
                    val stepX = if (abs(dx) > abs(dy)) if (dx > 0f) 1 else -1 else 0
                    val stepY = if (abs(dy) >= abs(dx)) if (dy > 0f) 1 else -1 else 0
                    step(stepX, stepY)
                }
            } else {
                walkClock = 0f
            }
            wasWalking = walking
        } else if (!stickHeld) {
            wasWalking = false
        }
        if (panel == Panel.NONE && width > 0) {
            clock += dt
            // One game minute every 1.2 real seconds, so a day lasts about twenty minutes.
            if (clock >= 1.2f) {
                clock -= 1.2f
                val day = world.day
                world.advance(1)
                if (world.day != day) {
                    flash(world.notice)
                    Achievements.celebrate(context, Achievements.FARM_DAY, world.day)
                    Achievements.celebrate(context, Achievements.FARM_LV, world.level())
                    save()
                }
            }
        }
        invalidate()
        postOnAnimation(frame)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sky)
        layoutButtons()
        drawHud(canvas)
        if (panel == Panel.NONE) drawMap(canvas) else drawPanel(canvas)
        drawStick(canvas)
        drawControls(canvas)
        if (flash.isNotEmpty() && SystemClock.uptimeMillis() < flashUntil) {
            PixelArt.drawBanner(canvas, flash, width / 2f, dp(124f), ::dp)
        }
    }

    private fun drawHud(canvas: Canvas) {
        ink.color = 0xFF3B2918.toInt()
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(22f)
        canvas.drawText("←", dp(12f), dp(30f), ink)
        ink.textSize = dp(13f)
        canvas.drawText("Gün ${world.day}   ${world.clock()}", dp(44f), dp(22f), ink)
        canvas.drawText("${world.gold} altın   Sv ${world.level()}", dp(44f), dp(40f), ink)
        val ratio = world.stamina / world.maxStamina().toFloat().coerceAtLeast(1f)
        dest.set(width - dp(132f), dp(10f), width - dp(8f), dp(28f))
        canvas.drawBitmap(barBg, null, dest, PixelArt.paint)
        val fill = RectF(dest.left + dp(4f), dest.top + dp(3f), dest.left + dp(4f) + (dest.width() - dp(8f)) * ratio, dest.bottom - dp(3f))
        src.set(0, 0, (barHp.width * ratio).toInt().coerceAtLeast(1), barHp.height)
        canvas.drawBitmap(barHp, src, fill, PixelArt.paint)
        ink.textAlign = Paint.Align.RIGHT
        ink.textSize = dp(10f)
        canvas.drawText("Enerji ${world.stamina}", width - dp(12f), dp(44f), ink)
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(12f)
        val questBottom = drawWrapped(canvas, world.notice, dp(12f), dp(60f), width - dp(24f))
        val season = if (world.raining()) "Yağmurlu" else "Açık"
        canvas.drawText(
            "$season   ${FarmWorld.label(world.seed)} tohumu ${world.count(world.seed + "_seed")}",
            dp(12f),
            questBottom + dp(16f),
            ink
        )
    }

    private fun drawWrapped(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float): Float {
        val words = text.split(" ")
        var line = ""
        var lineY = y
        var drawn = 0
        for (word in words) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && ink.measureText(trial) > maxWidth) {
                val shown = ellipsize(line, maxWidth, force = drawn == 1)
                canvas.drawText(shown, x, lineY, ink)
                drawn++
                if (drawn == 2) return lineY
                line = word
                lineY += dp(15f)
            } else {
                line = trial
            }
        }
        if (line.isNotEmpty() && drawn < 2) {
            canvas.drawText(ellipsize(line, maxWidth, force = false), x, lineY, ink)
        }
        return lineY
    }

    private fun ellipsize(text: String, maxWidth: Float, force: Boolean): String {
        val withMark = if (force && !text.endsWith("…")) "$text…" else text
        if (ink.measureText(withMark) <= maxWidth) return withMark
        var cut = text.removeSuffix("…")
        while (cut.length > 4 && ink.measureText("$cut…") > maxWidth) cut = cut.dropLast(1)
        return "$cut…"
    }

    private fun drawMap(canvas: Canvas) {
        val indoors = world.indoors
        val cols = if (indoors) FarmWorld.HW else FarmWorld.W
        val rows = if (indoors) FarmWorld.HH else FarmWorld.H
        val top = dp(108f)
        val bottom = stickCy - stickR - dp(8f)
        val cell = minOf(
            width / cols.toFloat(),
            (bottom - top) / if (indoors) rows.toFloat() else 10f
        )
        val viewW = width / cell
        val viewH = (bottom - top) / cell
        val camX = if (cols <= viewW + 0.01f) {
            (cols - viewW) / 2f
        } else {
            (world.px - viewW / 2f).coerceIn(0f, cols - viewW)
        }
        val camY = if (rows <= viewH + 0.01f) {
            (rows - viewH) / 2f
        } else {
            (world.py - viewH / 2f).coerceIn(0f, rows - viewH)
        }
        canvas.save()
        canvas.clipRect(0f, top, width.toFloat(), bottom)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val left = (x - camX) * cell
                val tileTop = top + (y - camY) * cell
                if (left > width || tileTop > bottom || left + cell < 0 || tileTop + cell < top) continue
                dest.set(left, tileTop, left + cell, tileTop + cell)
                if (indoors) drawRoomTile(canvas, x, y) else drawFarmTile(canvas, x, y)
            }
        }
        if (!indoors) drawCottage(canvas, camX, camY, top, cell)
        val fx = world.px + world.facingX
        val fy = world.py + world.facingY
        dest.set((fx - camX) * cell, top + (fy - camY) * cell, (fx + 1 - camX) * cell, top + (fy + 1 - camY) * cell)
        canvas.drawRect(dest, mark)
        drawActor(canvas, camX, camY, top, cell)
        canvas.restore()
    }

    private fun drawFarmTile(canvas: Canvas, x: Int, y: Int) {
        val tile = world.farm[y][x]
        val spot = when (tile) {
            FarmWorld.DOOR, FarmWorld.PATH -> 9 to 8
            FarmWorld.WATERED -> if ((x + y) % 2 == 0) 6 to 5 else 8 to 5
            FarmWorld.TILLED -> if ((x * 2 + y) % 2 == 0) 7 to 0 else 9 to 0
            else -> {
                when ((x * 3 + y * 5) % 4) {
                    0 -> 6 to 3
                    1 -> 8 to 3
                    2 -> 10 to 3
                    else -> 7 to 3
                }
            }
        }
        blitGround(canvas, spot.first, spot.second)
        if (tile == FarmWorld.GRASS) {
            val flower = when ((x * 5 + y * 7) % 11) {
                0 -> 0 to 3
                1 -> 3 to 3
                3 -> 2 to 4
                else -> null
            }
            if (flower != null) blitGround(canvas, flower.first, flower.second)
        }
        val crop = world.crop[y][x]
        if (crop != 0) {
            val stage = world.stage[y][x].coerceAtLeast(1)
            val frame = when (crop) {
                2 -> listOf(3, 7, 11, 14)[(stage - 1).coerceIn(0, 3)]
                3 -> listOf(1, 4, 8)[(stage - 1).coerceIn(0, 2)]
                else -> listOf(0, 2, 5)[(stage - 1).coerceIn(0, 2)]
            }
            val plant = RectF(dest.left, dest.top - dest.height() * 0.7f, dest.right, dest.bottom)
            src.set(frame * 16, 0, frame * 16 + 16, crops.height.coerceAtMost(32))
            canvas.drawBitmap(crops, src, plant, PixelArt.paint)
        }
    }

    private fun drawCottage(canvas: Canvas, camX: Float, camY: Float, top: Float, cell: Float) {
        val saved = RectF(dest)
        val widthCells = 3.25f
        val heightCells = widthCells * house.height / house.width.toFloat()
        val center = (5.5f - camX) * cell
        val foot = top + (6f - camY) * cell
        dest.set(center - cell * widthCells / 2f, foot - cell * heightCells, center + cell * widthCells / 2f, foot)
        canvas.drawBitmap(house, null, dest, PixelArt.paint)
        dest.set(saved)
    }

    private fun drawRoomTile(canvas: Canvas, x: Int, y: Int) {
        val border = x == 0 || y == 0 || x == FarmWorld.HW - 1 || y == FarmWorld.HH - 1
        if (x == FarmWorld.DOOR_X && y == FarmWorld.HH - 1) {
            blitGround(canvas, 6, 7)
            return
        }
        if (border) {
            blitGround(canvas, 7, 0)
            return
        }
        blitGround(canvas, 9, 8)
        val piece = world.house[y][x]
        if (piece == 0) return
        if (piece == 4) {
            drawLamp(canvas)
            return
        }
        val box = when (piece) {
            1 -> Rect(309, 12, 332, 48)
            2 -> Rect(225, 11, 304, 48)
            else -> Rect(208, 39, 224, 64)
        }
        val aspect = box.width().toFloat() / box.height().coerceAtLeast(1)
        var spriteH = dest.height() * 1.95f
        var spriteW = spriteH * aspect
        val maxW = dest.width() * 2.15f
        if (spriteW > maxW) {
            spriteW = maxW
            spriteH = spriteW / aspect
        }
        val sprite = RectF(
            dest.centerX() - spriteW / 2f,
            dest.bottom - spriteH - dest.height() * 0.04f,
            dest.centerX() + spriteW / 2f,
            dest.bottom - dest.height() * 0.04f
        )
        canvas.drawOval(
            dest.centerX() - spriteW * 0.38f,
            dest.bottom - dest.height() * 0.22f,
            dest.centerX() + spriteW * 0.38f,
            dest.bottom - dest.height() * 0.04f,
            Paint().apply { color = 0x553B2918 }
        )
        canvas.drawBitmap(furniture, box, sprite, PixelArt.paint)
    }

    private fun drawLamp(canvas: Canvas) {
        val cx = dest.centerX()
        val foot = dest.bottom - dest.height() * 0.06f
        val shade = Paint().apply { color = 0xFFF2D36B.toInt() }
        val pole = Paint().apply { color = 0xFF6B4630.toInt() }
        val glow = Paint().apply { color = 0x66FFE08A }
        canvas.drawOval(cx - dest.width() * 0.28f, foot - dest.height() * 0.16f, cx + dest.width() * 0.28f, foot, glow)
        canvas.drawRect(cx - dp(2.5f), foot - dest.height() * 1.15f, cx + dp(2.5f), foot - dest.height() * 0.12f, pole)
        canvas.drawOval(cx - dest.width() * 0.22f, foot - dest.height() * 0.18f, cx + dest.width() * 0.22f, foot - dest.height() * 0.04f, pole)
        canvas.drawOval(cx - dest.width() * 0.42f, foot - dest.height() * 1.55f, cx + dest.width() * 0.42f, foot - dest.height() * 1.05f, shade)
        canvas.drawRect(cx - dest.width() * 0.34f, foot - dest.height() * 1.35f, cx + dest.width() * 0.34f, foot - dest.height() * 1.12f, shade)
    }

    private fun drawActor(canvas: Canvas, camX: Float, camY: Float, top: Float, cell: Float) {
        val frameW = pet.width / petFrames()
        val aspect = frameW / pet.height.coerceAtLeast(1).toFloat()
        val h = cell * 1.35f
        val w = h * aspect
        val footX = (world.px + 0.5f - camX) * cell
        val footY = top + (world.py + 1f - camY) * cell
        dest.set(footX - w / 2f, footY - h, footX + w / 2f, footY)
        canvas.save()
        if (world.facingX < 0) canvas.scale(-1f, 1f, dest.centerX(), dest.centerY())
        val frame = ((SystemClock.uptimeMillis() / 180L) % petFrames()).toInt()
        PixelArt.blitSmooth(canvas, pet, frame, frameW, dest, src)
        canvas.restore()
    }

    private fun petFrames(): Int = if (pet.width > pet.height * 2) 4 else 1

    private fun blitGround(canvas: Canvas, col: Int, row: Int) {
        src.set(col * 16, row * 16, col * 16 + 16, row * 16 + 16)
        canvas.drawBitmap(ground, src, dest, PixelArt.paint)
    }

    private fun drawPanel(canvas: Canvas) {
        val top = dp(108f)
        val bottom = stickCy - stickR - dp(12f)
        canvas.drawRoundRect(dp(12f), top, width - dp(12f), bottom, dp(16f), dp(16f), card)
        canvas.drawRoundRect(dp(12f), top, width - dp(12f), bottom, dp(16f), dp(16f), edge)
        buttons["close"] = RectF(width - dp(56f), top + dp(6f), width - dp(18f), top + dp(32f))
        ink.textAlign = Paint.Align.LEFT
        ink.color = 0xFF3B2918.toInt()
        ink.textSize = dp(16f)
        val title = if (panel == Panel.SHOP) "Köy sandığı" else "Çanta"
        canvas.drawText(title, dp(24f), top + dp(26f), ink)
        ink.textAlign = Paint.Align.RIGHT
        canvas.drawText("Kapat", width - dp(22f), top + dp(26f), ink)
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = dp(14f)
        var y = top + dp(52f)
        if (panel == Panel.SHOP) {
            for ((item, price) in FarmWorld.BUY) {
                if (y > bottom - dp(28f)) break
                canvas.drawText("${FarmWorld.label(item)}   $price altın", dp(24f), y + dp(16f), ink)
                buttons["buy_$item"] = RectF(dp(20f), y, width - dp(20f), y + dp(28f))
                y += dp(30f)
            }
        } else {
            buttons["sellall"] = RectF(dp(20f), y, width - dp(20f), y + dp(28f))
            canvas.drawText("Bütün mahsulü sat", dp(24f), y + dp(16f), ink)
            y += dp(32f)
            for ((item, count) in world.bag.entries.sortedBy { it.key }) {
                if (count <= 0 || y > bottom - dp(28f)) continue
                canvas.drawText("${FarmWorld.label(item)}  ×$count", dp(24f), y + dp(16f), ink)
                buttons["pick_$item"] = RectF(dp(20f), y, width * 0.68f, y + dp(28f))
                if (item in FarmWorld.EAT) {
                    buttons["eat_$item"] = RectF(width * 0.68f, y, width - dp(20f), y + dp(28f))
                    ink.textAlign = Paint.Align.RIGHT
                    canvas.drawText("Ye", width - dp(24f), y + dp(16f), ink)
                    ink.textAlign = Paint.Align.LEFT
                }
                y += dp(30f)
            }
        }
    }

    private fun drawStick(canvas: Canvas) {
        if (panel != Panel.NONE) return
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC3B2918.toInt() }
        val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF6E2B8.toInt() }
        canvas.drawCircle(stickCx, stickCy, stickR, base)
        canvas.drawCircle(stickCx, stickCy, stickR, edge)
        canvas.drawCircle(knobX, knobY, stickR * 0.42f, knob)
        canvas.drawCircle(knobX, knobY, stickR * 0.42f, edge)
    }

    private fun drawControls(canvas: Canvas) {
        for ((id, rect) in buttons) {
            if (id == "back" || id == "close" || id.startsWith("buy_") || id.startsWith("pick_") || id.startsWith("eat_") || id == "sellall") continue
            val on = when (id) {
                "tool_hoe" -> world.tool == FarmWorld.Tool.HOE
                "tool_can" -> world.tool == FarmWorld.Tool.CAN
                "tool_seed" -> world.tool == FarmWorld.Tool.SEED
                "tool_hand" -> world.tool == FarmWorld.Tool.HAND
                "furn_bed" -> world.tool == FarmWorld.Tool.HAND && world.furnish == "bed"
                "furn_table" -> world.tool == FarmWorld.Tool.HAND && world.furnish == "table"
                "furn_chair" -> world.tool == FarmWorld.Tool.HAND && world.furnish == "chair"
                "furn_lamp" -> world.tool == FarmWorld.Tool.HAND && world.furnish == "lamp"
                else -> false
            }
            canvas.drawRoundRect(rect, dp(10f), dp(10f), Paint().apply {
                color = if (on) 0xFF8FCB6A.toInt() else 0xFFF3D7A1.toInt()
            })
            canvas.drawRoundRect(rect, dp(10f), dp(10f), edge)
            ink.color = 0xFF173307.toInt()
            ink.textAlign = Paint.Align.CENTER
            ink.textSize = dp(if (id.startsWith("furn_")) 11f else 12f)
            val label = when (id) {
                "use" -> "Kullan"
                "shop" -> "Sandık"
                "bag" -> "Çanta"
                "sleep" -> "Uyu"
                "tool_hoe" -> "Çapa"
                "tool_can" -> "Su"
                "tool_seed" -> "Tohum"
                "tool_hand" -> "El"
                "furn_bed" -> "Yatak ${world.count("bed")}"
                "furn_table" -> "Masa ${world.count("table")}"
                "furn_chair" -> "Sandalye ${world.count("chair")}"
                "furn_lamp" -> "Lamba ${world.count("lamp")}"
                else -> ""
            }
            canvas.drawText(label, rect.centerX(), rect.centerY() + dp(4f), ink)
        }
    }

    private fun layoutButtons() {
        buttons.clear()
        buttons["back"] = RectF(0f, 0f, dp(40f), dp(42f))
        val toolH = dp(34f)
        val toolTop = height - toolH - dp(8f)
        val toolW = (width - dp(16f)) / 4f
        listOf("hoe", "can", "seed", "hand").forEachIndexed { index, name ->
            val left = dp(8f) + index * toolW
            buttons["tool_$name"] = RectF(left, toolTop, left + toolW - dp(4f), toolTop + toolH)
        }
        val s = dp(42f)
        val base = toolTop - dp(8f)
        stickR = dp(54f)
        stickCx = dp(72f)
        stickCy = base - s * 1.7f
        if (!stickHeld) {
            knobX = stickCx
            knobY = stickCy
        }
        val actionW = dp(78f)
        val right = width - dp(8f)
        buttons["use"] = RectF(right - actionW, base - s * 3 - dp(4f), right, base - s * 2 - dp(4f))
        buttons["shop"] = RectF(right - actionW, base - s * 2 + dp(2f), right - actionW / 2f - dp(2f), base - s)
        buttons["bag"] = RectF(right - actionW / 2f + dp(2f), base - s * 2 + dp(2f), right, base - s)
        buttons["sleep"] = RectF(right - actionW, base - s + dp(4f), right, base + dp(2f))
        val furnLeft = stickCx + stickR + dp(14f)
        val furnRight = right - actionW - dp(8f)
        val furnTop = toolTop - dp(46f)
        val furnW = ((furnRight - furnLeft) / 4f).coerceAtLeast(dp(36f))
        listOf("bed", "table", "chair", "lamp").forEachIndexed { index, name ->
            val left = furnLeft + index * furnW
            buttons["furn_$name"] = RectF(left, furnTop, left + furnW - dp(4f), toolTop - dp(6f))
        }
    }

    private enum class Panel { NONE, SHOP, BAG }

    companion object {
        private const val KEY = "farm_world"
    }
}
