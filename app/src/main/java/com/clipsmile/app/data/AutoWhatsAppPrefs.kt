package com.clipsmile.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the "automatically forward every new clip to a WhatsApp contact"
 * preference: whether it is enabled, the target phone number (digits only,
 * international format, e.g. "905551112233"), and whether the experimental
 * "fully automatic" mode is on (which also taps WhatsApp's Send button via
 * an Accessibility Service instead of requiring one manual tap).
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

    var isFullyAutomatic: Boolean
        get() = prefs.getBoolean(KEY_FULLY_AUTOMATIC, false)
        set(value) = prefs.edit().putBoolean(KEY_FULLY_AUTOMATIC, value).apply()

    companion object {
        private const val PREFS_NAME = "clipsmile_auto_whatsapp"
        private const val KEY_ENABLED = "auto_enabled"
        private const val KEY_PHONE = "auto_phone"
        private const val KEY_FULLY_AUTOMATIC = "auto_fully_automatic"
    }
}
