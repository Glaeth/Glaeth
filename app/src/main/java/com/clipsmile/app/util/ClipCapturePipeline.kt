package com.clipsmile.app.util

import android.content.Context
import com.clipsmile.app.data.ClipRepository
import com.clipsmile.app.data.RemoteSyncPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single path for "a new piece of text was just copied": persist it on the device.
 * Clipboard text is never uploaded.
 */
object ClipCapturePipeline {

    fun captureAsync(
        scope: CoroutineScope,
        context: Context,
        text: String,
        forwardDuplicates: Boolean = false
    ) {
        val appContext = context.applicationContext
        scope.launch { capture(appContext, text, forwardDuplicates) }
    }

    suspend fun capture(context: Context, text: String, forwardDuplicates: Boolean = false) {
        val appContext = context.applicationContext
        val saved = ClipRepository.getInstance(appContext).saveIfNew(text)
        if (!saved && !forwardDuplicates) return
        val sync = RemoteSyncPrefs(appContext)
        if (sync.isEnabled) sync.isEnabled = false
    }
}
