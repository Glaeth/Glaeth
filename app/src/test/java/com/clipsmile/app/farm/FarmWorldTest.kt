package com.clipsmile.app.farm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmWorldTest {
    @Test
    fun doorEntersAndLeavesTheHouse() {
        val world = FarmWorld()
        assertFalse(world.indoors)
        walk(world, 0, -1)
        walk(world, 0, -1)
        val entered = world.move(-1, 0)
        assertEquals("", entered)
        assertTrue(world.indoors)
        assertEquals(FarmWorld.DOOR_X, world.px)
        assertEquals(FarmWorld.HH - 2, world.py)

        world.move(0, 1)
        assertFalse("evden çıkılamadı", world.indoors)
        assertEquals(6, world.px)
        assertEquals(5, world.py)
    }

    @Test
    fun wallsAndExitStayClear() {
        val world = FarmWorld()
        walk(world, 0, -1)
        walk(world, 0, -1)
        world.move(-1, 0)
        assertEquals("Geçilmez.", world.move(-5, 0))
        assertTrue(world.indoors)
        world.furnish = "bed"
        world.tool = FarmWorld.Tool.HAND
        // Face the exit and try to drop the bed on it.
        world.facingX = 0
        world.facingY = 1
        assertEquals("Kapının önü boş kalsın.", world.use())
        assertEquals(1, world.count("bed"))
        world.facingX = 0
        world.facingY = -1
        assertEquals("Eşya kondu.", world.use())
        assertEquals("Geçilmez.", world.move(0, -1))
    }

    @Test
    fun storyTillWaterPlantSleepHarvestSell() {
        val world = FarmWorld()
        val plots = ArrayList<Pair<Int, Int>>()
        // Open four grass tiles beside the path.
        world.px = 5
        world.py = 8
        world.facingX = -1
        world.facingY = 0
        world.tool = FarmWorld.Tool.HOE
        repeat(4) {
            val message = world.use()
            assertEquals("Toprak açıldı.", message)
            plots.add(world.px + world.facingX to world.py + world.facingY)
            world.move(0, 1)
        }
        assertEquals(1, world.quest)

        world.tool = FarmWorld.Tool.CAN
        for ((x, y) in plots) {
            world.px = x + 1
            world.py = y
            world.facingX = -1
            world.facingY = 0
            assertEquals("Sulandı.", world.use())
        }
        assertEquals(2, world.quest)

        world.tool = FarmWorld.Tool.SEED
        world.seed = "turnip"
        repeat(2) { index ->
            val plot = plots[index]
            world.px = plot.first + 1
            world.py = plot.second
            world.facingX = -1
            world.facingY = 0
            assertEquals("Ekildi.", world.use())
        }
        assertEquals(3, world.quest)
        assertEquals(6, world.count("turnip_seed"))

        // Two watered nights bring a turnip from stage 1 to 3.
        goIndoors(world)
        assertEquals("Gün doğdu. Mahsuller büyüdü.", world.sleep())
        assertEquals(2, world.day)
        leaveHouse(world)
        waterAll(world)
        goIndoors(world)
        world.sleep()
        leaveHouse(world)

        world.tool = FarmWorld.Tool.HAND
        val ripe = plots[0]
        world.px = ripe.first + 1
        world.py = ripe.second
        world.facingX = -1
        world.facingY = 0
        assertTrue(world.use().contains("toplandı"))
        assertEquals(4, world.quest)
        assertEquals(1, world.count("turnip"))

        assertTrue(world.sell("turnip").endsWith("altın."))
        assertEquals(6, world.quest)
        goIndoors(world)
        world.furnish = "chair"
        world.tool = FarmWorld.Tool.HAND
        world.facingX = 0
        world.facingY = -1
        assertEquals("Eşya kondu.", world.use())
        assertEquals(7, world.quest)
    }

    @Test
    fun midnightFaintCostsGoldAndMorningStartsAtSix() {
        val world = FarmWorld()
        val before = world.gold
        world.minute = 23 * 60 + 50
        world.advance(20)
        assertEquals(2, world.day)
        assertEquals(6 * 60, world.minute)
        assertTrue(world.gold < before)
        assertEquals(world.maxStamina() / 2, world.stamina)
        assertTrue(world.notice.contains("bayıldın"))
    }

    @Test
    fun rainRewatersAndPricesRiseLater() {
        val world = FarmWorld()
        world.farm[8][4] = FarmWorld.WATERED
        world.crop[8][4] = 1
        world.stage[8][4] = 1
        world.day = 3
        goIndoors(world)
        world.sleep()
        assertEquals(4, world.day)
        assertTrue(world.raining())
        assertEquals(FarmWorld.WATERED, world.farm[8][4])
        assertEquals(2, world.stage[8][4])
    }

    @Test
    fun eatRestoresStaminaAndSaveRoundTrips() {
        val world = FarmWorld()
        world.stamina = 4
        world.give("turnip", 1)
        assertEquals("Enerji +16.", world.eat("turnip"))
        assertEquals(20, world.stamina)
        world.furnish = "lamp"
        val copy = FarmWorld(world.export())
        assertEquals(world.day, copy.day)
        assertEquals(world.gold, copy.gold)
        assertEquals(world.stamina, copy.stamina)
        assertEquals("lamp", copy.furnish)
        assertEquals(world.farm[5][FarmWorld.DOOR_X], copy.farm[5][FarmWorld.DOOR_X])
    }

    @Test
    fun dailyDeliveryPaysAfterTheStory() {
        val world = FarmWorld()
        world.quest = 7
        world.daily = 4
        world.gold = 10
        world.crop[8][4] = 1
        world.stage[8][4] = 3
        world.farm[8][4] = FarmWorld.TILLED
        world.px = 5
        world.py = 8
        world.facingX = -1
        world.facingY = 0
        world.tool = FarmWorld.Tool.HAND
        val message = world.use()
        assertTrue(message.contains("80 altın"))
        assertEquals(90, world.gold)
        assertEquals(0, world.daily)
    }

    @Test
    fun brokenSaveStartsAFreshFarm() {
        val world = FarmWorld("nope")
        assertEquals(1, world.day)
        assertEquals(8, world.count("turnip_seed"))
    }

    private fun walk(world: FarmWorld, dx: Int, dy: Int) {
        assertEquals("", world.move(dx, dy))
    }

    private fun goIndoors(world: FarmWorld) {
        world.px = 6
        world.py = 5
        world.indoors = false
        assertEquals("", world.move(-1, 0))
        assertTrue(world.indoors)
    }

    private fun leaveHouse(world: FarmWorld) {
        var guard = 0
        while (world.indoors && guard++ < 6) {
            assertEquals("", world.move(0, 1))
        }
        assertFalse(world.indoors)
    }

    private fun waterAll(world: FarmWorld) {
        world.tool = FarmWorld.Tool.CAN
        for (y in 0 until FarmWorld.H) {
            for (x in 0 until FarmWorld.W) {
                if (world.crop[y][x] == 0 || world.farm[y][x] != FarmWorld.TILLED) continue
                world.px = x + 1
                world.py = y
                world.facingX = -1
                world.facingY = 0
                world.use()
            }
        }
    }
}
