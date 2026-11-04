package mobappdev.example.nback_cimpl

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import mobappdev.example.nback_cimpl.data.AppDatabase
import mobappdev.example.nback_cimpl.data.AudioPlayer
import mobappdev.example.nback_cimpl.data.ScoreHistoryRepository
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.domain.AudioOutput

/**
 * Manual DI root.
 *
 * Holds the one DataStore (preferences + settings) and the one Room
 * database so both repositories draw from singletons that match the
 * Application lifecycle.
 */
private const val APP_PREFERENCES_NAME = "game_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_PREFERENCES_NAME
)

class GameApplication : Application() {
    lateinit var userPreferencesRespository: UserPreferencesRepository
    lateinit var scoreHistoryRepository: ScoreHistoryRepository
    lateinit var audioOutput: AudioOutput

    override fun onCreate() {
        super.onCreate()
        userPreferencesRespository = UserPreferencesRepository(dataStore)
        scoreHistoryRepository = ScoreHistoryRepository(
            AppDatabase.get(this).scoreDao()
        )
        audioOutput = AudioPlayer(this)
    }
}
