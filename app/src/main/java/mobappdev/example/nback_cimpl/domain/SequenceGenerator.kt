package mobappdev.example.nback_cimpl.domain

import kotlin.random.Random

/**
 * Pure-Kotlin generator for a Dual N-Back round's stimulus sequence.
 *
 * No Android SDK dependencies: the whole class is unit-testable on the JVM
 * without Robolectric or mocks. A caller may inject a seeded [Random] for
 * deterministic tests.
 *
 * Guarantees provided by [generate]:
 *  - The returned list has exactly `totalEvents` elements.
 *  - For every index `i` in `0 until nBack`, neither modality matches
 *    `events[i - nBack]` — because that index does not exist. The first
 *    `nBack` ticks are therefore never counted as matches by construction.
 *  - For each modality independently, the fraction of indices `i >= nBack`
 *    where `events[i] == events[i - nBack]` equals `round(K * targetMatchRate) / K`,
 *    where `K = totalEvents - nBack`. With the default rate of 0.30 this
 *    always lies in the SPEC-required `[0.20, 0.40]` window.
 */
class SequenceGenerator(
    private val random: Random = Random.Default,
) {

    /**
     * Build one round's worth of [StimulusEvent]s.
     *
     * @param totalEvents  Length of the round (SPEC range: 15..30).
     * @param nBack        The N in N-back (SPEC range: 1..5).
     * @param gridSize     Side length of the visual grid; positions span
     *                     `0 until gridSize * gridSize`.
     * @param letters      Letter pool for the auditory stream; must be
     *                     non-empty and distinct.
     * @param targetMatchRate Desired match fraction over the matchable
     *                     window `nBack until totalEvents`, applied
     *                     independently to each modality. Default 0.30
     *                     keeps us safely inside `[0.20, 0.40]`.
     *
     * @throws IllegalArgumentException if any argument violates the
     *   stated preconditions (bad range, empty/duplicated letters, grid
     *   too small to avoid forced matches, etc.).
     */
    fun generate(
        totalEvents: Int,
        nBack: Int,
        gridSize: Int,
        letters: List<Char>,
        targetMatchRate: Double = 0.30,
    ): List<StimulusEvent> {
        TODO("Step 2: implement constructive generation")
    }
}
