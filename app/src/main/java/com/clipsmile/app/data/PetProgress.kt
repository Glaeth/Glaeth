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

    /** Charges refill one at a time, every three hours, and never bank past the cap. */
    fun charges(kind: String): Int = refill(kind)

    fun lifetime(kind: String): Int = prefs.getInt(lifeKey(kind), 0)

    fun tryNuzzle(): Int {
        if (!spend("nuzzle")) return -1
        return bumpLife("nuzzle")
    }

    fun tryFeed(): Boolean {
        if (!spend("feed")) return false
        bumpLife("feed")
        snack(18)
        return true
    }

    fun tryBath(): Boolean {
        if (!spend("bath")) return false
        bumpLife("bath")
        snack(8)
        return true
    }

    fun carePercent(): Int {
        val ready = charges("nuzzle") + charges("feed") + charges("bath")
        return ready * 100 / (CHARGE_MAX * 3)
    }

    private fun spend(kind: String): Boolean {
        val count = refill(kind)
        if (count <= 0) return false
        val editor = prefs.edit().putInt(chargeKey(kind), count - 1)
        if (count >= CHARGE_MAX) editor.putLong(chargeAtKey(kind), System.currentTimeMillis())
        editor.apply()
        return true
    }

    private fun bumpLife(kind: String): Int {
        val next = lifetime(kind) + 1
        prefs.edit().putInt(lifeKey(kind), next).apply()
        return next
    }

    private fun refill(kind: String): Int {
        val now = System.currentTimeMillis()
        var count = prefs.getInt(chargeKey(kind), -1)
        var at = prefs.getLong(chargeAtKey(kind), now)
        if (count < 0) {
            count = CHARGE_START
            prefs.edit().putInt(chargeKey(kind), count).putLong(chargeAtKey(kind), now).apply()
            return count
        }
        if (count >= CHARGE_MAX) {
            prefs.edit().putLong(chargeAtKey(kind), now).apply()
            return CHARGE_MAX
        }
        val gained = ((now - at) / CHARGE_INTERVAL).toInt()
        if (gained <= 0) return count
        val next = (count + gained).coerceAtMost(CHARGE_MAX)
        val nextAt = if (next >= CHARGE_MAX) now else at + gained * CHARGE_INTERVAL
        prefs.edit().putInt(chargeKey(kind), next).putLong(chargeAtKey(kind), nextAt).apply()
        return next
    }

    fun fullness(): Int = decayedFullness()

    fun level(): Int = prefs.getInt(KEY_LEVEL, 1).coerceAtLeast(1)

    fun streak(): Int = prefs.getInt(KEY_STREAK, 0)

    fun visitedToday(): Boolean = prefs.getString(KEY_LAST_DAY, null) == dayKey(0)

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

    private fun nameKey(minLevel: Int) = "dog_name_$minLevel"

    private fun chargeKey(kind: String) = "chg_$kind"

    private fun chargeAtKey(kind: String) = "chg_at_$kind"

    private fun lifeKey(kind: String) = "life_$kind"

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
        private const val KEY_FULL = "fullness"
        private const val KEY_FULL_AT = "fullness_at"
        private const val DEFAULT_NAME = "Çirkin"
        const val CHARGE_MAX = 3
        const val CHARGE_START = 2
        const val CHARGE_INTERVAL = 3L * 60L * 60L * 1000L
        private const val DEFAULT_FULL = 55
        private const val FEED_AMOUNT = 30
        private const val HUNGER_PER_HOUR = 5f
        private const val NAME_LIMIT = 24
    }
}
