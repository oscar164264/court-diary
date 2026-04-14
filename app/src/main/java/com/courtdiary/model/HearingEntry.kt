package com.courtdiary.model

import androidx.room.*

@Entity(
    tableName = "hearing_entries",
    foreignKeys = [ForeignKey(
        entity = CourtCase::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("caseId")]
)
data class HearingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val date: Long,
    val stage: String = "",
    val outcome: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
