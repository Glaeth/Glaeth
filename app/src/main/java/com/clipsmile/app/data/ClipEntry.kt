package com.clipsmile.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single piece of text captured from the system clipboard.
 * Every new copy made anywhere on the device is stored as its own row,
 * which is how the app keeps the "stacked, one below another" history.
 */
@Entity(tableName = "clip_entries")
data class ClipEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long
)
