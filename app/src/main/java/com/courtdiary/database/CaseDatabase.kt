package com.courtdiary.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.courtdiary.model.CaseDocument
import com.courtdiary.model.CourtCase
import com.courtdiary.model.FeeEntry
import com.courtdiary.model.HearingEntry
import com.courtdiary.model.Precedent

@Database(
    entities = [CourtCase::class, CaseDocument::class, Precedent::class,
                HearingEntry::class, FeeEntry::class],
    version = 4,
    exportSchema = false
)
abstract class CaseDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao
    abstract fun caseDocumentDao(): CaseDocumentDao
    abstract fun precedentDao(): PrecedentDao
    abstract fun hearingEntryDao(): HearingEntryDao
    abstract fun feeEntryDao(): FeeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: CaseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS case_documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        caseId INTEGER NOT NULL,
                        fileName TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        uploadedAt INTEGER NOT NULL,
                        FOREIGN KEY(caseId) REFERENCES cases(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_case_documents_caseId ON case_documents(caseId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS precedents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        textContent TEXT NOT NULL DEFAULT '',
                        filePath TEXT NOT NULL DEFAULT '',
                        fileName TEXT NOT NULL DEFAULT '',
                        mimeType TEXT NOT NULL DEFAULT '',
                        fileSize INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to cases table
                database.execSQL("ALTER TABLE cases ADD COLUMN status TEXT NOT NULL DEFAULT 'Active'")
                database.execSQL("ALTER TABLE cases ADD COLUMN opposingParty TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE cases ADD COLUMN opposingCounsel TEXT NOT NULL DEFAULT ''")

                // Create hearing_entries table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hearing_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        caseId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        stage TEXT NOT NULL DEFAULT '',
                        outcome TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(caseId) REFERENCES cases(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_hearing_entries_caseId ON hearing_entries(caseId)"
                )

                // Create fee_entries table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fee_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        caseId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        FOREIGN KEY(caseId) REFERENCES cases(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fee_entries_caseId ON fee_entries(caseId)"
                )
            }
        }

        fun getDatabase(context: Context): CaseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaseDatabase::class.java,
                    "court_diary_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
