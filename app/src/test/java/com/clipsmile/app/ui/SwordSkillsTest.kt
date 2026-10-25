package com.clipsmile.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwordSkillsTest {
    @Test
    fun aNewFighterHasOnePointAndCannotSkipTheTree() {
        val skills = SwordSkills()
        assertEquals(1, skills.level())
        assertEquals(1, skills.points())
        assertFalse(skills.canRaise("ice"))
        assertTrue(skills.raise("ice").contains("önce"))
        assertEquals("Alev açıldı.", skills.raise("fire"))
        assertEquals(0, skills.points())
        assertFalse(skills.canRaise("bolt"))
    }

    @Test
    fun eightKillsOpenTheNextPoint() {
        val skills = SwordSkills()
        skills.raise("fire")
        var leveled = false
        repeat(8) { leveled = skills.addKill() || leveled }
        assertEquals(2, skills.level())
        assertEquals(40, skills.xp)
        assertTrue(leveled)
        assertEquals(1, skills.points())
        assertEquals("Alev 2. seviye.", skills.raise("fire"))
    }

    @Test
    fun hotbarAssignsClearsAndSwaps() {
        val skills = SwordSkills()
        skills.raise("slash")
        skills.xp = 40
        skills.raise("heal")
        assertEquals("Kesik 1. yuvada.", skills.assign(0, "slash"))
        skills.assign(1, "heal")
        skills.swap(0, 1)
        assertEquals("heal", skills.bar[0])
        assertEquals("slash", skills.bar[1])
        skills.clear(0)
        assertEquals("", skills.bar[0])
        val copy = SwordSkills(skills.export())
        assertEquals("slash", copy.bar[1])
        assertEquals(1, copy.rankOf("slash"))
        assertEquals(1, copy.rankOf("heal"))
    }
}
