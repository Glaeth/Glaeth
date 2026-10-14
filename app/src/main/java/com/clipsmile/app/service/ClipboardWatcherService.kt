package com.clipsmile.app.service

import android.app.Notification
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.ClipSmileApp
import com.clipsmile.app.R
import com.clipsmile.app.ui.ClipboardCaptureActivity
import com.clipsmile.app.ui.MainActivity
import com.clipsmile.app.util.ClipCapturePipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Foreground service that keeps a [ClipboardManager.OnPrimaryClipChangedListener]
 * registered so every new copy is written to the local database as soon as it
 * happens, without requiring the user to keep the app open.
 *
 * Note: starting with Android 10 (API 29), the platform only delivers
 * clipboard content to the app that currently has input focus. When this
 * app is not in front, the service cannot read the text itself and instead
 * opens [ClipboardCaptureActivity] for a moment so the copy can be read.
 */
class ClipboardWatcherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var clipboardManager: ClipboardManager

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    override fun onCreate() {
        super.onCreate()
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
        val behindAnotherApp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !AppVisibility.hasVisibleActivity
        if (behindAnotherApp) {
            ClipboardCaptureActivity.launch(this)
            return
        }

        val text = readClipboardText()
        if (text.isNullOrBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ClipboardCaptureActivity.launch(this)
            }
            return
        }
        ClipCapturePipeline.captureAsync(serviceScope, this, text)
    }

    private fun readClipboardText(): String? {
        return try {
            val clip = clipboardManager.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(this)?.toString()
        } catch (e: SecurityException) {
            null
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
