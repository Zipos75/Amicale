package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GoalEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goal_events WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getGoalsForMatchFlow(matchId: Long): Flow<List<GoalEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEvent): Long

    @Delete
    suspend fun deleteGoal(goal: GoalEvent)

    @Query("DELETE FROM goal_events WHERE matchId = :matchId")
    suspend fun deleteGoalsForMatch(matchId: Long)

    @Query("DELETE FROM goal_events WHERE matchId IN (SELECT id FROM matches WHERE isFromCalendar = 1)")
    suspend fun deleteGoalsForImportedMatches(): Int

    @Query("DELETE FROM goal_events WHERE id = (SELECT id FROM goal_events WHERE matchId = :matchId AND isMyTeam = :isMyTeam ORDER BY timestamp DESC LIMIT 1)")
    suspend fun removeLastGoalForTeam(matchId: Long, isMyTeam: Boolean): Int
}
