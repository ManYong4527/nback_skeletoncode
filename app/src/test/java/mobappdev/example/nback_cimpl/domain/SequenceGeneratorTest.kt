package mobappdev.example.nback_cimpl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pure-JVM tests for [SequenceGenerator]. No Android dependencies.
 *
 * Covers the three CONTEXT.md §2.1 requirements:
 *  1. Sequence length matches the request.
 *  2. First `nBack` indices contain zero matches.
 *  3. Match rate for each modality falls in `[0.20, 0.40]`.
 *
 * Plus determinism (seeded [Random]) and input validation.
 */
class SequenceGeneratorTest {

    private val defaultLetters = listOf('C', 'D', 'G', 'K', 'P', 'T', 'V')

    /** Indices `i` where `events[i].visualIndex == events[i - nBack].visualIndex`. */
    private fun visualMatchIndices(events: List<StimulusEvent>, nBack: Int): List<Int> =
        (nBack until events.size).filter { i ->
            events[i].visualIndex == events[i - nBack].visualIndex
        }

    /** Indices `i` where `events[i].letter == events[i - nBack].letter`. */
    private fun auditoryMatchIndices(events: List<StimulusEvent>, nBack: Int): List<Int> =
        (nBack until events.size).filter { i ->
            events[i].letter == events[i - nBack].letter
        }

    // ---------- length ----------

    @Test
    fun generate_returnsExactlyTotalEvents() {
        val events = SequenceGenerator(Random(1)).generate(
            totalEvents = 20, nBack = 2, gridSize = 3, letters = defaultLetters,
        )
        assertEquals(20, events.size)
    }

    // ---------- first-N contains no matches ----------

    @Test
    fun firstNEvents_haveNoVisualOrAuditoryMatch() {
        val nBack = 3
        val events = SequenceGenerator(Random(42)).generate(
            totalEvents = 25, nBack = nBack, gridSize = 3, letters = defaultLetters,
        )

        val visualInWarmup = visualMatchIndices(events, nBack).filter { it < nBack }
        val auditoryInWarmup = auditoryMatchIndices(events, nBack).filter { it < nBack }

        assertTrue(
            "Visual match found inside warmup window: $visualInWarmup",
            visualInWarmup.isEmpty(),
        )
        assertTrue(
            "Auditory match found inside warmup window: $auditoryInWarmup",
            auditoryInWarmup.isEmpty(),
        )
    }

    // ---------- match rate in [0.20, 0.40], per modality, across many seeds ----------

    @Test
    fun matchRate_fallsWithin20To40Percent_acrossManySeeds() {
        val totalEvents = 30
        val nBack = 2
        val matchable = totalEvents - nBack

        for (seed in 1L..50L) {
            val events = SequenceGenerator(Random(seed)).generate(
                totalEvents = totalEvents,
                nBack = nBack,
                gridSize = 3,
                letters = defaultLetters,
            )
            val vRate = visualMatchIndices(events, nBack).size.toDouble() / matchable
            val aRate = auditoryMatchIndices(events, nBack).size.toDouble() / matchable

            assertTrue(
                "Seed $seed: visual rate $vRate outside [0.20, 0.40]",
                vRate in 0.20..0.40,
            )
            assertTrue(
                "Seed $seed: auditory rate $aRate outside [0.20, 0.40]",
                aRate in 0.20..0.40,
            )
        }
    }

    @Test
    fun matchRate_holdsAtSpecMinimumRoundLength() {
        // SPEC range lower bound: 15 events. With nBack=5 the matchable window is 10 ticks.
        val events = SequenceGenerator(Random(7)).generate(
            totalEvents = 15, nBack = 5, gridSize = 3, letters = defaultLetters,
        )
        val rate = visualMatchIndices(events, 5).size.toDouble() / 10
        assertTrue("Visual rate $rate outside [0.20, 0.40]", rate in 0.20..0.40)
    }

    // ---------- determinism ----------

    @Test
    fun sameSeed_producesIdenticalSequences() {
        val a = SequenceGenerator(Random(123)).generate(20, 2, 3, defaultLetters)
        val b = SequenceGenerator(Random(123)).generate(20, 2, 3, defaultLetters)
        assertEquals(a, b)
    }

    // ---------- input validation ----------

    @Test(expected = IllegalArgumentException::class)
    fun rejects_totalEventsNotGreaterThanNBack() {
        SequenceGenerator(Random(1)).generate(
            totalEvents = 2, nBack = 2, gridSize = 3, letters = defaultLetters,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_gridSizeBelowTwo() {
        SequenceGenerator(Random(1)).generate(
            totalEvents = 10, nBack = 2, gridSize = 1, letters = defaultLetters,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_duplicateLetters() {
        SequenceGenerator(Random(1)).generate(
            totalEvents = 10, nBack = 2, gridSize = 3, letters = listOf('C', 'C', 'D'),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_matchRateOutsideUnitInterval() {
        SequenceGenerator(Random(1)).generate(
            totalEvents = 10, nBack = 2, gridSize = 3, letters = defaultLetters,
            targetMatchRate = 1.5,
        )
    }
}
