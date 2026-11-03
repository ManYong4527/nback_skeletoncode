package mobappdev.example.nback_cimpl.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [ScoringEngine].
 *
 * Covers CONTEXT.md §2.1 third requirement:
 *  - Correct calculation from hits / false alarms.
 *  - Boundary: score never dips below zero.
 */
class ScoringEngineTest {

    // ---------- straight calculation ----------

    @Test
    fun zeroHitsZeroFalseAlarms_scoresZero() {
        assertEquals(0, ScoringEngine.scoreOf(hits = 0, falseAlarms = 0))
    }

    @Test
    fun hitsOnly_areWorthTenEach() {
        assertEquals(50, ScoringEngine.scoreOf(hits = 5, falseAlarms = 0))
    }

    @Test
    fun falseAlarmsOnly_dockFivePerPenalty_butFloorsAtZero() {
        // 3 * 5 = 15 penalty, no hits → max(0, -15) = 0
        assertEquals(0, ScoringEngine.scoreOf(hits = 0, falseAlarms = 3))
    }

    @Test
    fun mixedHitsAndFalseAlarms_combineLinearly() {
        // 4 * 10 - 2 * 5 = 40 - 10 = 30
        assertEquals(30, ScoringEngine.scoreOf(hits = 4, falseAlarms = 2))
    }

    // ---------- the max(0, ...) clamp (SPEC "prevent negative results") ----------

    @Test
    fun falseAlarmsOutweighHits_scoreIsClampedToZero() {
        // 1 * 10 - 5 * 5 = 10 - 25 = -15 → 0
        assertEquals(0, ScoringEngine.scoreOf(hits = 1, falseAlarms = 5))
    }

    @Test
    fun exactlyBreakEven_scoresZero() {
        // 2 * 10 - 4 * 5 = 0
        assertEquals(0, ScoringEngine.scoreOf(hits = 2, falseAlarms = 4))
    }

    @Test
    fun oneMoreHitThanBreakEven_scoresTen() {
        // 3 * 10 - 4 * 5 = 10
        assertEquals(10, ScoringEngine.scoreOf(hits = 3, falseAlarms = 4))
    }

    // ---------- input validation ----------

    @Test(expected = IllegalArgumentException::class)
    fun negativeHits_areRejected() {
        ScoringEngine.scoreOf(hits = -1, falseAlarms = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeFalseAlarms_areRejected() {
        ScoringEngine.scoreOf(hits = 0, falseAlarms = -1)
    }
}
