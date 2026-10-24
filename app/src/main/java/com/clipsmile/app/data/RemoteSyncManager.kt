package com.clipsmile.app.data

import android.content.Context
import com.clipsmile.app.util.NtfyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Sending : SyncStatus()
    data class Success(val atMillis: Long) : SyncStatus()
    data class Failed(val atMillis: Long) : SyncStatus()
}

/**
 * Fires off a background publish to the configured ntfy.sh channel every time
 * a new clip is captured, so it is actively pushed to "a place the user can
 * reach" (any browser, or the ntfy app on another device) in real time.
 *
 * This lives alongside [ClipRepository] as another singleton side-effect of
 * saving a new clip: the UI and the background service both call
 * [notifyNewClip] right after a successful local save.
 */
object RemoteSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lastStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val lastStatus: StateFlow<SyncStatus> = _lastStatus

    fun notifyNewClip(context: Context, text: String) {
        val prefs = RemoteSyncPrefs(context)
        if (!prefs.isEnabled) return

        scope.launch {
            publishAndTrackStatus(channel = prefs.channel, message = text, title = "Yeni kopya", tags = "clipboard")
        }
    }

    suspend fun sendTestMessage(context: Context): Boolean {
        val prefs = RemoteSyncPrefs(context)
        return publishAndTrackStatus(
            channel = prefs.channel,
            message = "Bu bir test mesajidir. Her sey yolunda calisiyor!",
            title = ":) test",
            tags = "white_check_mark"
        )
    }

    private suspend fun publishAndTrackStatus(
        channel: String,
        message: String,
        title: String,
        tags: String
    ): Boolean {
        _lastStatus.value = SyncStatus.Sending
        val success = NtfyClient.publish(channel = channel, message = message, title = title, tags = tags)
        _lastStatus.value = if (success) {
            SyncStatus.Success(System.currentTimeMillis())
        } else {
            SyncStatus.Failed(System.currentTimeMillis())
        }
        return success
    }
}
