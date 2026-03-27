package com.courtdiary.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "precedents")
data class Precedent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val textContent: String = "",   // pasted/typed text
    val filePath: String = "",      // path if a file was uploaded (empty = text-only)
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
