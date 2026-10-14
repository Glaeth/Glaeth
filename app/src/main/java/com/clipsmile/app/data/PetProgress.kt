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

    data class Snapshot(val streak: Int, val level: Int)

    fun recordVisit(): Snapshot {
        val today = dayKey(0)
        val last = prefs.getString(KEY_LAST_DAY, null)
        var streak = prefs.getInt(KEY_STREAK, 0)
        var level = prefs.getInt(KEY_LEVEL, 0)

        if (last == today && streak > 0 && level > 0) {
            return Snapshot(streak, level)
        }

        streak = if (last == dayKey(-1) && streak > 0) streak + 1 else 1
        level += 1
        prefs.edit()
            .putString(KEY_LAST_DAY, today)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_LEVEL, level)
            .apply()
        return Snapshot(streak, level)
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
    }
}
