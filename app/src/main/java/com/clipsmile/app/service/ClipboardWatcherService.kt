package com.clipsmile.app.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.ClipSmileApp
import com.clipsmile.app.R
import com.clipsmile.app.ui.MainActivity
import com.clipsmile.app.util.ClipCapturePipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Foreground service that keeps sampling the clipboard while the screen is on.
 *
 * Android 10+ does not tell a background app that something was copied, and it
 * hides the clipboard text until this app has window focus. [ClipboardFocusReader]
 * borrows focus for a fraction of a second, the new text is saved, and
 * [com.clipsmile.app.data.RemoteSyncManager] posts it to the server. The user
 * does not open the app or tap send.
 */
class ClipboardWatcherService : android.app.Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var powerManager: PowerManager
    private lateinit var focusReader: ClipboardFocusReader

    private var polling = false
    private var lastDelivered: String? = null
    private var focusMisses = 0
    private var overlayMissingNotified = false

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        // Fires only while this app already has focus (or on older Android).
        // Still useful when the sample window happens to be up during a copy.
        if (AppVisibility.hasVisibleActivity) return@OnPrimaryClipChangedListener
        deliver(readClipboardText())
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                handler.post { sampleClipboard() }
            }
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            sampleClipboard()
            if (polling) handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService<ClipboardManager>()!!
        powerManager = getSystemService<PowerManager>()!!
        focusReader = ClipboardFocusReader(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        clipboardManager.addPrimaryClipChangedListener(clipListener)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        polling = true
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        polling = false
        handler.removeCallbacksAndMessages(null)
        focusReader.cancel()
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
            // Not registered.
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (prefsMonitoringEnabled()) {
            start(applicationContext)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun sampleClipboard() {
        if (!polling) return
        if (!powerManager.isInteractive) return
        if (AppVisibility.hasVisibleActivity) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (!overlayMissingNotified) {
                postStatus(this, getString(R.string.notification_need_overlay))
                overlayMissingNotified = true
            }
            return
        }
        overlayMissingNotified = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            deliver(readClipboardText())
            return
        }
        if (focusReader.inFlight) return
        focusReader.read { text, hadFocus ->
            if (!hadFocus) {
                focusMisses += 1
                if (focusMisses == FOCUS_MISS_NOTICE) {
                    postStatus(this, getString(R.string.notification_focus_failed))
                }
                return@read
            }
            focusMisses = 0
            deliver(text)
        }
    }

    private fun deliver(text: String?) {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == lastDelivered) return
        lastDelivered = trimmed
        ClipCapturePipeline.captureAsync(serviceScope, this, trimmed)
        postStatus(this, getString(R.string.notification_sent))
    }

    private fun prefsMonitoringEnabled(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MONITORING_ENABLED, true)
    }

    private fun readClipboardText(): String? {
        return try {
            val clip = clipboardManager.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(this)?.toString()
        } catch (_: SecurityException) {
            null
        }
    }

    private fun buildNotification(): Notification {
        return notification(this, statusLine ?: getString(R.string.notification_text))
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val POLL_INTERVAL_MS = 2_000L
        private const val FOCUS_MISS_NOTICE = 3
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

        @Volatile
        private var statusLine: String? = null

        fun postStatus(context: Context, text: String) {
            statusLine = text
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification(context, text))
        }

        private fun notification(context: Context, text: String): Notification {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = androidx.core.app.TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(openAppIntent)
                .getPendingIntent(
                    0,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            return NotificationCompat.Builder(context, ClipSmileApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
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
