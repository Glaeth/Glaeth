package com.clipsmile.app.service

import android.app.Notification
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away used to kill monitoring. Start it again so a
        // later copy is still seen while the process is alive.
        if (prefsMonitoringEnabled()) {
            start(applicationContext)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun handleClipboardChange() {
        val text = readClipboardText()
        if (AppVisibility.hasVisibleActivity) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
            postStatus(this, getString(R.string.notification_need_overlay))
        }
        ClipboardCaptureActivity.launch(this, text)
    }

    private fun prefsMonitoringEnabled(): Boolean {
        val prefs = getSharedPreferences("clipsmile_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("monitoring_enabled", true)
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
            .setContentText(statusLine ?: getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101

        @Volatile
        private var statusLine: String? = null

        fun postStatus(context: Context, text: String) {
            statusLine = text
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = androidx.core.app.TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(openAppIntent)
                .getPendingIntent(
                    0,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            val notification = NotificationCompat.Builder(context, ClipSmileApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        }

        fun start(context: Context) {
            val intent = Intent(context, ClipboardWatcherService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClipboardWatcherService::class.java))
        }
    }
}
