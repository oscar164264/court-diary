package com.courtdiary.repository

import com.courtdiary.database.FeeEntryDao
import com.courtdiary.model.FeeEntry
import kotlinx.coroutines.flow.Flow

class FeeEntryRepository(private val dao: FeeEntryDao) {
    fun getEntriesForCase(caseId: Int): Flow<List<FeeEntry>> = dao.getEntriesForCase(caseId)
    fun getTotalCharged(caseId: Int): Flow<Double> = dao.getTotalCharged(caseId)
    fun getTotalPaid(caseId: Int): Flow<Double> = dao.getTotalPaid(caseId)
    suspend fun insert(entry: FeeEntry): Long = dao.insert(entry)
    suspend fun update(entry: FeeEntry) = dao.update(entry)
    suspend fun delete(entry: FeeEntry) = dao.delete(entry)
    suspend fun getEntriesForCaseOnce(caseId: Int): List<FeeEntry> = dao.getEntriesForCaseOnce(caseId)
}
