// workedupshoury/jarvisv2.0/jarvisV2.0-aaa92dd1e8476ce67109495778760087eb2dcc1d/app/src/main/java/com/example/jarvisv2/data/AppDatabase.kt

package com.example.jarvisv2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// 1. BUMP VERSION to 2 and add MediaSearch entity
@Database(entities = [ChatMessage::class, MediaSearch::class], version = 2, exportSchema = false)
@TypeConverters(ChatTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun mediaSearchDao(): MediaSearchDao // Assumed: Added in previous step

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_database"
                )
                    // 2. ADD DESTRUCTIVE MIGRATION fallback for safe upgrade
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}