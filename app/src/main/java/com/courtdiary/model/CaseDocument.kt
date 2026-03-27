package com.courtdiary.model

import androidx.room.*

@Entity(
    tableName = "case_documents",
    foreignKeys = [ForeignKey(
        entity = CourtCase::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE   // auto-delete documents when the parent case is deleted
    )],
    indices = [Index("caseId")]
)
data class CaseDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val fileName: String,
    val filePath: String,       // absolute path inside app's internal storage
    val mimeType: String,
    val fileSize: Long,
    val uploadedAt: Long = System.currentTimeMillis()
)
