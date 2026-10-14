package com.clipsmile.app.util

import android.content.Context
import com.clipsmile.app.data.ClipRepository
import com.clipsmile.app.data.RemoteSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single path for "a new piece of text was just copied": persist it once,
 * mirror it to the internet channel if that is turned on.
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
        if (saved) {
            RemoteSyncManager.notifyNewClip(appContext, text)
        }
    }
}
