package com.clipsmile.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional, experimental Accessibility Service used only when the user turns
 * on "Tam Otomatik" (fully automatic) WhatsApp forwarding in Settings.
 *
 * WhatsApp does not offer any official, silent way for a third-party app to
 * actually send a message - the user always has to tap the Send button
 * themselves, which is an anti-spam measure built into WhatsApp itself, not
 * just a policy choice. This service simulates that one tap on the user's
 * behalf: once [com.clipsmile.app.util.WhatsAppAutoSender] opens WhatsApp's
 * chat screen with the text already filled in, this service looks for the
 * Send button inside WhatsApp's window and clicks it - but only within a
 * short time window after [armPendingSend] was called, so it never
 * interferes with messages the user is typing and sending by hand.
 *
 * This relies on WhatsApp's internal view structure, which is not a public
 * API and can change between WhatsApp app updates, so it may stop working
 * and need adjustment; the normal "one tap" auto-fill flow keeps working
 * regardless of this service's state.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isArmed()) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return
        findSendButton(root)?.let { button ->
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            disarm()
        }
    }

    override fun onInterrupt() {
        // Nothing to clean up.
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (resourceId in SEND_BUTTON_RESOURCE_IDS) {
            val matches = node.findAccessibilityNodeInfosByViewId(resourceId)
            if (matches.isNotEmpty()) return matches[0]
        }
        for (description in SEND_BUTTON_DESCRIPTIONS) {
            val matches = node.findAccessibilityNodeInfosByText(description)
            matches.firstOrNull { it.isClickable }?.let { return it }
        }
        return null
    }

    private fun isArmed(): Boolean {
        val armedAt = pendingSendArmedAtMillis
        if (armedAt == 0L) return false
        if (System.currentTimeMillis() - armedAt > ARM_WINDOW_MILLIS) {
            disarm()
            return false
        }
        return true
    }

    private fun disarm() {
        pendingSendArmedAtMillis = 0L
    }

    companion object {
        private const val ARM_WINDOW_MILLIS = 15_000L
        private val SEND_BUTTON_RESOURCE_IDS = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )
        private val SEND_BUTTON_DESCRIPTIONS = listOf("Gönder", "Send")

        @Volatile
        private var pendingSendArmedAtMillis: Long = 0L

        /** Tells this service to click the next Send button it sees within [ARM_WINDOW_MILLIS]. */
        fun armPendingSend() {
            pendingSendArmedAtMillis = System.currentTimeMillis()
        }

        /** Whether the user has granted this app's accessibility service in system settings. */
        fun isAccessibilityServiceEnabledSetting(context: Context): Boolean {
            val expected = ComponentName(context, WhatsAppAutoSendService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
