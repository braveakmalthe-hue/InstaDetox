package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detox_sessions")
data class DetoxSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionType: String,
    val durationSeconds: Long,
    val reflectionText: String,
    val moodRating: String,
    val timestamp: Long = System.currentTimeMillis()
)
