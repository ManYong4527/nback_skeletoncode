package mobappdev.example.nback_cimpl.domain

/**
 * One tick of a Dual N-Back round.
 *
 * Each event carries both modalities so that the visual and auditory streams
 * stay index-aligned: `events[i].visualIndex` and `events[i].letter` are the
 * stimuli presented together at tick `i`.
 *
 * @property visualIndex Index of the highlighted cell, in `0 until gridSize * gridSize`.
 * @property letter      Spoken letter, drawn from the configured letter pool.
 */
data class StimulusEvent(
    val visualIndex: Int,
    val letter: Char,
)
