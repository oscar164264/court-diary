package com.courtdiary.database

import androidx.room.*
import com.courtdiary.model.Precedent
import kotlinx.coroutines.flow.Flow

@Dao
interface PrecedentDao {

    @Query("SELECT * FROM precedents ORDER BY updatedAt DESC")
    fun getAllPrecedents(): Flow<List<Precedent>>

    /** Searches title, pasted text content, and uploaded file name. */
    @Query("""
        SELECT * FROM precedents
        WHERE title LIKE '%' || :query || '%'
           OR textContent LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchPrecedents(query: String): Flow<List<Precedent>>

    @Query("SELECT * FROM precedents WHERE id = :id")
    suspend fun getPrecedentById(id: Int): Precedent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrecedent(precedent: Precedent): Long

    @Update
    suspend fun updatePrecedent(precedent: Precedent)

    @Delete
    suspend fun deletePrecedent(precedent: Precedent)
}
