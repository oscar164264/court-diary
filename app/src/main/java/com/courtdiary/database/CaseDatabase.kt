package com.courtdiary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.courtdiary.model.CourtCase

/**
 * Room database singleton.
 * Version 1 – initial schema.
 */
@Database(
    entities = [CourtCase::class],
    version = 1,
    exportSchema = false
)
abstract class CaseDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao

    companion object {
        @Volatile
        private var INSTANCE: CaseDatabase? = null

        fun getDatabase(context: Context): CaseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaseDatabase::class.java,
                    "court_diary_db"
                )
                    .fallbackToDestructiveMigration() // safe for v1 – add proper migrations later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
