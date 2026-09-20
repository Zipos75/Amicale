package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    indices = [Index(value = ["uid"], unique = true)]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val isFromCalendar: Boolean = false,
    val timestamp: Long,
    val homeTeam: String,
    val awayTeam: String,
    val isHome: Boolean = true,
    val myTeam: String,
    val opponentTeam: String,
    val category: String = "U10",
    val location: String? = null,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val isScoreEncoded: Boolean = false,
    val isFinished: Boolean = false,
    val currentPeriod: Int = 1,
    val periodDurationMinutes: Int = 25,
    val homePenaltyCorners: Int = 0,
    val awayPenaltyCorners: Int = 0,
    val startedAtMs: Long? = null,
    val elapsedBeforeMs: Long = 0L,
    val isTimerRunning: Boolean = false,
    val isCountDown: Boolean = true
) {
    /**
     * Category rules helper:
     * - Penalty Corners exist ONLY in U10 and U11/U12.
     * - U7/U8 and U9 do NOT have penalty corners.
     */
    val hasPenaltyCorners: Boolean
        get() = category == "U10" || category == "U11/U12"

    /**
     * Duration for default category
     */
    companion object {
        fun defaultDurationForCategory(cat: String): Int {
            return when (cat) {
                "U7/U8" -> 20
                else -> 25 // U9, U10, U11/U12 are 2x25 min
            }
        }
    }

    /**
     * Result for myTeam: "WIN", "DRAW", "LOSS"
     */
    val matchResult: String
        get() {
            val myScore = if (isHome) homeScore else awayScore
            val oppScore = if (isHome) awayScore else homeScore
            return when {
                myScore > oppScore -> "WIN"
                myScore == oppScore -> "DRAW"
                else -> "LOSS"
            }
        }
}
