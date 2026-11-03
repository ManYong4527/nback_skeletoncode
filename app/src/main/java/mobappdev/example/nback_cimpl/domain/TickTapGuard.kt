package mobappdev.example.nback_cimpl.domain

/**
 * Per-tick double-tap guard, pure Kotlin so it can be unit-tested
 * without instantiating the Android ViewModel.
 *
 * SPEC §2.1 "Double-press prevention":
 *   "Only the first tap per stimulus per modality is registered;
 *    subsequent taps within the same tick are ignored."
 *
 * The game loop calls [resetForNewTick] at every tick boundary; UI
 * callbacks invoke [acceptVisual] / [acceptAudio]. Each returns `true`
 * exactly once per tick per modality — later taps return `false` and the
 * caller must drop them on the floor.
 *
 * Visual and audio are independent (SPEC "simultaneously and
 * independently"): a visual tap does not block an audio tap, and vice
 * versa.
 */
class TickTapGuard {
    private var visualAccepted: Boolean = false
    private var audioAccepted: Boolean = false

    /** @return true iff any visual tap has been accepted this tick. */
    val visualWasTapped: Boolean get() = visualAccepted

    /** @return true iff any audio tap has been accepted this tick. */
    val audioWasTapped: Boolean get() = audioAccepted

    /** @return true iff this is the first visual tap of the current tick. */
    fun acceptVisual(): Boolean {
        if (visualAccepted) return false
        visualAccepted = true
        return true
    }

    /** @return true iff this is the first audio tap of the current tick. */
    fun acceptAudio(): Boolean {
        if (audioAccepted) return false
        audioAccepted = true
        return true
    }

    /** Clear both flags at tick boundary so the next tick starts fresh. */
    fun resetForNewTick() {
        visualAccepted = false
        audioAccepted = false
    }
}
