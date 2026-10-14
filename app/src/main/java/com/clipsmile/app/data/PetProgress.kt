package com.clipsmile.app.data

import android.content.Context
import java.util.Calendar

/**
 * Duolingo-style visit record for the pet on the home screen.
 * Opening the app on a new calendar day raises the streak when the previous
 * visit was yesterday, and always raises the dog's lifetime level.
 */
class PetProgress(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Snapshot(val streak: Int, val level: Int, val name: String, val fullness: Int)

    var petName: String
        get() = displayName(selectedLevel(), DEFAULT_NAME)
        set(value) {
            setName(selectedLevel(), value)
        }

    fun selectedLevel(): Int = prefs.getInt(KEY_SELECTED, 1).coerceAtLeast(1)

    fun setSelectedLevel(level: Int) {
        prefs.edit().putInt(KEY_SELECTED, level).apply()
    }

    fun displayName(minLevel: Int, fallback: String): String {
        val stored = prefs.getString(nameKey(minLevel), null)?.takeIf { it.isNotBlank() }
        if (stored != null) return stored
        if (minLevel == 1) {
            val legacy = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            if (legacy != null) return legacy
        }
        return fallback
    }

    fun setName(minLevel: Int, value: String) {
        val cleaned = value.trim().take(NAME_LIMIT)
        if (cleaned.isEmpty()) return
        val editor = prefs.edit().putString(nameKey(minLevel), cleaned)
        if (minLevel == 1) editor.putString(KEY_NAME, cleaned)
        editor.apply()
    }

    fun rememberName(minLevel: Int, fallback: String) {
        if (!prefs.getString(nameKey(minLevel), null).isNullOrBlank()) return
        val legacy = if (minLevel == 1) prefs.getString(KEY_NAME, null) else null
        setName(minLevel, legacy?.takeIf { it.isNotBlank() } ?: fallback)
    }

    fun unlockDate(minLevel: Int): String? = prefs.getString(unlockKey(minLevel), null)

    fun formatUnlock(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val parts = iso.split("-")
        if (parts.size != 3) return iso
        return "${parts[2]}.${parts[1]}.${parts[0]}"
    }

    fun stampUnlocks(level: Int, gates: List<Int>) {
        val today = dayKey(0)
        val editor = prefs.edit()
        var changed = false
        for (gate in gates) {
            if (level >= gate && prefs.getString(unlockKey(gate), null) == null) {
                editor.putString(unlockKey(gate), today)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    fun claimFreshUnlock(minLevel: Int): Boolean {
        val seen = prefs.getInt(KEY_SEEN, 1)
        if (minLevel <= seen) return false
        prefs.edit().putInt(KEY_SEEN, minLevel).apply()
        return true
    }

    fun feedsToday(): Int {
        rollCareDay()
        return prefs.getInt(KEY_FEEDS, 0)
    }

    fun bathsToday(): Int {
        rollCareDay()
        return prefs.getInt(KEY_BATHS, 0)
    }

    fun tryFeed(): Boolean {
        rollCareDay()
        val count = prefs.getInt(KEY_FEEDS, 0)
        if (count >= FEEDS_PER_DAY) return false
        prefs.edit().putInt(KEY_FEEDS, count + 1).apply()
        snack(18)
        return true
    }

    fun tryBath(): Boolean {
        rollCareDay()
        val count = prefs.getInt(KEY_BATHS, 0)
        if (count >= BATHS_PER_DAY) return false
        prefs.edit().putInt(KEY_BATHS, count + 1).apply()
        snack(8)
        return true
    }

    fun carePercent(): Int {
        val done = feedsToday() + bathsToday()
        return done * 100 / (FEEDS_PER_DAY + BATHS_PER_DAY)
    }

    fun fullness(): Int = decayedFullness()

    fun level(): Int = prefs.getInt(KEY_LEVEL, 1).coerceAtLeast(1)

    fun feed(): Int = snack(FEED_AMOUNT)

    fun snack(amount: Int): Int {
        val next = (decayedFullness() + amount).coerceAtMost(100)
        prefs.edit()
            .putInt(KEY_FULL, next)
            .putLong(KEY_FULL_AT, System.currentTimeMillis())
            .apply()
        return next
    }

    fun recordVisit(): Snapshot {
        val today = dayKey(0)
        val last = prefs.getString(KEY_LAST_DAY, null)
        var streak = prefs.getInt(KEY_STREAK, 0)
        var level = prefs.getInt(KEY_LEVEL, 0)

        if (last != today || streak == 0 || level == 0) {
            streak = if (last == dayKey(-1) && streak > 0) streak + 1 else 1
            level += 1
            prefs.edit()
                .putString(KEY_LAST_DAY, today)
                .putInt(KEY_STREAK, streak)
                .putInt(KEY_LEVEL, level)
                .apply()
        }
        return Snapshot(streak, level, petName, decayedFullness())
    }

    private fun decayedFullness(): Int {
        val saved = prefs.getInt(KEY_FULL, DEFAULT_FULL)
        val at = prefs.getLong(KEY_FULL_AT, System.currentTimeMillis())
        val elapsedHours = (System.currentTimeMillis() - at) / 3_600_000f
        return (saved - elapsedHours * HUNGER_PER_HOUR).toInt().coerceIn(0, 100)
    }

    private fun rollCareDay() {
        val today = dayKey(0)
        if (prefs.getString(KEY_CARE_DAY, null) == today) return
        prefs.edit()
            .putString(KEY_CARE_DAY, today)
            .putInt(KEY_FEEDS, 0)
            .putInt(KEY_BATHS, 0)
            .apply()
    }

    private fun nameKey(minLevel: Int) = "dog_name_$minLevel"

    private fun unlockKey(minLevel: Int) = "unlock_$minLevel"

    private fun dayKey(dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        if (dayOffset != 0) calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_pet"
        private const val KEY_LAST_DAY = "last_open_day"
        private const val KEY_STREAK = "streak"
        private const val KEY_LEVEL = "level"
        private const val KEY_NAME = "pet_name"
        private const val KEY_SELECTED = "selected_dog_level"
        private const val KEY_SEEN = "seen_dog_level"
        private const val KEY_CARE_DAY = "care_day"
        private const val KEY_FEEDS = "feeds_today"
        private const val KEY_BATHS = "baths_today"
        private const val KEY_FULL = "fullness"
        private const val KEY_FULL_AT = "fullness_at"
        private const val DEFAULT_NAME = "Çirkin"
        const val FEEDS_PER_DAY = 4
        const val BATHS_PER_DAY = 3
        private const val DEFAULT_FULL = 55
        private const val FEED_AMOUNT = 30
        private const val HUNGER_PER_HOUR = 5f
        private const val NAME_LIMIT = 24
    }
}
