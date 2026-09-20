package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.GoalDao
import com.example.data.dao.MatchDao
import com.example.data.dao.OpponentColorDao
import com.example.data.model.GoalEvent
import com.example.data.model.MatchEntity
import com.example.data.model.OpponentColor

@Database(
    entities = [
        MatchEntity::class,
        GoalEvent::class,
        OpponentColor::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun matchDao(): MatchDao
    abstract fun goalDao(): GoalDao
    abstract fun opponentColorDao(): OpponentColorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hockey_match_sheet.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
