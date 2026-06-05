package com.flame.recorder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long,
    val isTemporary: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0,
    val summary: String? = null
)