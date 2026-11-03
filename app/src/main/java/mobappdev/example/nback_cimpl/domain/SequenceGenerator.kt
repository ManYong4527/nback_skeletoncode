package mobappdev.example.nback_cimpl.domain

import kotlin.math.roundToInt
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
        require(nBack >= 1) { "nBack must be >= 1, was $nBack" }
        require(totalEvents > nBack) {
            "totalEvents ($totalEvents) must exceed nBack ($nBack) so some ticks are matchable"
        }
        require(gridSize >= 2) {
            "gridSize must be >= 2 so non-match ticks can differ from events[i - nBack], was $gridSize"
        }
        require(letters.size >= 2) {
            "letters must contain at least 2 symbols, had ${letters.size}"
        }
        require(letters.toSet().size == letters.size) { "letters must be distinct" }
        require(targetMatchRate in 0.0..1.0) {
            "targetMatchRate must be in [0.0, 1.0], was $targetMatchRate"
        }

        val visualPool: List<Int> = (0 until gridSize * gridSize).toList()
        val visualStream = buildStream(totalEvents, nBack, visualPool, targetMatchRate)
        val letterStream = buildStream(totalEvents, nBack, letters, targetMatchRate)

        return List(totalEvents) { i ->
            StimulusEvent(visualIndex = visualStream[i], letter = letterStream[i])
        }
    }

    /**
     * Build one modality's stream with an exact, pre-planned number of
     * N-back matches.
     *
     * The first [nBack] ticks are drawn freely from [valuePool]; they can
     * never be N-back matches because index `i - nBack` is negative.
     * The remaining `length - nBack` indices form the matchable window;
     * we pick `round(window * targetMatchRate)` of them up-front as planned
     * matches and copy from `i - nBack`. All other indices in the window
     * pick a value *excluding* `values[i - nBack]` so no accidental match
     * can inflate the rate.
     */
    private fun <T> buildStream(
        length: Int,
        nBack: Int,
        valuePool: List<T>,
        targetMatchRate: Double,
    ): List<T> {
        val matchableCount = length - nBack
        val plannedMatchCount = (matchableCount * targetMatchRate).roundToInt()
            .coerceIn(0, matchableCount)
        val plannedMatches: Set<Int> = (nBack until length)
            .shuffled(random)
            .take(plannedMatchCount)
            .toSet()

        val result = ArrayList<T>(length)
        for (i in 0 until length) {
            val value: T = when {
                i < nBack -> valuePool.random(random)
                i in plannedMatches -> result[i - nBack]
                else -> {
                    val forbidden = result[i - nBack]
                    val candidates = valuePool.filter { it != forbidden }
                    candidates.random(random)
                }
            }
            result.add(value)
        }
        return result
    }
}
