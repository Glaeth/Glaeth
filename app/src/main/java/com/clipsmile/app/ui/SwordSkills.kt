package com.clipsmile.app.ui

import android.content.Context

/**
 * Sword-gate skill tree. One point arrives with each level.
 * Killing a soldier or an archer is worth 5 XP. A level is 40 XP.
 */
class SwordSkills(raw: String? = null) {
    var xp = 0
    val rank = HashMap<String, Int>()
    val bar = Array(3) { "" }

    init {
        if (!raw.isNullOrBlank()) restore(raw)
    }

    fun level(): Int = 1 + xp / XP_PER_LEVEL

    fun points(): Int = (level() - rank.values.sum()).coerceAtLeast(0)

    fun progress(): Float = (xp % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat()

    /** @return true when this kill opened a new level */
    fun addKill(): Boolean {
        val before = level()
        xp += 5
        return level() > before
    }

    fun rankOf(id: String): Int = rank[id] ?: 0

    fun canRaise(id: String): Boolean {
        val skill = skill(id) ?: return false
        if (rankOf(id) >= MAX_RANK) return false
        if (points() < 1) return false
        if (skill.need.isNotEmpty() && rankOf(skill.need) < skill.needRank) return false
        return true
    }

    fun raise(id: String): String {
        val skill = skill(id) ?: return "Bilinmeyen yetenek."
        if (rankOf(id) >= MAX_RANK) return "Bu yetenek dolu."
        if (skill.need.isNotEmpty() && rankOf(skill.need) < skill.needRank) {
            val other = skill(skill.need)
            return "${other?.name ?: skill.need} önce açılmalı."
        }
        if (points() < 1) return "Yetenek puanı yok."
        rank[id] = rankOf(id) + 1
        return if (rankOf(id) == 1) "${skill.name} açıldı." else "${skill.name} ${rankOf(id)}. seviye."
    }

    fun assign(slot: Int, id: String): String {
        if (slot !in bar.indices) return ""
        if (rankOf(id) <= 0) return "Önce yeteneği aç."
        for (index in bar.indices) {
            if (bar[index] == id) bar[index] = ""
        }
        bar[slot] = id
        return "${skill(id)?.name ?: id} ${slot + 1}. yuvada."
    }

    fun clear(slot: Int) {
        if (slot in bar.indices) bar[slot] = ""
    }

    fun swap(first: Int, second: Int) {
        if (first !in bar.indices || second !in bar.indices || first == second) return
        val kept = bar[first]
        bar[first] = bar[second]
        bar[second] = kept
    }

    fun export(): String {
        val ranks = rank.entries
            .filter { it.value > 0 }
            .joinToString(",") { "${it.key}:${it.value.coerceIn(0, MAX_RANK)}" }
        return "$xp|$ranks|${bar.joinToString(",")}"
    }

    private fun restore(raw: String) {
        val parts = raw.split("|")
        if (parts.size < 3) return
        xp = parts[0].toIntOrNull()?.coerceAtLeast(0) ?: 0
        rank.clear()
        if (parts[1].isNotBlank()) {
            for (pair in parts[1].split(",")) {
                val bits = pair.split(":")
                val id = bits.getOrNull(0) ?: continue
                if (skill(id) == null) continue
                rank[id] = bits.getOrNull(1)?.toIntOrNull()?.coerceIn(0, MAX_RANK) ?: 0
            }
        }
        val slots = parts[2].split(",")
        for (index in bar.indices) {
            val id = slots.getOrNull(index).orEmpty()
            bar[index] = if (rankOf(id) > 0) id else ""
        }
    }

    data class Skill(
        val id: String,
        val name: String,
        val blurb: String,
        val need: String,
        val needRank: Int,
        val icon: String,
        val fx: String
    )

    companion object {
        const val XP_PER_LEVEL = 40
        const val MAX_RANK = 3
        private const val PREFS = "clipsmile_pet"
        private const val KEY = "sword_skills"

        val all = listOf(
            Skill("fire", "Alev", "Ateş topu ilk rakibi yakar.", "", 0, "skills/fire.png", "fx/fire.png"),
            Skill("ice", "Buz", "Top, rakibi yavaşlatır, öldürmez.", "fire", 1, "skills/ice.png", "fx/water.png"),
            Skill("heal", "Şifa", "Bir can doldurur.", "", 0, "skills/heal.png", "fx/green.png"),
            Skill("ward", "Zırh", "Kısa süre ok ve kılıç işlemez.", "heal", 1, "skills/ward.png", "fx/purple.png"),
            Skill("bolt", "Yıldırım", "En uzaktaki okçuya düşer.", "fire", 2, "skills/bolt.png", "fx/purple.png"),
            Skill("vine", "Diken", "En yakın askeri yere kökler.", "ice", 1, "skills/vine.png", "fx/green.png"),
            Skill("slash", "Kesik", "Sonraki savuruş daha uzağa erer.", "", 0, "skills/slash.png", "fx/fire.png"),
            Skill("dash", "Hamle", "Öne atılır, değdiğini keser.", "slash", 1, "skills/dash.png", "fx/water.png")
        )

        fun skill(id: String): Skill? = all.firstOrNull { it.id == id }

        fun load(context: Context): SwordSkills {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            return SwordSkills(raw)
        }

        fun save(context: Context, skills: SwordSkills) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, skills.export())
                .apply()
        }
    }
}
