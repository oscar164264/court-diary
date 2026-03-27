package com.courtdiary.repository

import com.courtdiary.database.CaseDocumentDao
import com.courtdiary.model.CaseDocument
import kotlinx.coroutines.flow.Flow

class CaseDocumentRepository(private val dao: CaseDocumentDao) {

    fun getDocumentsForCase(caseId: Int): Flow<List<CaseDocument>> =
        dao.getDocumentsForCase(caseId)

    suspend fun insertDocument(doc: CaseDocument): Long = dao.insertDocument(doc)

    suspend fun deleteDocument(doc: CaseDocument) = dao.deleteDocument(doc)
}
