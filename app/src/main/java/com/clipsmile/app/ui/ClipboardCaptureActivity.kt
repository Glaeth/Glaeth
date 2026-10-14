package com.clipsmile.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.clipsmile.app.util.ClipCapturePipeline
import kotlinx.coroutines.launch

/**
 * Invisible activity used only to read the clipboard while this app is in
 * the background.
 *
 * From Android 10 on, the system only reveals clipboard contents to the app
 * that currently has focus. The watcher service still hears that *something*
 * was copied, but it cannot read the text itself. It brings this activity
 * up for a moment (allowed once the user grants "display over other apps"),
 * this activity reads the text as soon as it has window focus, hands it to
 * [ClipCapturePipeline], and closes immediately so the user lands back where
 * they were — or in WhatsApp, if automatic forwarding is on.
 */
class ClipboardCaptureActivity : AppCompatActivity() {

    private var handled = false
    private var startedAtMillis = 0L
    private val timeoutHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startedAtMillis = System.currentTimeMillis()
        // A window with no content never becomes the focused app, so Android
        // keeps hiding the clipboard. A transparent full-screen view is enough
        // to take focus, read the copy, then close.
        setContentView(View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        })
        overridePendingTransition(0, 0)
        timeoutHandler.postDelayed({
            if (!handled) finishQuietly()
        }, FOCUS_TIMEOUT_MILLIS)
    }

    override fun onResume() {
        super.onResume()
        timeoutHandler.post { handleClipboard() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handled = false
        startedAtMillis = System.currentTimeMillis()
        timeoutHandler.post { handleClipboard() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            handleClipboard()
        }
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun handleClipboard() {
        if (handled || isFinishing) return

        val text = intent.getStringExtra(EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: readClipboardText()
        if (text.isNullOrBlank()) {
            val stillWaiting = System.currentTimeMillis() - startedAtMillis < FOCUS_TIMEOUT_MILLIS
            if (stillWaiting) {
                timeoutHandler.postDelayed({ handleClipboard() }, RETRY_MILLIS)
            } else {
                handled = true
                finishQuietly()
            }
            return
        }
        handled = true

        // Stay open until the text is saved and WhatsApp (if enabled) has
        // been launched. This window is the foreground app for that instant,
        // which is what lets the launch succeed on Android 10+.
        lifecycleScope.launch {
            try {
                ClipCapturePipeline.capture(
                    this@ClipboardCaptureActivity,
                    text,
                    forwardDuplicates = true
                )
            } finally {
                finishQuietly()
            }
        }
    }

    private fun readClipboardText(): String? {
        return try {
            val clipboard = getSystemService<ClipboardManager>() ?: return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(this)?.toString()
        } catch (e: SecurityException) {
            null
        }
    }

    private fun finishQuietly() {
        if (isFinishing) return
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val FOCUS_TIMEOUT_MILLIS = 2000L
        private const val RETRY_MILLIS = 120L
        private const val RELAUNCH_DEBOUNCE_MILLIS = 700L

        @Volatile
        private var lastLaunchAtMillis: Long = 0L

        const val EXTRA_TEXT = "clip_text"

        fun launch(context: Context, knownText: String? = null) {
            val now = System.currentTimeMillis()
            if (now - lastLaunchAtMillis < RELAUNCH_DEBOUNCE_MILLIS) return
            lastLaunchAtMillis = now

            val intent = Intent(context, ClipboardCaptureActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                if (!knownText.isNullOrBlank()) putExtra(EXTRA_TEXT, knownText)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                lastLaunchAtMillis = 0L
            }
        }
    }
}
