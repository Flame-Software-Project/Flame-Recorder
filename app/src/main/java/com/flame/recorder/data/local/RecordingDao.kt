package com.flame.recorder.data.local

import androidx.room.*
import com.flame.recorder.data.model.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE isDeleted = 0 AND isTemporary = 0 ORDER BY timestamp DESC")
    fun getAllSavedRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isTemporary = 1 ORDER BY timestamp DESC")
    fun getTemporaryRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getRecycledRecordings(): Flow<List<Recording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)

    @Query("DELETE FROM recordings WHERE isDeleted = 1 AND :currentTime - deletedTimestamp > :maxAgeMs")
    suspend fun purgeOldRecycled(currentTime: Long, maxAgeMs: Long)
}