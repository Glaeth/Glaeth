package com.clipsmile.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.clipsmile.app.R

/**
 * Opens the user's email app with every saved clip already written into a
 * new message. Nothing is sent until they tap Send in that app.
 */
object EmailExportHelper {

    private const val ADDRESS = "guray.vatanuk@gmail.com"
    private const val MAX_BODY_LENGTH = 80_000

    fun export(context: Context, text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, R.string.toast_nothing_to_send, Toast.LENGTH_SHORT).show()
            return
        }

        val body = if (text.length <= MAX_BODY_LENGTH) {
            text
        } else {
            text.take(MAX_BODY_LENGTH) + "\n\n… (kısaltıldı)"
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$ADDRESS")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_export_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.toast_no_email_app, Toast.LENGTH_SHORT).show()
        }
    }
}
