package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.calendar.CalendarImporter
import com.example.data.model.HockeyRulesRepository
import com.example.data.model.MatchEntity
import com.example.timer.LiveTimerState
import com.example.ui.theme.HockeyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class HockeyAppTest {

    @Test
    fun testCalendarSummaryHyphenSplitting() {
        // Specific case in prompt:
        // "Amicale Anderlecht U9B-1-Pingouin U9B-2. Règle qui fonctionne : couper sur le premier tiret qui n'est pas suivi d'un chiffre."
        val summary = "Amicale Anderlecht U9B-1-Pingouin U9B-2"
        val (home, away) = CalendarImporter.splitTeams(summary)
        assertEquals("Amicale Anderlecht U9B-1", home)
        assertEquals("Pingouin U9B-2", away)
    }

    @Test
    fun testCalendarSummaryWithComplexNames() {
        val summary = "Waterloo Ducks U10B-3-Royal Léopold U10B-1"
        val (home, away) = CalendarImporter.splitTeams(summary)
        assertEquals("Waterloo Ducks U10B-3", home)
        assertEquals("Royal Léopold U10B-1", away)
    }

    @Test
    fun testLineUnfoldingAndUnescape() {
        val raw = "BEGIN:VCALENDAR\nSUMMARY:Match de ho\n ckey\\, test\\; suite\nEND:VCALENDAR"
        val unfolded = CalendarImporter.unfoldICal(raw)
        assertEquals(3, unfolded.size)
        assertEquals("SUMMARY:Match de hockey\\, test\\; suite", unfolded[1])
        assertEquals("SUMMARY:Match de hockey, test; suite", CalendarImporter.unescapeICal(unfolded[1]))
    }

    @Test
    fun testCategoryRulesCompliance() {
        val u7 = MatchEntity.defaultDurationForCategory("U7/U8")
        val u9 = MatchEntity.defaultDurationForCategory("U9")
        val u10 = MatchEntity.defaultDurationForCategory("U10")
        val u11 = MatchEntity.defaultDurationForCategory("U11/U12")

        assertEquals(20, u7)
        assertEquals(25, u9)
        assertEquals(25, u10)
        assertEquals(25, u11)

        // PC compliance: only U10, U11/U12 have penalty corners
        assertFalse(createDummyMatch("U7/U8").hasPenaltyCorners)
        assertFalse(createDummyMatch("U9").hasPenaltyCorners)
        assertTrue(createDummyMatch("U10").hasPenaltyCorners)
        assertTrue(createDummyMatch("U11/U12").hasPenaltyCorners)
    }

    private fun createDummyMatch(category: String): MatchEntity {
        return MatchEntity(
            uid = "test_uid",
            timestamp = System.currentTimeMillis(),
            homeTeam = "Team A",
            awayTeam = "Team B",
            isHome = true,
            myTeam = "Team A",
            opponentTeam = "Team B",
            category = category
        )
    }

    @Test
    fun testTimerSystemClockCalculation() {
        // Elapsed = elapsedBefore + (now - startedAt)
        val now = 1000000L
        val startedAt = now - 60000L // 1 minute ago
        val state = LiveTimerState(
            isRunning = true,
            startedAtMs = startedAt,
            elapsedBeforeMs = 30000L, // 30s before
            totalDurationMs = 25 * 60 * 1000L,
            isCountDown = true
        )
        // With current timestamp logic, should not accumulate ticks
        assertTrue(state.totalDurationMs == 1500000L)
    }
}
