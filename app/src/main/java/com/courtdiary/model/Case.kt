package com.courtdiary.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a court case.
 * caseNumber is unique across all records.
 */
@Entity(
    tableName = "cases",
    indices = [Index(value = ["caseNumber"], unique = true)]
)
data class CourtCase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Core required fields
    val caseNumber: String,          // Required & Unique
    val clientPhone: String,         // Required
    val nextHearingDate: Long,       // Required – stored as epoch millis

    // Optional fields
    val courtName: String = "",
    val clientName: String = "",     // Optional
    val lastHearingDate: Long? = null,
    val lastStage: String = "",
    val nextStage: String = "",
    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis()
)
