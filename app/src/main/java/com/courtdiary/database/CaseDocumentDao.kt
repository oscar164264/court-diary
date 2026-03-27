package com.courtdiary.database

import androidx.room.*
import com.courtdiary.model.CaseDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDocumentDao {

    @Query("SELECT * FROM case_documents WHERE caseId = :caseId ORDER BY uploadedAt DESC")
    fun getDocumentsForCase(caseId: Int): Flow<List<CaseDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CaseDocument): Long

    @Delete
    suspend fun deleteDocument(doc: CaseDocument)
}
