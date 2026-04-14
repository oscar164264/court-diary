package com.courtdiary.database

import androidx.room.*
import com.courtdiary.model.HearingEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HearingEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: HearingEntry): Long

    @Update
    suspend fun update(entry: HearingEntry)

    @Delete
    suspend fun delete(entry: HearingEntry)

    @Query("SELECT * FROM hearing_entries WHERE caseId = :caseId ORDER BY date DESC")
    fun getEntriesForCase(caseId: Int): Flow<List<HearingEntry>>

    @Query("SELECT * FROM hearing_entries WHERE caseId = :caseId ORDER BY date DESC")
    suspend fun getEntriesForCaseOnce(caseId: Int): List<HearingEntry>
}
