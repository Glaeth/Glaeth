package com.clipsmile.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Clicks WhatsApp's Send button only during a short window after this app
 * itself opened a chat with a copied message already filled in, then presses
 * Back. It does not read or store what was typed in other apps.
 */
class WhatsAppSendClickService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isArmed()) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return
        val button = findSendButton(root) ?: return
        if (!clickNode(button)) return

        disarm()
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, BACK_DELAY_MILLIS)
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (resourceId in SEND_BUTTON_IDS) {
            val matches = root.findAccessibilityNodeInfosByViewId(resourceId)
            if (!matches.isNullOrEmpty()) return matches[0]
        }
        return findByDescription(root)
    }

    private fun findByDescription(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val description = node.contentDescription?.toString().orEmpty()
        if (description.isNotBlank() &&
            SEND_DESCRIPTIONS.any { description.equals(it, ignoreCase = true) }
        ) {
            return node
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findByDescription(child)?.let { return it }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
            depth++
        }
        return false
    }

    companion object {
        private const val ARM_WINDOW_MILLIS = 15_000L
        private const val BACK_DELAY_MILLIS = 500L
        private val SEND_BUTTON_IDS = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )
        private val SEND_DESCRIPTIONS = listOf("Gönder", "Send")

        @Volatile
        private var armedUntilMillis: Long = 0L

        fun arm() {
            armedUntilMillis = System.currentTimeMillis() + ARM_WINDOW_MILLIS
        }

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, WhatsAppSendClickService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        private fun isArmed(): Boolean {
            if (System.currentTimeMillis() > armedUntilMillis) {
                armedUntilMillis = 0L
                return false
            }
            return true
        }

        private fun disarm() {
            armedUntilMillis = 0L
        }
    }
}
