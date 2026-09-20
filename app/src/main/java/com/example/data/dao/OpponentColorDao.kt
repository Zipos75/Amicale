package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.OpponentColor
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentColorDao {

    @Query("SELECT * FROM opponent_colors")
    fun getAllColorsFlow(): Flow<List<OpponentColor>>

    @Query("SELECT * FROM opponent_colors WHERE LOWER(TRIM(clubKey)) = LOWER(TRIM(:clubKey)) LIMIT 1")
    suspend fun getColorForClub(clubKey: String): OpponentColor?

    @Query("SELECT colorHex FROM opponent_colors WHERE LOWER(TRIM(clubKey)) = LOWER(TRIM(:clubKey)) LIMIT 1")
    fun getColorHexFlow(clubKey: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveColor(opponentColor: OpponentColor)
}
