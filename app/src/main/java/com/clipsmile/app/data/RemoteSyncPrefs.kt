package com.clipsmile.app.data

import android.content.Context
import android.content.SharedPreferences
import com.clipsmile.app.util.NtfyClient
import kotlin.random.Random

/**
 * Stores the user's "send everything to the internet" preference: whether it
 * is enabled, and which ntfy.sh channel (topic) the clips are published to.
 * The channel name is the secret part of the otherwise-unauthenticated
 * public URL `https://ntfy.sh/<channel>`.
 */
class RemoteSyncPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var channel: String
        get() = prefs.getString(KEY_CHANNEL, null) ?: generateChannel().also { channel = it }
        set(value) = prefs.edit().putString(KEY_CHANNEL, value).apply()

    val channelUrl: String
        get() = NtfyClient.BASE_URL + channel

    fun generateChannel(): String {
        val suffix = (1..10)
            .map { ALPHABET[Random.nextInt(ALPHABET.length)] }
            .joinToString("")
        return "clipsmile-$suffix"
    }

    companion object {
        private const val PREFS_NAME = "clipsmile_remote_sync"
        private const val KEY_ENABLED = "sync_enabled"
        private const val KEY_CHANNEL = "sync_channel"
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
