package com.example.data.dao

import androidx.room.*
import com.example.data.model.DetoxSession
import kotlinx.coroutines.flow.Flow

@Dao
interface DetoxSessionDao {
    @Query("SELECT * FROM detox_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<DetoxSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DetoxSession): Long

    @Query("DELETE FROM detox_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM detox_sessions")
    suspend fun clearAll()
}
