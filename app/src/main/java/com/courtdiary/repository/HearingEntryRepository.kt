package com.courtdiary.repository

import com.courtdiary.database.HearingEntryDao
import com.courtdiary.model.HearingEntry
import kotlinx.coroutines.flow.Flow

class HearingEntryRepository(private val dao: HearingEntryDao) {
    fun getEntriesForCase(caseId: Int): Flow<List<HearingEntry>> = dao.getEntriesForCase(caseId)
    suspend fun insert(entry: HearingEntry): Long = dao.insert(entry)
    suspend fun update(entry: HearingEntry) = dao.update(entry)
    suspend fun delete(entry: HearingEntry) = dao.delete(entry)
    suspend fun getEntriesForCaseOnce(caseId: Int): List<HearingEntry> = dao.getEntriesForCaseOnce(caseId)
}
