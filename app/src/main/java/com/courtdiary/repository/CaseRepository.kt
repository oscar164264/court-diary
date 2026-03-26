package com.courtdiary.repository

import com.courtdiary.database.CaseDao
import com.courtdiary.model.CourtCase
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all CourtCase data.
 * The ViewModel talks only to this repository.
 */
class CaseRepository(private val dao: CaseDao) {

    fun getAllCases(): Flow<List<CourtCase>> = dao.getAllCases()

    suspend fun getCaseById(id: Int): CourtCase? = dao.getCaseById(id)

    /** Returns the generated row ID, or throws on duplicate case number. */
    suspend fun insertCase(case: CourtCase): Long = dao.insertCase(case)

    suspend fun updateCase(case: CourtCase) = dao.updateCase(case)

    suspend fun deleteCase(case: CourtCase) = dao.deleteCase(case)

    fun getCasesByDate(startOfDay: Long, endOfDay: Long): Flow<List<CourtCase>> =
        dao.getCasesByDate(startOfDay, endOfDay)

    suspend fun getCasesByDateOnce(startOfDay: Long, endOfDay: Long): List<CourtCase> =
        dao.getCasesByDateOnce(startOfDay, endOfDay)

    fun getUpcomingCases(start: Long, end: Long): Flow<List<CourtCase>> =
        dao.getUpcomingCases(start, end)

    fun getPastCases(today: Long): Flow<List<CourtCase>> =
        dao.getPastCases(today)

    fun searchCases(query: String): Flow<List<CourtCase>> =
        dao.searchCases(query)

    /** Returns null when the case number is available, or the conflicting CourtCase when it exists. */
    suspend fun getCaseByCaseNumber(caseNumber: String): CourtCase? =
        dao.getCaseByCaseNumber(caseNumber)

    fun getAllHearingDates(): Flow<List<Long>> = dao.getAllHearingDates()

    /** Full snapshot for backup. */
    suspend fun getAllCasesOnce(): List<CourtCase> = dao.getAllCasesOnce()
}
