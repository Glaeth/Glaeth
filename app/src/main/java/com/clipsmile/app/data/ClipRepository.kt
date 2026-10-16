package com.clipsmile.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single access point for reading/writing captured clipboard entries.
 * Shared by the UI (MainActivity) and the background [com.clipsmile.app.service.ClipboardWatcherService].
 */
class ClipRepository private constructor(context: Context) {

    private val dao = ClipDatabase.getInstance(context).clipDao()

    // The activity's foreground listener and the background service's listener
    // can both react to the same copy at nearly the same time; this mutex makes
    // the "check last entry, then insert" sequence atomic so we never store a
    // duplicate row for a single copy action.
    private val writeMutex = Mutex()

    fun observeEntries(): Flow<List<ClipEntry>> = dao.observeAll()

    /**
     * Saves [text] as a new entry unless it is blank or an exact repeat of the
     * most recently saved entry.
     */
    suspend fun saveIfNew(text: String?): Boolean {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return false

        return writeMutex.withLock {
            val lastSaved = dao.getLastContent()
            if (lastSaved == trimmed) {
                false
            } else {
                dao.insert(ClipEntry(content = trimmed, timestamp = System.currentTimeMillis()))
                true
            }
        }
    }

    suspend fun delete(entry: ClipEntry) = dao.delete(entry)

    /** Re-inserts a previously deleted entry, used to support "undo delete". */
    suspend fun restore(entry: ClipEntry) {
        dao.insert(entry.copy(id = 0))
    }

    suspend fun clearAll() = dao.deleteAll()

    companion object {
        @Volatile
        private var instance: ClipRepository? = null

        fun getInstance(context: Context): ClipRepository =
            instance ?: synchronized(this) {
                instance ?: ClipRepository(context.applicationContext).also { instance = it }
            }
    }
}
