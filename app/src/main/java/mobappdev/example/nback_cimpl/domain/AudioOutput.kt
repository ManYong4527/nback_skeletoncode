package mobappdev.example.nback_cimpl.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Lifecycle of the audio engine. The VM watches this to know whether
 * Audio / Dual modes are usable right now; the UI watches it to
 * surface the SPEC §3.3 "TTS fallback" cue.
 */
enum class AudioStatus { Initializing, Ready, Failed }

/**
 * Pure-Kotlin seam for speaking letters out loud. Keeps [android.speech.tts.TextToSpeech]
 * out of the ViewModel (STYLE.md §1: the VM never touches Context) and
 * lets tests substitute a fake.
 */
interface AudioOutput {
    val status: StateFlow<AudioStatus>
    fun speak(letter: Char)
}
