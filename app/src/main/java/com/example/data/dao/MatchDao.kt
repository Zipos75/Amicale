package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM matches ORDER BY timestamp ASC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    fun getMatchByIdFlow(id: Long): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE uid = :uid LIMIT 1")
    suspend fun getMatchByUid(uid: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<MatchEntity>): List<Long>

    @Update
    suspend fun update(match: MatchEntity)

    @Delete
    suspend fun delete(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id AND isFromCalendar = 0")
    suspend fun deleteManualMatch(id: Long): Int

    @Query("DELETE FROM matches WHERE isFromCalendar = 1")
    suspend fun deleteImportedMatches(): Int

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getMatchCount(): Int
}
