package mobappdev.example.nback_cimpl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [TickTapGuard], the extracted double-press
 * prevention core.
 *
 * Covers CONTEXT.md §2.1 second requirement:
 *   "Rapid successive taps within the same event tick only increment
 *    matches once."
 *
 * The guard enforces one-acceptance-per-modality-per-tick while keeping
 * the two modalities independent (SPEC "simultaneously and
 * independently"). GameVM delegates its tap filtering here, so testing
 * the guard locks the behaviour in without spinning up the ViewModel or
 * the coroutine loop.
 */
class DoublePressPreventionTest {

    // ---------- single-tap acceptance ----------

    @Test
    fun freshGuard_acceptsFirstVisualTap() {
        val guard = TickTapGuard()
        assertTrue(guard.acceptVisual())
        assertTrue(guard.visualWasTapped)
    }

    @Test
    fun freshGuard_acceptsFirstAudioTap() {
        val guard = TickTapGuard()
        assertTrue(guard.acceptAudio())
        assertTrue(guard.audioWasTapped)
    }

    @Test
    fun freshGuard_reportsNoTapsBeforeAnyCall() {
        val guard = TickTapGuard()
        assertFalse(guard.visualWasTapped)
        assertFalse(guard.audioWasTapped)
    }

    // ---------- double-press rejection ----------

    @Test
    fun secondVisualTap_sameTick_isRejected() {
        val guard = TickTapGuard()
        guard.acceptVisual()
        assertFalse(guard.acceptVisual())
    }

    @Test
    fun secondAudioTap_sameTick_isRejected() {
        val guard = TickTapGuard()
        guard.acceptAudio()
        assertFalse(guard.acceptAudio())
    }

    @Test
    fun manyRapidVisualTaps_onlyFirstIsAccepted() {
        val guard = TickTapGuard()
        val results = (1..10).map { guard.acceptVisual() }
        assertEquals(1, results.count { it })
    }

    @Test
    fun manyRapidAudioTaps_onlyFirstIsAccepted() {
        val guard = TickTapGuard()
        val results = (1..10).map { guard.acceptAudio() }
        assertEquals(1, results.count { it })
    }

    // ---------- modality independence ----------

    @Test
    fun visualTap_doesNotBlockAudioTap() {
        val guard = TickTapGuard()
        guard.acceptVisual()
        assertTrue(guard.acceptAudio())
    }

    @Test
    fun audioTap_doesNotBlockVisualTap() {
        val guard = TickTapGuard()
        guard.acceptAudio()
        assertTrue(guard.acceptVisual())
    }

    // ---------- reset at tick boundary ----------

    @Test
    fun resetForNewTick_allowsVisualTapAgain() {
        val guard = TickTapGuard()
        guard.acceptVisual()
        guard.resetForNewTick()
        assertTrue(guard.acceptVisual())
    }

    @Test
    fun resetForNewTick_clearsBothModalityFlags() {
        val guard = TickTapGuard()
        guard.acceptVisual()
        guard.acceptAudio()
        guard.resetForNewTick()
        assertFalse(guard.visualWasTapped)
        assertFalse(guard.audioWasTapped)
    }

    @Test
    fun manyTicksInSequence_eachAllowsExactlyOneAcceptPerModality() {
        val guard = TickTapGuard()
        var acceptedVisual = 0
        var acceptedAudio = 0
        repeat(5) {
            guard.resetForNewTick()
            repeat(3) { if (guard.acceptVisual()) acceptedVisual++ }
            repeat(3) { if (guard.acceptAudio()) acceptedAudio++ }
        }
        assertEquals(5, acceptedVisual) // one accept per tick × 5 ticks
        assertEquals(5, acceptedAudio)
    }
}
