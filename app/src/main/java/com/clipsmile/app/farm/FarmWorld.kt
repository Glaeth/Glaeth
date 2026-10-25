package com.clipsmile.app.farm

/**
 * A small vertical farm. Days, soil, crops, stamina and the house are all
 * decided here so the rules can be checked without a screen.
 */
class FarmWorld(raw: String? = null) {

    var day = 1
    var minute = 6 * 60
    var stamina = 90
    var gold = 160
    var px = 6
    var py = 7
    var indoors = false
    var tool = Tool.HOE
    var seed = "turnip"
    var xp = 0
    var quest = 0
    var harvested = 0
    var sold = 0
    var placed = 0
    var wateredEver = 0
    var plantedEver = 0
    var tilledEver = 0
    var slept = 0
    var daily = 0
    var notice = "Nine Elma tarlayı sana bıraktı. Çapa ile dört tarla aç."
    var facingX = -1
    var facingY = 0
    var furnish = "chair"

    val farm = Array(H) { IntArray(W) { GRASS } }
    val crop = Array(H) { IntArray(W) }
    val stage = Array(H) { IntArray(W) }
    val house = Array(HH) { IntArray(HW) }
    val bag = HashMap<String, Int>()

    init {
        if (raw.isNullOrBlank() || !restore(raw)) fresh()
        settleYard()
    }

    fun level(): Int = 1 + xp / 50

    fun clock(): String {
        val hour = (minute / 60).coerceIn(0, 23)
        val mins = (minute % 60).coerceIn(0, 59)
        return "%02d:%02d".format(hour, mins)
    }

    fun count(item: String): Int = bag[item] ?: 0

    fun advance(minutes: Int) {
        if (minutes <= 0) return
        minute += minutes
        if (minute >= 24 * 60) collapse()
    }

    fun move(dx: Int, dy: Int): String {
        if (dx == 0 && dy == 0) return ""
        val nx = px + dx
        val ny = py + dy
        if (!inside(nx, ny)) return "Duvar."
        if (solid(nx, ny)) return "Geçilmez."
        px = nx
        py = ny
        facingX = dx
        facingY = dy
        if (tile(nx, ny) == DOOR) changeRoom()
        return ""
    }

    fun use(): String {
        val tx = (px + facingX).coerceIn(0, width() - 1)
        val ty = (py + facingY).coerceIn(0, height() - 1)
        if (tx == px && ty == py) return "Önündeki kareye bak."
        return when (tool) {
            Tool.HOE -> hoe(tx, ty)
            Tool.CAN -> water(tx, ty)
            Tool.SEED -> plant(tx, ty)
            Tool.HAND -> hand(tx, ty)
        }
    }

    fun sleep(): String {
        if (!indoors) return "Uyumak için eve gir."
        return wake(false)
    }

    fun buy(item: String): String {
        val price = BUY[item] ?: return "Bu satılmıyor."
        if (gold < price) return "Altın yetmiyor."
        gold -= price
        give(item, 1)
        return "$item alındı."
    }

    fun sell(item: String): String {
        if (SELL[item] == null) return "Bunu alıcı istemiyor."
        if (count(item) <= 0) return "Çantada yok."
        val price = priceOf(item)
        give(item, -1)
        gold += price
        sold += 1
        xp += 4
        pushQuest()
        return "$price altın."
    }

    fun sellAllCrops(): String {
        var earned = 0
        for (item in SELL.keys) {
            val have = count(item)
            if (have <= 0) continue
            val price = priceOf(item)
            earned += have * price
            bag.remove(item)
            sold += have
            xp += have * 4
        }
        if (earned == 0) return "Satacak mahsul yok."
        gold += earned
        pushQuest()
        return "Sandık $earned altın verdi."
    }

    fun eat(item: String): String {
        val gain = EAT[item] ?: return "Bu yenmez."
        if (count(item) <= 0) return "Çantada yok."
        give(item, -1)
        stamina = (stamina + gain).coerceAtMost(maxStamina())
        return "Enerji +$gain."
    }

    fun raining(): Boolean = day % 4 == 0

    private fun hoe(x: Int, y: Int): String {
        if (indoors) return "Evde tarla yok."
        if (farm[y][x] != GRASS) return "Burası açılamaz."
        if (!spend(3)) return "Yoruldun."
        farm[y][x] = TILLED
        tilledEver += 1
        pushQuest()
        return "Toprak açıldı."
    }

    private fun water(x: Int, y: Int): String {
        if (indoors) return "Evde sulanacak yer yok."
        val ground = farm[y][x]
        if (ground != TILLED && ground != WATERED) return "Önce çapa."
        if (ground == WATERED) return "Zaten ıslak."
        if (!spend(2)) return "Yoruldun."
        farm[y][x] = WATERED
        wateredEver += 1
        pushQuest()
        return "Sulandı."
    }

    private fun plant(x: Int, y: Int): String {
        if (indoors) return "Evde ekilmez."
        val ground = farm[y][x]
        if (ground != TILLED && ground != WATERED) return "Önce toprağı aç."
        if (crop[y][x] != 0) return "Burada bir bitki var."
        val key = seed + "_seed"
        if (count(key) <= 0) return "Tohum kalmadı."
        if (!spend(1)) return "Yoruldun."
        give(key, -1)
        crop[y][x] = CROPS.getValue(seed)
        stage[y][x] = 1
        plantedEver += 1
        pushQuest()
        return "Ekildi."
    }

    private fun hand(x: Int, y: Int): String {
        if (indoors) return placeOrTake(x, y)
        if (crop[y][x] == 0) return "Toplanacak bir şey yok."
        val id = crop[y][x]
        val need = GROW.getValue(id)
        if (stage[y][x] < need) return "Daha büyümedi."
        if (!spend(2)) return "Yoruldun."
        val item = NAME.getValue(id)
        give(item, 1)
        crop[y][x] = 0
        stage[y][x] = 0
        if (farm[y][x] == WATERED) farm[y][x] = TILLED
        harvested += 1
        daily += 1
        xp += 8
        var extra = ""
        if (quest >= 7 && daily >= 5) {
            daily = 0
            gold += 80
            extra = " Günlük teslimat 80 altın."
        }
        pushQuest()
        return "$item toplandı.$extra"
    }

    private fun placeOrTake(x: Int, y: Int): String {
        if (house[y][x] != 0) {
            val kind = house[y][x]
            house[y][x] = 0
            give(FURNITURE.getValue(kind), 1)
            return "Eşya çantaya döndü."
        }
        val held = FURNITURE.entries.firstOrNull { it.value == furnish && count(it.value) > 0 }
            ?: return "Seçili eşya çantada yok."
        if (x == DOOR_X && y == HH - 1) return "Kapının önü boş kalsın."
        give(held.value, -1)
        house[y][x] = held.key
        placed += 1
        pushQuest()
        return "Eşya kondu."
    }

    private fun wake(fainted: Boolean): String {
        val loss = if (fainted && gold > 0) (gold / 10).coerceAtLeast(1).coerceAtMost(gold) else 0
        gold -= loss
        stamina = if (fainted) maxStamina() / 2 else maxStamina()
        minute = 6 * 60
        day += 1
        grow()
        slept += 1
        px = if (indoors) DOOR_X else 6
        py = if (indoors) HH - 2 else 7
        pushQuest()
        return if (fainted) {
            "Gece yarısı bayıldın. $loss altın düştü."
        } else {
            "Gün doğdu. Mahsuller büyüdü."
        }
    }

    private fun collapse() {
        notice = wake(true)
    }

    private fun grow() {
        for (y in 0 until H) {
            for (x in 0 until W) {
                val id = crop[y][x]
                if (id != 0 && farm[y][x] == WATERED && stage[y][x] < GROW.getValue(id)) {
                    stage[y][x] += 1
                }
                if (farm[y][x] == WATERED) farm[y][x] = TILLED
            }
        }
        if (raining()) {
            for (y in 0 until H) {
                for (x in 0 until W) {
                    if (farm[y][x] == TILLED) farm[y][x] = WATERED
                }
            }
        }
    }

    private fun priceOf(item: String): Int {
        val base = SELL[item] ?: return 0
        val bonus = ((day / 7) * 8).coerceAtMost(80)
        return base * (100 + bonus) / 100
    }

    private fun spend(cost: Int): Boolean {
        if (stamina < cost) return false
        stamina -= cost
        return true
    }

    internal fun give(item: String, amount: Int) {
        val next = count(item) + amount
        if (next <= 0) bag.remove(item) else bag[item] = next
    }

    private fun pushQuest() {
        var guard = 0
        while (guard++ < 8) {
            val next = when (quest) {
                0 -> if (tilledEver >= 4) 1 else 0
                1 -> if (wateredEver >= 4) 2 else 1
                2 -> if (plantedEver >= 2) 3 else 2
                3 -> if (harvested >= 1) 4 else 3
                4 -> if (sold >= 1) 5 else 4
                5 -> if (slept >= 1) 6 else 5
                6 -> if (placed >= 1) 7 else 6
                else -> 7
            }
            if (next == quest) break
            quest = next
            xp += 20
        }
        notice = QUEST.getValue(quest)
        if (raining()) notice = "$notice Yağmur tarlayı ıslattı."
    }

    private fun changeRoom() {
        indoors = !indoors
        if (indoors) {
            px = DOOR_X
            py = HH - 2
        } else {
            px = 6
            py = 5
        }
        facingX = 0
        facingY = if (indoors) -1 else 1
    }

    private fun fresh() {
        bag.clear()
        bag["turnip_seed"] = 8
        bag["pumpkin_seed"] = 2
        bag["berry_seed"] = 2
        bag["chair"] = 1
        bag["table"] = 1
        bag["bed"] = 1
        bag["lamp"] = 1
        for (y in 0 until H) {
            for (x in 0 until W) {
                farm[y][x] = when {
                    x == DOOR_X && y == 5 -> DOOR
                    y in 1..4 && x in 4..6 -> BLOCK
                    x == 6 && y >= 5 -> PATH
                    x == DOOR_X && y == 6 -> PATH
                    else -> GRASS
                }
                crop[y][x] = 0
                stage[y][x] = 0
            }
        }
        farm[5][DOOR_X] = DOOR
        for (y in 0 until HH) {
            for (x in 0 until HW) house[y][x] = 0
        }
    }

    fun maxStamina() = 90 + (level() - 1) * 4

    /**
     * The first build drew the cottage as a solid slab. Shrink that footprint
     * so an already-saved day still has a door, a path, and soil that can be hoed.
     */
    private fun settleYard() {
        for (y in 0 until H) {
            for (x in 0 until W) {
                val door = x == DOOR_X && y == 5
                val cottage = y in 1..4 && x in 4..6
                val lane = (x == 6 && y >= 5) || (x == DOOR_X && y == 6)
                when {
                    door -> farm[y][x] = DOOR
                    cottage -> farm[y][x] = BLOCK
                    farm[y][x] == BLOCK -> farm[y][x] = GRASS
                    lane && farm[y][x] == GRASS -> farm[y][x] = PATH
                }
                if (door || cottage) {
                    crop[y][x] = 0
                    stage[y][x] = 0
                }
            }
        }
        if (!indoors && px in 0 until W && py in 0 until H && solid(px, py)) {
            px = 6
            py = 7
        }
    }

    private fun width() = if (indoors) HW else W
    private fun height() = if (indoors) HH else H

    private fun inside(x: Int, y: Int) = x in 0 until width() && y in 0 until height()

    private fun tile(x: Int, y: Int): Int {
        if (indoors) return if (x == DOOR_X && y == HH - 1) DOOR else 0
        return farm[y][x]
    }

    private fun solid(x: Int, y: Int): Boolean {
        if (indoors) {
            if (x == DOOR_X && y == HH - 1) return false
            if (x == 0 || y == 0 || x == HW - 1 || y == HH - 1) return true
            return house[y][x] != 0
        }
        return farm[y][x] == BLOCK
    }

    fun export(): String {
        val bagText = bag.entries.joinToString(",") { "${it.key}:${it.value}" }
        val farmText = farm.joinToString(";") { it.joinToString(",") }
        val cropText = crop.joinToString(";") { it.joinToString(",") }
        val stageText = stage.joinToString(";") { it.joinToString(",") }
        val houseText = house.joinToString(";") { it.joinToString(",") }
        return listOf(
            "v1", day, minute, stamina, gold, px, py, if (indoors) 1 else 0, tool.name, seed,
            xp, quest, harvested, sold, placed, wateredEver, plantedEver, tilledEver, slept, daily,
            facingX, facingY, bagText, farmText, cropText, stageText, houseText, furnish
        ).joinToString("|")
    }

    private fun restore(raw: String): Boolean {
        val parts = raw.split("|")
        if (parts.size < 27 || parts[0] != "v1") return false
        return try {
            day = parts[1].toInt()
            minute = parts[2].toInt().coerceIn(0, 24 * 60)
            stamina = parts[3].toInt().coerceAtLeast(0)
            gold = parts[4].toInt().coerceAtLeast(0)
            px = parts[5].toInt()
            py = parts[6].toInt()
            indoors = parts[7] == "1"
            tool = Tool.valueOf(parts[8])
            seed = parts[9]
            xp = parts[10].toInt()
            quest = parts[11].toInt().coerceIn(0, 7)
            harvested = parts[12].toInt()
            sold = parts[13].toInt()
            placed = parts[14].toInt()
            wateredEver = parts[15].toInt()
            plantedEver = parts[16].toInt()
            tilledEver = parts[17].toInt()
            slept = parts[18].toInt()
            daily = parts[19].toInt()
            facingX = parts[20].toInt()
            facingY = parts[21].toInt()
            bag.clear()
            if (parts[22].isNotBlank()) {
                for (pair in parts[22].split(",")) {
                    val bits = pair.split(":")
                    if (bits.size == 2) bag[bits[0]] = bits[1].toInt().coerceAtLeast(0)
                }
            }
            readGrid(parts[23], farm)
            readGrid(parts[24], crop)
            readGrid(parts[25], stage)
            readGrid(parts[26], house)
            furnish = parts.getOrNull(27)?.takeIf { it in FURNITURE.values } ?: "chair"
            notice = QUEST.getValue(quest)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun readGrid(text: String, into: Array<IntArray>) {
        val rows = text.split(";")
        for (y in into.indices) {
            val cols = if (y < rows.size) rows[y].split(",") else emptyList()
            for (x in into[y].indices) {
                into[y][x] = cols.getOrNull(x)?.toIntOrNull() ?: 0
            }
        }
    }

    enum class Tool { HOE, CAN, SEED, HAND }

    companion object {
        const val W = 12
        const val H = 16
        const val HW = 10
        const val HH = 8
        const val GRASS = 0
        const val TILLED = 1
        const val WATERED = 2
        const val PATH = 3
        const val BLOCK = 4
        const val DOOR = 5
        const val DOOR_X = 5

        val CROPS = mapOf("turnip" to 1, "pumpkin" to 2, "berry" to 3)
        val GROW = mapOf(1 to 3, 2 to 4, 3 to 3)
        val NAME = mapOf(1 to "turnip", 2 to "pumpkin", 3 to "berry")
        val SELL = mapOf("turnip" to 45, "pumpkin" to 110, "berry" to 70)
        val BUY = mapOf(
            "turnip_seed" to 15,
            "pumpkin_seed" to 40,
            "berry_seed" to 25,
            "chair" to 60,
            "table" to 90,
            "bed" to 140,
            "lamp" to 50
        )
        val FURNITURE = mapOf(1 to "bed", 2 to "table", 3 to "chair", 4 to "lamp")
        val EAT = mapOf("turnip" to 16, "berry" to 22, "pumpkin" to 34)
        val QUEST = mapOf(
            0 to "Nine Elma tarlayı sana bıraktı. Çapa ile dört tarla aç.",
            1 to "Dört tarlayı sulama kabıyla ıslat.",
            2 to "İki tohum ek.",
            3 to "Bir mahsulü olgunlaştırıp topla.",
            4 to "Mahsulü köy sandığına sat.",
            5 to "Evde uyu. Gece yarısı ayakta kalırsan bayılırsın.",
            6 to "Eve bir eşya koy.",
            7 to "Her gün beş mahsul topla, sandık 80 altın yollasın."
        )

        fun label(item: String): String = when (item) {
            "turnip" -> "Şalgam"
            "pumpkin" -> "Kabak"
            "berry" -> "Böğürtlen"
            "turnip_seed" -> "Şalgam tohumu"
            "pumpkin_seed" -> "Kabak tohumu"
            "berry_seed" -> "Böğürtlen tohumu"
            "chair" -> "Sandalye"
            "table" -> "Masa"
            "bed" -> "Yatak"
            "lamp" -> "Lamba"
            else -> item
        }
    }
}
