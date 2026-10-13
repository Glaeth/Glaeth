package com.clipsmile.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clipsmile.app.data.AutoWhatsAppPrefs
import com.clipsmile.app.service.WhatsAppAutoSendService

/**
 * Launches a direct WhatsApp conversation with a pre-configured contact and
 * the freshly copied text already filled into the message box, so the
 * person on the other end doesn't have to open WhatsApp, find the chat, and
 * paste the text themselves - they only need to tap Send once.
 *
 * When [AutoWhatsAppPrefs.isFullyAutomatic] is turned on, it also arms
 * [WhatsAppAutoSendService] so that even that last tap happens automatically.
 */
object WhatsAppAutoSender {

    private const val MAX_MESSAGE_LENGTH = 3500
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    fun autoSendIfEnabled(context: Context, text: String) {
        val prefs = AutoWhatsAppPrefs(context)
        if (!prefs.isEnabled) return
        send(context, prefs.phoneNumber, text, prefs.isFullyAutomatic)
    }

    /** Sends immediately regardless of the "enabled" toggle; used by the Settings test button. */
    fun sendNow(context: Context, phoneNumber: String, text: String, fullyAutomatic: Boolean) {
        send(context, phoneNumber, text, fullyAutomatic)
    }

    fun normalizePhone(rawPhone: String): String = rawPhone.filter { it.isDigit() }

    fun isValidPhone(rawPhone: String): Boolean = normalizePhone(rawPhone).length in 10..15

    private fun send(context: Context, rawPhone: String, text: String, fullyAutomatic: Boolean) {
        val phone = normalizePhone(rawPhone)
        if (phone.isBlank() || text.isBlank()) return

        if (fullyAutomatic) {
            WhatsAppAutoSendService.armPendingSend()
        }

        val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(truncate(text))}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        for (pkg in listOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)) {
            try {
                intent.setPackage(pkg)
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Try the next candidate package.
            }
        }

        try {
            intent.setPackage(null)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp isn't installed; nothing more we can do automatically.
        }
    }

    private fun truncate(text: String): String =
        if (text.length <= MAX_MESSAGE_LENGTH) text else text.take(MAX_MESSAGE_LENGTH) + "… (kısaltıldı)"
}
