package com.clipsmile.app.util

import com.clipsmile.app.data.ClipEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClipFormatter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /**
     * Joins every saved entry, oldest first, leaving one blank line between
     * each item so the exported/shared text is clearly stacked "alt alta".
     */
    fun joinForExport(entries: List<ClipEntry>): String =
        entries.sortedBy { it.timestamp }
            .joinToString(separator = "\n\n") { it.content }
}
