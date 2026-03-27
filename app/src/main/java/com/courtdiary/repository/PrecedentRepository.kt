package com.courtdiary.repository

import com.courtdiary.database.PrecedentDao
import com.courtdiary.model.Precedent
import kotlinx.coroutines.flow.Flow

class PrecedentRepository(private val dao: PrecedentDao) {

    fun getAllPrecedents(): Flow<List<Precedent>> = dao.getAllPrecedents()

    fun searchPrecedents(query: String): Flow<List<Precedent>> = dao.searchPrecedents(query)

    suspend fun getPrecedentById(id: Int): Precedent? = dao.getPrecedentById(id)

    suspend fun insertPrecedent(precedent: Precedent): Long = dao.insertPrecedent(precedent)

    suspend fun updatePrecedent(precedent: Precedent) = dao.updatePrecedent(precedent)

    suspend fun deletePrecedent(precedent: Precedent) = dao.deletePrecedent(precedent)
}
