package mobappdev.example.nback_cimpl.domain

/**
 * Immutable snapshot of the player-configurable round parameters.
 *
 * SPEC §3.1 pins the legal ranges; the companion object holds them as
 * `IntRange` constants so both the DataStore reader (clamp on load) and
 * the Settings screen (slider bounds) draw from one source of truth.
 *
 * Defaults (nBack=2, 2000ms, 20 events, 3×3, 7 letters) match the
 * Path 1 hard-coded constants so existing round behaviour is preserved
 * before the user ever opens Settings.
 *
 * Letter pool: the first [letterCount] entries of [LETTER_POOL] (SPEC
 * requires a "fixed, phonetically distinct set"; shrinking the pool is
 * one of the configurable knobs).
 */
data class GameSettings(
    val nBack: Int = 2,
    val eventDurationMs: Long = 2000L,
    val eventsPerRound: Int = 20,
    val gridSize: Int = 3,
    val letterCount: Int = 7,
) {
    /** The active letter pool: first [letterCount] of [LETTER_POOL]. */
    val letters: List<Char> get() = LETTER_POOL.take(letterCount)

    companion object {
        val LETTER_POOL: List<Char> = listOf('C', 'D', 'G', 'K', 'P', 'T', 'V')

        val N_BACK_RANGE: IntRange = 1..5
        val EVENT_DURATION_MS_RANGE: LongRange = 1000L..3000L
        val EVENTS_PER_ROUND_RANGE: IntRange = 15..30
        val GRID_SIZE_OPTIONS: List<Int> = listOf(3, 5)
        val LETTER_COUNT_RANGE: IntRange = 2..LETTER_POOL.size

        /** Clamp each field into its SPEC-allowed range (used on DataStore reads). */
        fun sanitize(raw: GameSettings): GameSettings = GameSettings(
            nBack = raw.nBack.coerceIn(N_BACK_RANGE),
            eventDurationMs = raw.eventDurationMs.coerceIn(EVENT_DURATION_MS_RANGE),
            eventsPerRound = raw.eventsPerRound.coerceIn(EVENTS_PER_ROUND_RANGE),
            gridSize = if (raw.gridSize in GRID_SIZE_OPTIONS) raw.gridSize else 3,
            letterCount = raw.letterCount.coerceIn(LETTER_COUNT_RANGE),
        )
    }
}
