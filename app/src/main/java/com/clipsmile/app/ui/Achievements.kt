package com.clipsmile.app.ui

import android.content.Context
import android.widget.Toast
import com.clipsmile.app.R

object Achievements {
    const val DROP = "drop"
    const val FLAP = "flap"
    const val RUN = "run"

    data class Badge(
        val id: String,
        val kind: String,
        val need: Int,
        val titleRes: Int,
        val detailRes: Int
    )

    val all = listOf(
        Badge("drop_1", DROP, 1, R.string.ach_drop_1_title, R.string.ach_drop_1_detail),
        Badge("drop_10", DROP, 10, R.string.ach_drop_10_title, R.string.ach_drop_10_detail),
        Badge("drop_25", DROP, 25, R.string.ach_drop_25_title, R.string.ach_drop_25_detail),
        Badge("flap_1", FLAP, 1, R.string.ach_flap_1_title, R.string.ach_flap_1_detail),
        Badge("flap_5", FLAP, 5, R.string.ach_flap_5_title, R.string.ach_flap_5_detail),
        Badge("flap_15", FLAP, 15, R.string.ach_flap_15_title, R.string.ach_flap_15_detail),
        Badge("run_1", RUN, 1, R.string.ach_run_1_title, R.string.ach_run_1_detail),
        Badge("run_100", RUN, 100, R.string.ach_run_100_title, R.string.ach_run_100_detail),
        Badge("run_500", RUN, 500, R.string.ach_run_500_title, R.string.ach_run_500_detail)
    )

    private const val PREFS = "clipsmile_pet"

    fun sectionTitle(kind: String): Int = when (kind) {
        DROP -> R.string.ach_section_drop
        FLAP -> R.string.ach_section_flap
        else -> R.string.ach_section_run
    }

    fun isUnlocked(context: Context, id: String): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(id), false)
    }

    fun unlockedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return all.count { prefs.getBoolean(key(it.id), false) }
    }

    fun celebrate(context: Context, kind: String, score: Int) {
        val title = grant(context, kind, score) ?: return
        Toast.makeText(
            context,
            context.getString(R.string.achievement_unlocked_toast, context.getString(title)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun grant(context: Context, kind: String, score: Int): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var best: Badge? = null
        for (badge in all) {
            if (badge.kind != kind || score < badge.need) continue
            if (prefs.getBoolean(key(badge.id), false)) continue
            editor.putBoolean(key(badge.id), true)
            if (best == null || badge.need > best.need) best = badge
        }
        if (best == null) return null
        editor.apply()
        return best.titleRes
    }

    private fun key(id: String) = "ach_$id"
}
