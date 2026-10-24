package com.clipsmile.app.service

import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.getSystemService

/**
 * Reads the clipboard while no activity of this app is in front.
 *
 * From Android 10 on, the system only returns clipboard text to the app that
 * currently has window focus, and it does not notify listeners that lack
 * focus. A 1×1 focusable overlay — allowed after the user grants "display
 * over other apps" — takes focus for a moment, reads the text, and is
 * removed. [WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM] keeps the
 * keyboard attached to the app underneath, so the Samsung keyboard can stay
 * selected.
 */
class ClipboardFocusReader(private val context: Context) {

    private val windowManager = context.getSystemService<WindowManager>()
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var finished = false

    val inFlight: Boolean
        get() = overlayView != null

    /**
     * @param onResult text that was readable, and whether the overlay actually
     * received window focus. A null text with focus means the clipboard was empty.
     */
    fun read(onResult: (text: String?, hadFocus: Boolean) -> Unit) {
        if (inFlight) return
        val wm = windowManager ?: run {
            onResult(null, false)
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { read(onResult) }
            return
        }

        val view = View(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        val params = WindowManager.LayoutParams(
            1,
            1,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        }

        finished = false
        var gotFocus = false
        val finish: (String?, Boolean) -> Unit = { text, hadFocus ->
            if (!finished) {
                finished = true
                removeOverlay()
                onResult(text, hadFocus)
            }
        }

        view.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus || finished) return@addOnWindowFocusChangeListener
            gotFocus = true
            view.post { finish(readClipboard(), true) }
        }

        overlayView = view
        try {
            wm.addView(view, params)
        } catch (_: Exception) {
            overlayView = null
            finished = true
            onResult(null, false)
            return
        }
        if (finished) return
        view.requestFocus()
        handler.postDelayed({
            if (!finished) finish(readClipboard(), gotFocus)
        }, FOCUS_TIMEOUT_MS)
    }

    fun cancel() {
        finished = true
        removeOverlay()
    }

    private fun removeOverlay() {
        handler.removeCallbacksAndMessages(null)
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
            // Already detached.
        }
    }

    private fun readClipboard(): String? {
        return try {
            val clipboard = context.getSystemService<ClipboardManager>() ?: return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    companion object {
        private const val FOCUS_TIMEOUT_MS = 280L
    }
}
