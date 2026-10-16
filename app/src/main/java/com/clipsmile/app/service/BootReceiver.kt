package com.clipsmile.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts clipboard monitoring after a reboot so copies keep uploading without
 * the user opening the app again.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MONITORING_ENABLED, true)) return
        try {
            ClipboardWatcherService.start(context)
        } catch (_: Exception) {
            // Android may refuse a foreground start from this broadcast.
        }
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}
