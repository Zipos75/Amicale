package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.model.GoalEvent
import com.example.data.model.MatchEntity
import com.example.data.model.OpponentColor
import kotlinx.coroutines.flow.Flow

class MatchRepository(private val db: AppDatabase) {

    private val matchDao = db.matchDao()
    private val goalDao = db.goalDao()
    private val colorDao = db.opponentColorDao()

    val allMatchesFlow: Flow<List<MatchEntity>> = matchDao.getAllMatchesFlow()

    fun getMatchFlow(id: Long): Flow<MatchEntity?> = matchDao.getMatchByIdFlow(id)

    suspend fun getMatch(id: Long): MatchEntity? = matchDao.getMatchById(id)

    fun getGoalsFlow(matchId: Long): Flow<List<GoalEvent>> = goalDao.getGoalsForMatchFlow(matchId)

    suspend fun insertMatch(match: MatchEntity): Long = matchDao.insert(match)

    suspend fun updateMatch(match: MatchEntity) = matchDao.update(match)

    suspend fun deleteManualMatch(id: Long) = matchDao.deleteManualMatch(id)

    suspend fun addGoal(
        matchId: Long,
        isMyTeam: Boolean,
        period: Int,
        minute: Int,
        teamName: String
    ) {
        val match = matchDao.getMatchById(matchId) ?: return
        val newHomeScore = if (match.isHome == isMyTeam) match.homeScore + 1 else match.homeScore
        val newAwayScore = if (match.isHome != isMyTeam) match.awayScore + 1 else match.awayScore

        goalDao.insertGoal(
            GoalEvent(
                matchId = matchId,
                period = period,
                minute = minute,
                isMyTeam = isMyTeam,
                teamName = teamName
            )
        )

        matchDao.update(
            match.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                isScoreEncoded = true
            )
        )
    }

    suspend fun removeLastGoal(matchId: Long, isMyTeam: Boolean) {
        val match = matchDao.getMatchById(matchId) ?: return
        val currentTeamScore = if (isMyTeam) {
            if (match.isHome) match.homeScore else match.awayScore
        } else {
            if (match.isHome) match.awayScore else match.homeScore
        }

        if (currentTeamScore <= 0) return

        goalDao.removeLastGoalForTeam(matchId, isMyTeam)

        val newHomeScore = if (match.isHome == isMyTeam) (match.homeScore - 1).coerceAtLeast(0) else match.homeScore
        val newAwayScore = if (match.isHome != isMyTeam) (match.awayScore - 1).coerceAtLeast(0) else match.awayScore

        matchDao.update(
            match.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore
            )
        )
    }

    suspend fun changePenaltyCorner(matchId: Long, isMyTeam: Boolean, delta: Int) {
        val match = matchDao.getMatchById(matchId) ?: return
        val newHomePc = if (match.isHome == isMyTeam) (match.homePenaltyCorners + delta).coerceAtLeast(0) else match.homePenaltyCorners
        val newAwayPc = if (match.isHome != isMyTeam) (match.awayPenaltyCorners + delta).coerceAtLeast(0) else match.awayPenaltyCorners
        matchDao.update(
            match.copy(
                homePenaltyCorners = newHomePc,
                awayPenaltyCorners = newAwayPc
            )
        )
    }

    suspend fun setOpponentColor(clubKey: String, colorHex: String) {
        colorDao.saveColor(OpponentColor(clubKey = clubKey.trim(), colorHex = colorHex))
    }

    fun getOpponentColorHex(clubKey: String): Flow<String?> {
        return colorDao.getColorHexFlow(clubKey.trim())
    }

    suspend fun getOpponentColorHexOnce(clubKey: String): String? {
        return colorDao.getColorForClub(clubKey.trim())?.colorHex
    }

    /**
     * Imports or updates matches.
     * CRITICAL RULE: Keeps already encoded scores and states intact!
     */
    suspend fun mergeImportedMatches(importedList: List<MatchEntity>): Int {
        var count = 0
        for (imported in importedList) {
            val existing = matchDao.getMatchByUid(imported.uid)
            if (existing != null) {
                // Keep existing scores, penalty corners, timer state and user overrides
                val updated = existing.copy(
                    timestamp = imported.timestamp,
                    homeTeam = imported.homeTeam,
                    awayTeam = imported.awayTeam,
                    isHome = imported.isHome,
                    myTeam = imported.myTeam,
                    opponentTeam = imported.opponentTeam,
                    location = imported.location ?: existing.location,
                    category = imported.category
                )
                matchDao.update(updated)
            } else {
                matchDao.insert(imported)
            }
            count++
        }
        return count
    }

    suspend fun deleteImportedMatches(): Int {
        goalDao.deleteGoalsForImportedMatches()
        return matchDao.deleteImportedMatches()
    }
}
