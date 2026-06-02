package com.example.data.repository

import com.example.data.dao.DetoxSessionDao
import com.example.data.model.DetoxSession
import kotlinx.coroutines.flow.Flow

class DetoxRepository(private val detoxSessionDao: DetoxSessionDao) {
    val allSessions: Flow<List<DetoxSession>> = detoxSessionDao.getAllSessions()

    suspend fun insertSession(session: DetoxSession): Long {
        return detoxSessionDao.insertSession(session)
    }

    suspend fun deleteSessionById(id: Int) {
        detoxSessionDao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        detoxSessionDao.clearAll()
    }
}
