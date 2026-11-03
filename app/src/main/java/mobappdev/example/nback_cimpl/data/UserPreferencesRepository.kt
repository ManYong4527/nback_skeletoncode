package mobappdev.example.nback_cimpl.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mobappdev.example.nback_cimpl.domain.GameSettings
import java.io.IOException

/**
 * DataStore-backed persistence for both the high score and the player's
 * [GameSettings]. Sharing one DataStore file keeps the backing Prefs
 * atomic across settings changes and avoids multi-file race conditions.
 *
 * All reads emit via [Flow] so the UI (Home card, Settings sliders) and
 * the ViewModel stay reactive — change a slider, the Home readout
 * updates without any explicit refresh.
 *
 * Writes are suspend functions dispatched on the DataStore's internal
 * IO executor; callers should launch them on [kotlinx.coroutines.Dispatchers.IO]
 * per STYLE.md §3 ("DataStore operations strictly on Dispatchers.IO").
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        const val TAG = "UserPreferencesRepo"

        val HIGHSCORE = intPreferencesKey("highscore")
        val N_BACK = intPreferencesKey("n_back")
        val EVENT_DURATION_MS = longPreferencesKey("event_duration_ms")
        val EVENTS_PER_ROUND = intPreferencesKey("events_per_round")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val LETTER_COUNT = intPreferencesKey("letter_count")
    }

    /** Guard every DataStore read against transient IO exceptions. */
    private val prefs: Flow<Preferences> = dataStore.data.catch {
        if (it is IOException) {
            Log.e(TAG, "Error reading preferences", it)
            emit(emptyPreferences())
        } else {
            throw it
        }
    }

    val highscore: Flow<Int> = prefs.map { it[HIGHSCORE] ?: 0 }

    /**
     * Current [GameSettings] snapshot. Missing keys fall back to the
     * data-class defaults, and every value passes through
     * [GameSettings.sanitize] so an out-of-range value in storage (older
     * build, future downgrade) cannot push the engine out of SPEC.
     */
    val settings: Flow<GameSettings> = prefs.map { p ->
        val defaults = GameSettings()
        GameSettings.sanitize(
            GameSettings(
                nBack = p[N_BACK] ?: defaults.nBack,
                eventDurationMs = p[EVENT_DURATION_MS] ?: defaults.eventDurationMs,
                eventsPerRound = p[EVENTS_PER_ROUND] ?: defaults.eventsPerRound,
                gridSize = p[GRID_SIZE] ?: defaults.gridSize,
                letterCount = p[LETTER_COUNT] ?: defaults.letterCount,
            )
        )
    }

    suspend fun saveHighScore(score: Int) {
        dataStore.edit { it[HIGHSCORE] = score }
    }

    suspend fun saveSettings(settings: GameSettings) {
        val sanitized = GameSettings.sanitize(settings)
        dataStore.edit { p ->
            p[N_BACK] = sanitized.nBack
            p[EVENT_DURATION_MS] = sanitized.eventDurationMs
            p[EVENTS_PER_ROUND] = sanitized.eventsPerRound
            p[GRID_SIZE] = sanitized.gridSize
            p[LETTER_COUNT] = sanitized.letterCount
        }
    }
}
