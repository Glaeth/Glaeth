package com.clipsmile.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Sending : SyncStatus()
    data class Success(val atMillis: Long) : SyncStatus()
    data class Failed(val atMillis: Long) : SyncStatus()
}

/** Clipboard text stays on the device. The old ntfy upload does not run. */
object RemoteSyncManager {

    private val _lastStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val lastStatus: StateFlow<SyncStatus> = _lastStatus

    fun notifyNewClip(context: Context, text: String) {
        // Internet upload of clipboard copies was removed. Keep the text on the device.
        RemoteSyncPrefs(context).isEnabled = false
        if (text.isEmpty()) return
    }

    suspend fun sendTestMessage(context: Context): Boolean {
        RemoteSyncPrefs(context).isEnabled = false
        return false
    }
}
