package mobappdev.example.nback_cimpl.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mobappdev.example.nback_cimpl.domain.AudioOutput
import mobappdev.example.nback_cimpl.domain.AudioStatus
import java.util.Locale

/**
 * Android [TextToSpeech] implementation of [AudioOutput].
 *
 * TTS initialisation is async: [status] starts at [AudioStatus.Initializing]
 * and flips to [AudioStatus.Ready] or [AudioStatus.Failed] once the engine
 * reports back. [speak] is a no-op outside Ready, so callers can fire
 * taps-into-the-wind during the warmup gap without checking status.
 *
 * SPEC §3.3 fallback: callers observe [status] — when it reads Failed,
 * the Settings picker greys out Audio / Dual and the VM declines to
 * start an audio-bearing round.
 *
 * Not shut down explicitly: Application has no guaranteed teardown hook
 * (onTerminate runs only in the emulator). The TTS binder is released
 * by the OS when the process exits. Matches the usual Android pattern.
 */
class AudioPlayer(context: Context) : AudioOutput {

    private val _status = MutableStateFlow(AudioStatus.Initializing)
    override val status: StateFlow<AudioStatus> = _status.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { initCode ->
        _status.value = resolveInitState(initCode)
    }

    private fun resolveInitState(initCode: Int): AudioStatus {
        if (initCode != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed with code $initCode")
            return AudioStatus.Failed
        }
        val langResult = tts.setLanguage(Locale.US)
        return if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w(TAG, "TextToSpeech language unavailable (result=$langResult)")
            AudioStatus.Failed
        } else {
            AudioStatus.Ready
        }
    }

    override fun speak(letter: Char) {
        if (_status.value != AudioStatus.Ready) return
        val text = letter.toString()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    private companion object {
        const val TAG = "AudioPlayer"
    }
}
