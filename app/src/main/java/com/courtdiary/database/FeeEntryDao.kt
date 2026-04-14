package com.courtdiary.database

import androidx.room.*
import com.courtdiary.model.FeeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: FeeEntry): Long

    @Update
    suspend fun update(entry: FeeEntry)

    @Delete
    suspend fun delete(entry: FeeEntry)

    @Query("SELECT * FROM fee_entries WHERE caseId = :caseId ORDER BY date DESC")
    fun getEntriesForCase(caseId: Int): Flow<List<FeeEntry>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM fee_entries WHERE caseId = :caseId AND type = 'CHARGED'")
    fun getTotalCharged(caseId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM fee_entries WHERE caseId = :caseId AND type = 'PAID'")
    fun getTotalPaid(caseId: Int): Flow<Double>

    @Query("SELECT * FROM fee_entries WHERE caseId = :caseId ORDER BY date DESC")
    suspend fun getEntriesForCaseOnce(caseId: Int): List<FeeEntry>
}
