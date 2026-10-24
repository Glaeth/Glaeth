package com.clipsmile.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.clipsmile.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * Helper responsible for pushing saved clipboard text to WhatsApp.
 *
 * WhatsApp (and the system "send" sheet) imposes a practical limit on how much
 * text can travel as a plain [Intent.EXTRA_TEXT] payload, so once the combined
 * export grows past [MAX_PLAIN_TEXT_LENGTH] we switch to attaching a small
 * `.txt` file instead, shared through a [FileProvider] so it works without any
 * extra storage permissions.
 */
object WhatsAppShareHelper {

    private const val MAX_PLAIN_TEXT_LENGTH = 60_000
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    fun shareText(context: Context, text: String, preferredPackage: String? = WHATSAPP_PACKAGE) {
        if (text.isBlank()) {
            Toast.makeText(context, R.string.toast_nothing_to_send, Toast.LENGTH_SHORT).show()
            return
        }

        if (text.length > MAX_PLAIN_TEXT_LENGTH) {
            shareAsFile(context, text)
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        sendWithWhatsAppFallback(context, intent, preferredPackage)
    }

    private fun shareAsFile(context: Context, text: String) {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "kopyalar_${System.currentTimeMillis()}.txt")
        FileOutputStream(file).use { it.write(text.toByteArray(Charsets.UTF_8)) }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        sendWithWhatsAppFallback(context, intent, WHATSAPP_PACKAGE)
    }

    private fun sendWithWhatsAppFallback(context: Context, intent: Intent, preferredPackage: String?) {
        val candidates = listOfNotNull(preferredPackage, WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE).distinct()

        for (pkg in candidates) {
            try {
                intent.setPackage(pkg)
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Try the next candidate package.
            }
        }

        // No WhatsApp variant installed: fall back to the generic share sheet
        // so the user can still pick another app, and let them know why.
        Toast.makeText(context, R.string.toast_whatsapp_not_found, Toast.LENGTH_SHORT).show()
        intent.setPackage(null)
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.whatsapp_share_chooser_title)))
        } catch (e: ActivityNotFoundException) {
            // No app at all can handle a share intent; nothing more we can do.
        }
    }
}
