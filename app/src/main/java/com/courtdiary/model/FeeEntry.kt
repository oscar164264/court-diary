package com.courtdiary.model

import androidx.room.*

@Entity(
    tableName = "fee_entries",
    foreignKeys = [ForeignKey(
        entity = CourtCase::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("caseId")]
)
data class FeeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val type: String,           // "CHARGED" or "PAID"
    val amount: Double,
    val description: String = "",
    val date: Long = System.currentTimeMillis()
)
