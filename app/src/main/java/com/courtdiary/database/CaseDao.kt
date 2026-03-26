package com.courtdiary.database

import androidx.room.*
import com.courtdiary.model.Case
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Case entity.
 * Provides all database operations needed by the app.
 */
@Dao
interface CaseDao {

    // ──────────────────────────────────────────
    // Write operations
    // ──────────────────────────────────────────

    /** Insert a new case. Throws if caseNumber already exists (ABORT). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCase(case: Case): Long

    /** Update an existing case. */
    @Update
    suspend fun updateCase(case: Case)

    /** Delete a case. */
    @Delete
    suspend fun deleteCase(case: Case)

    // ──────────────────────────────────────────
    // Read operations (reactive Flows)
    // ──────────────────────────────────────────

    /** Stream all cases ordered by next hearing date ascending. */
    @Query("SELECT * FROM cases ORDER BY nextHearingDate ASC")
    fun getAllCases(): Flow<List<Case>>

    /**
     * Stream cases whose next hearing date falls within [startOfDay, endOfDay].
     * Used to show cases for a specific calendar day.
     */
    @Query(
        """SELECT * FROM cases
           WHERE nextHearingDate >= :startOfDay AND nextHearingDate <= :endOfDay
           ORDER BY nextHearingDate ASC"""
    )
    fun getCasesByDate(startOfDay: Long, endOfDay: Long): Flow<List<Case>>

    /**
     * One-shot read of cases for a date range (used by WorkManager notification worker).
     */
    @Query(
        """SELECT * FROM cases
           WHERE nextHearingDate >= :startOfDay AND nextHearingDate <= :endOfDay"""
    )
    suspend fun getCasesByDateOnce(startOfDay: Long, endOfDay: Long): List<Case>

    /** Stream cases in the future: from [start] to [end] (upcoming). */
    @Query(
        """SELECT * FROM cases
           WHERE nextHearingDate >= :start AND nextHearingDate <= :end
           ORDER BY nextHearingDate ASC"""
    )
    fun getUpcomingCases(start: Long, end: Long): Flow<List<Case>>

    /** Stream cases with nextHearingDate strictly before [today]. */
    @Query(
        """SELECT * FROM cases
           WHERE nextHearingDate < :today
           ORDER BY nextHearingDate DESC"""
    )
    fun getPastCases(today: Long): Flow<List<Case>>

    /** Search cases by case number, client name, or phone number. */
    @Query(
        """SELECT * FROM cases
           WHERE caseNumber LIKE '%' || :query || '%'
              OR clientName LIKE '%' || :query || '%'
              OR clientPhone LIKE '%' || :query || '%'
           ORDER BY nextHearingDate ASC"""
    )
    fun searchCases(query: String): Flow<List<Case>>

    /** Check whether a case with the given number exists (returns null if not). */
    @Query("SELECT * FROM cases WHERE caseNumber = :caseNumber LIMIT 1")
    suspend fun getCaseByCaseNumber(caseNumber: String): Case?

    /** Load a single case by its row id. */
    @Query("SELECT * FROM cases WHERE id = :id LIMIT 1")
    suspend fun getCaseById(id: Int): Case?

    /** Stream all distinct next-hearing epoch-millis values – used to highlight calendar dates. */
    @Query("SELECT DISTINCT nextHearingDate FROM cases")
    fun getAllHearingDates(): Flow<List<Long>>

    /** One-shot snapshot of every case (used for backup export). */
    @Query("SELECT * FROM cases")
    suspend fun getAllCasesOnce(): List<Case>
}
