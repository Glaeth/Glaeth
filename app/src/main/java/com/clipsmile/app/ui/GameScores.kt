package com.clipsmile.app.ui

import android.content.Context

object GameScores {
    const val BEST_DROP = "best_food_drop"
    const val BEST_FLAP = "best_flappy"

    private const val PREFS = "clipsmile_pet"

    fun best(context: Context, key: String): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key, 0)
    }

    fun submit(context: Context, key: String, score: Int): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val best = maxOf(prefs.getInt(key, 0), score)
        prefs.edit().putInt(key, best).apply()
        return best
    }
}
