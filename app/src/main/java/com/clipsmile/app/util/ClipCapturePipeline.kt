package com.clipsmile.app.util

import android.content.Context
import com.clipsmile.app.data.ClipRepository
import com.clipsmile.app.data.RemoteSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single path for "a new piece of text was just copied": persist it once,
 * mirror it to the internet channel if that is turned on, and open the
 * pre-configured WhatsApp chat if automatic forwarding is turned on.
 */
object ClipCapturePipeline {

    fun captureAsync(scope: CoroutineScope, context: Context, text: String) {
        val appContext = context.applicationContext
        scope.launch { capture(appContext, text) }
    }

    suspend fun capture(context: Context, text: String) {
        val appContext = context.applicationContext
        val saved = ClipRepository.getInstance(appContext).saveIfNew(text)
        if (!saved) return
        RemoteSyncManager.notifyNewClip(appContext, text)
        withContext(Dispatchers.Main) {
            WhatsAppAutoSender.autoSendIfEnabled(appContext, text)
        }
    }
}
