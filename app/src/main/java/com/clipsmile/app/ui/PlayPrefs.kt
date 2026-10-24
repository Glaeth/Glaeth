package com.clipsmile.app.ui

import android.content.Context

/** Music and effect switches from the settings screen. */
object PlayPrefs {
    private const val PREFS = "clipsmile_play"
    private const val MUSIC = "music"
    private const val EFFECTS = "effects"

    fun music(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(MUSIC, true)

    fun effects(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(EFFECTS, true)

    fun setMusic(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(MUSIC, enabled).apply()
    }

    fun setEffects(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(EFFECTS, enabled).apply()
    }
}
