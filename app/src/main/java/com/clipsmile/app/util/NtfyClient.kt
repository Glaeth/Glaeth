package com.clipsmile.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for publishing plain-text messages to a public
 * [ntfy.sh](https://ntfy.sh) topic over HTTP, so a copied clip can be picked
 * up from any other device that is subscribed to the same topic (via the
 * ntfy app, or simply by opening `https://ntfy.sh/<channel>` in a browser).
 *
 * No API key or account is required; the topic name itself is the only
 * "secret" protecting the data, since the public instance has no auth.
 */
object NtfyClient {

    const val BASE_URL = "https://ntfy.sh/"

    // ntfy.sh's default per-message size limit is 4096 bytes; stay comfortably under it.
    private const val MAX_MESSAGE_BYTES = 3800
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    suspend fun publish(
        channel: String,
        message: String,
        title: String? = null,
        tags: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (channel.isBlank() || message.isBlank()) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val url = URL(BASE_URL + channel)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                // Title/tags travel as HTTP headers, which are not safe for
                // non-ASCII text, so callers must only pass ASCII-only values.
                title?.let { setRequestProperty("X-Title", it) }
                tags?.let { setRequestProperty("X-Tags", it) }
            }

            connection.outputStream.use { it.write(truncate(message).toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            code in 200..299
        } catch (e: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun truncate(text: String): String {
        if (text.toByteArray(Charsets.UTF_8).size <= MAX_MESSAGE_BYTES) return text

        var candidate = text
        while (candidate.isNotEmpty() && candidate.toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES) {
            candidate = candidate.dropLast(50)
        }
        return "$candidate\n\n… (kısaltıldı)"
    }
}
