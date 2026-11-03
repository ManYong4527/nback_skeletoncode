package mobappdev.example.nback_cimpl.domain

import kotlin.math.max

/**
 * Pure-Kotlin scoring for one N-Back round.
 *
 * SPEC §2.1 fixes the formula:
 *   Score = max(0, (Hits * 10) - (FalseAlarms * 5))
 *
 * Misses are tracked separately for player feedback but have no effect
 * on the score. The `max(0, ...)` clamp is load-bearing: without it,
 * runs where false alarms outnumber hits would surface negative scores,
 * which SPEC explicitly forbids.
 *
 * Kept as an `object` because the scoring rule is a pure total function
 * with no round-local state. Testable on the JVM without any Android
 * dependencies (CONTEXT.md §2.1 third required test lives against this).
 */
object ScoringEngine {
    const val HIT_POINTS: Int = 10
    const val FALSE_ALARM_PENALTY: Int = 5

    /** @return the player's score given the running hit and false-alarm counts. */
    fun scoreOf(hits: Int, falseAlarms: Int): Int {
        require(hits >= 0) { "hits must be >= 0, was $hits" }
        require(falseAlarms >= 0) { "falseAlarms must be >= 0, was $falseAlarms" }
        return max(0, hits * HIT_POINTS - falseAlarms * FALSE_ALARM_PENALTY)
    }
}
