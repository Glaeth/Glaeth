package com.clipsmile.app.service

import android.app.Notification
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.ClipSmileApp
import com.clipsmile.app.R
import com.clipsmile.app.data.ClipRepository
import com.clipsmile.app.data.RemoteSyncManager
import com.clipsmile.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps a [ClipboardManager.OnPrimaryClipChangedListener]
 * registered so every new copy is written to the local database as soon as it
 * happens, without requiring the user to keep the app open.
 *
 * Note: starting with Android 10 (API 29), the platform only delivers
 * clipboard content to the app that currently has input focus (or is the
 * default IME), as a privacy protection. This service still captures copies
 * made while [MainActivity] is in the foreground and copies made right
 * before switching back to this app; [MainActivity] also registers its own
 * listener while visible as a reliable fallback.
 */
class ClipboardWatcherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: ClipRepository
    private lateinit var clipboardManager: ClipboardManager

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    override fun onCreate() {
        super.onCreate()
        repository = ClipRepository.getInstance(applicationContext)
        clipboardManager = getSystemService<ClipboardManager>()!!
        startForeground(NOTIFICATION_ID, buildNotification())
        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleClipboardChange() {
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return

        serviceScope.launch {
            if (repository.saveIfNew(text)) {
                RemoteSyncManager.notifyNewClip(applicationContext, text)
            }
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = androidx.core.app.TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(openAppIntent)
            .getPendingIntent(
                0,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(this, ClipSmileApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101

        fun start(context: Context) {
            val intent = Intent(context, ClipboardWatcherService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClipboardWatcherService::class.java))
        }
    }
}
