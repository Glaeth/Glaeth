package com.clipsmile.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the "open WhatsApp with each new clip already filled in" preference:
 * whether it is enabled, and the target phone number (digits only,
 * international format, e.g. "905551112233").
 */
class AutoWhatsAppPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var phoneNumber: String
        get() = prefs.getString(KEY_PHONE, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    companion object {
        private const val PREFS_NAME = "clipsmile_auto_whatsapp"
        private const val KEY_ENABLED = "auto_enabled"
        private const val KEY_PHONE = "auto_phone"
    }
}
