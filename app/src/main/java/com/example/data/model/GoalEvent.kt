package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_events",
    indices = [Index(value = ["matchId"])]
)
data class GoalEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val period: Int, // 1 or 2
    val minute: Int, // minute in match
    val isMyTeam: Boolean,
    val teamName: String,
    val timestamp: Long = System.currentTimeMillis()
)
