package com.clipsmile.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    @Insert
    suspend fun insert(entry: ClipEntry): Long

    @Query("SELECT * FROM clip_entries ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ClipEntry>>

    @Query("SELECT content FROM clip_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastContent(): String?

    @Delete
    suspend fun delete(entry: ClipEntry)

    @Query("DELETE FROM clip_entries")
    suspend fun deleteAll()
}
