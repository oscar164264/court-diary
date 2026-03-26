package com.courtdiary.repository

import com.courtdiary.database.CaseDao
import com.courtdiary.model.Case
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all Case data.
 * The ViewModel talks only to this repository.
 */
class CaseRepository(private val dao: CaseDao) {

    fun getAllCases(): Flow<List<Case>> = dao.getAllCases()

    suspend fun getCaseById(id: Int): Case? = dao.getCaseById(id)

    /** Returns the generated row ID, or throws on duplicate case number. */
    suspend fun insertCase(case: Case): Long = dao.insertCase(case)

    suspend fun updateCase(case: Case) = dao.updateCase(case)

    suspend fun deleteCase(case: Case) = dao.deleteCase(case)

    fun getCasesByDate(startOfDay: Long, endOfDay: Long): Flow<List<Case>> =
        dao.getCasesByDate(startOfDay, endOfDay)

    suspend fun getCasesByDateOnce(startOfDay: Long, endOfDay: Long): List<Case> =
        dao.getCasesByDateOnce(startOfDay, endOfDay)

    fun getUpcomingCases(start: Long, end: Long): Flow<List<Case>> =
        dao.getUpcomingCases(start, end)

    fun getPastCases(today: Long): Flow<List<Case>> =
        dao.getPastCases(today)

    fun searchCases(query: String): Flow<List<Case>> =
        dao.searchCases(query)

    /** Returns null when the case number is available, or the conflicting Case when it exists. */
    suspend fun getCaseByCaseNumber(caseNumber: String): Case? =
        dao.getCaseByCaseNumber(caseNumber)

    fun getAllHearingDates(): Flow<List<Long>> = dao.getAllHearingDates()

    /** Full snapshot for backup. */
    suspend fun getAllCasesOnce(): List<Case> = dao.getAllCasesOnce()
}
