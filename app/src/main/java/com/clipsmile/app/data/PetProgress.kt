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
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
        set(value) {
            val cleaned = value.trim().take(NAME_LIMIT)
            if (cleaned.isEmpty()) return
            prefs.edit().putString(KEY_NAME, cleaned).apply()
        }

    fun fullness(): Int = decayedFullness()

    fun feed(): Int {
        val next = (decayedFullness() + FEED_AMOUNT).coerceAtMost(100)
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
        private const val KEY_FULL = "fullness"
        private const val KEY_FULL_AT = "fullness_at"
        private const val DEFAULT_NAME = "Çirkin"
        private const val DEFAULT_FULL = 55
        private const val FEED_AMOUNT = 30
        private const val HUNGER_PER_HOUR = 5f
        private const val NAME_LIMIT = 24
    }
}
