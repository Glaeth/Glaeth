package com.clipsmile.app.service

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.util.Calendar
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.clipsmile.app.AppVisibility
import com.clipsmile.app.ClipSmileApp
import com.clipsmile.app.R
import com.clipsmile.app.data.Den
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
    private var overlayMissingNotified = false
    private var captionHour = -1

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
        refreshCaption()
        if (!powerManager.isInteractive) return
        if (AppVisibility.hasVisibleActivity) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (!overlayMissingNotified) {
                postStatus(this, getString(R.string.notification_need_overlay, com.clipsmile.app.ui.DogRoster.homeName(this)))
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
            if (!hadFocus) return@read
            deliver(text)
        }
    }

    private fun deliver(text: String?) {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == lastDelivered) return
        lastDelivered = trimmed
        ClipCapturePipeline.captureAsync(serviceScope, this, trimmed)
        postStatus(this, getString(R.string.notification_sent, com.clipsmile.app.ui.DogRoster.homeName(this)))
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

    private fun refreshCaption() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour == captionHour) return
        if (statusLine != null && System.currentTimeMillis() < statusUntil) return
        captionHour = hour
        statusLine = null
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification(this, rotatingLine(this)))
    }

    private fun buildNotification(): Notification {
        return notification(this, statusLine ?: rotatingLine(this))
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val POLL_INTERVAL_MS = 2_000L
        private const val PREFS_NAME = "clipsmile_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

        @Volatile
        private var statusLine: String? = null

        @Volatile
        private var statusUntil: Long = 0L

        @Volatile
        private var artCache: Bitmap? = null

        @Volatile
        private var artRes: Int = 0

        private fun notificationArt(context: Context, portrait: Int): Bitmap? {
            if (artRes == portrait) artCache?.let { return it }
            val decoded = BitmapFactory.decodeResource(context.resources, portrait) ?: return null
            val size = 192
            val scaled = if (decoded.width == size && decoded.height == size) {
                decoded
            } else {
                Bitmap.createScaledBitmap(decoded, size, size, true)
            }
            artCache = scaled
            artRes = portrait
            return scaled
        }

        private fun home(context: Context): Pair<com.clipsmile.app.ui.DogCompanion, String> {
            val dog = com.clipsmile.app.ui.DogRoster.selected(context)
            val name = com.clipsmile.app.ui.DogRoster.homeName(context)
            return dog to name
        }

        fun postStatus(context: Context, text: String) {
            statusLine = text
            statusUntil = System.currentTimeMillis() + 3 * 60 * 1000L
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification(context, text))
        }

        private fun rotatingLine(context: Context): String {
            val den = Den(context)
            val lines = context.resources.getStringArray(R.array.notification_lines)
            val name = home(context).second
            if (lines.isEmpty()) return context.getString(R.string.notification_text, name)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val salt = if (den.onboarded()) den.level(den.activeId()) else 1
            val line = lines[(hour + salt) % lines.size]
            return line.format(name)
        }

        private fun notification(context: Context, text: String): Notification {
            val (dog, name) = home(context)
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = androidx.core.app.TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(openAppIntent)
                .getPendingIntent(
                    0,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            val builder = NotificationCompat.Builder(context, ClipSmileApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title, name))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
            notificationArt(context, dog.portraitRes)?.let { builder.setLargeIcon(it) }
            return builder.build()
        }

        /** Title, portrait and line follow whichever dog is at home. */
        fun refresh(context: Context) {
            val manager = context.getSystemService(android.app.NotificationManager::class.java) ?: return
            val alive = manager.activeNotifications.any { it.id == NOTIFICATION_ID }
            if (!alive) return
            val text = if (statusLine != null && System.currentTimeMillis() < statusUntil) {
                statusLine!!
            } else {
                rotatingLine(context)
            }
            manager.notify(NOTIFICATION_ID, notification(context, text))
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
