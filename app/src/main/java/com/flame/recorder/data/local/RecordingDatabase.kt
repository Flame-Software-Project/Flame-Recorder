package com.flame.recorder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.flame.recorder.data.model.Recording

@Database(entities = [Recording::class], version = 1, exportSchema = false)
abstract class RecordingDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: RecordingDatabase? = null

        fun getDatabase(context: Context): RecordingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecordingDatabase::class.java,
                    "recording_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}