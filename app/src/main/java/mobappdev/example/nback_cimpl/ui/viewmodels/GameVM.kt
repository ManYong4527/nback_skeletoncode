package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.domain.GameSettings
import mobappdev.example.nback_cimpl.domain.ScoringEngine
import mobappdev.example.nback_cimpl.domain.SequenceGenerator
import mobappdev.example.nback_cimpl.domain.StimulusEvent
import mobappdev.example.nback_cimpl.domain.TickTapGuard

/**
 * Public surface area for the Game ViewModel.
 *
 * Keeping an interface lets @Preview composables and future tests swap
 * in [FakeVM] without pulling in Android framework classes.
 */
interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    /**
     * Current player-configurable round parameters. Updating via
     * [saveSettings] writes through to DataStore; this StateFlow
     * mirrors what's persisted. In-flight rounds use the snapshot
     * taken at [startGame] time and are not disrupted mid-tick.
     */
    val settings: StateFlow<GameSettings>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun abortRound()
    fun registerVisualMatch()
    fun registerAudioMatch()
    fun saveSettings(settings: GameSettings)
}

/**
 * Owns the Dual N-Back round's timer, sequence, and scoring state.
 *
 * Path 1 scope: only [GameType.Visual] actually runs through a tick
 * loop; the auditory modality lands in a future round. Audio taps are
 * still accepted (debounced + warmup-guarded) so the UI contract stays
 * stable.
 */
class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
) : GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    override val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    /**
     * Snapshot of [settings] captured at [startGame]. Mutating settings
     * mid-round does NOT retune the loop; the next round uses the newer
     * values. Keeps round semantics predictable.
     */
    private var activeSettings: GameSettings = GameSettings()

    private val sequenceGenerator = SequenceGenerator()
    private var events: List<StimulusEvent> = emptyList()
    private var loopJob: Job? = null

    // Reset at the start of every tick; enforces SPEC §2.1 double-press prevention.
    // Backed by TickTapGuard so the logic is unit-testable without the VM.
    private val tapGuard = TickTapGuard()

    override fun setGameType(gameType: GameType) {
        _gameState.update { it.copy(gameType = gameType) }
    }

    override fun startGame() {
        loopJob?.cancel()

        val type = _gameState.value.gameType
        if (type != GameType.Visual) {
            Log.w(TAG, "GameType $type not yet implemented; aborting start")
            _gameState.update { it.copy(isRoundActive = false) }
            return
        }

        // Freeze the player's current settings for the duration of this
        // round so slider changes made mid-round can't desync events.
        activeSettings = _settings.value

        events = sequenceGenerator.generate(
            totalEvents = activeSettings.eventsPerRound,
            nBack = activeSettings.nBack,
            gridSize = activeSettings.gridSize,
            letters = activeSettings.letters,
        )
        Log.d(TAG, "Generated ${events.size} events: $events")

        _gameState.update {
            it.copy(
                totalRounds = events.size,
                roundIndex = -1,
                currentEvent = null,
                isWarmup = true,
                isRoundActive = true,
                hits = 0,
                misses = 0,
                falseAlarms = 0,
                visualFalseAlarmPulse = 0,
                audioFalseAlarmPulse = 0,
            )
        }
        _score.value = 0

        loopJob = viewModelScope.launch { runVisualRound() }
    }

    override fun saveSettings(settings: GameSettings) {
        viewModelScope.launch {
            userPreferencesRepository.saveSettings(settings)
        }
    }

    override fun registerVisualMatch() {
        val state = _gameState.value
        if (!state.isRoundActive || state.isWarmup) return
        if (!tapGuard.acceptVisual()) return

        val n = activeSettings.nBack
        val i = state.roundIndex
        if (i < n) return // defensive — isWarmup normally traps this

        val isMatch = events[i].visualIndex == events[i - n].visualIndex
        if (isMatch) {
            _gameState.update { it.copy(hits = it.hits + 1) }
        } else {
            _gameState.update {
                it.copy(
                    falseAlarms = it.falseAlarms + 1,
                    visualFalseAlarmPulse = it.visualFalseAlarmPulse + 1,
                )
            }
        }
        recomputeScore()
    }

    override fun registerAudioMatch() {
        // Path 1: auditory playback isn't wired yet. We still enforce the
        // same guards so UI callers behave identically once audio lands.
        val state = _gameState.value
        if (!state.isRoundActive || state.isWarmup) return
        if (!tapGuard.acceptAudio()) return
    }

    /**
     * Cancel an in-flight round without saving high score.
     *
     * Called when the player leaves the Game screen mid-round (BackHandler
     * or the explicit back button). Satisfies the SPEC "round timer is
     * cancelled cleanly when the user navigates away mid-round" rule.
     */
    override fun abortRound() {
        loopJob?.cancel()
        loopJob = null
        _gameState.update {
            it.copy(
                isRoundActive = false,
                isWarmup = false,
                currentEvent = null,
                roundIndex = -1,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }

    private suspend fun runVisualRound() {
        val tickMs = activeSettings.eventDurationMs
        val n = activeSettings.nBack
        val visibleMs = (tickMs * STIMULUS_VISIBLE_FRACTION).toLong()
        val gapMs = tickMs - visibleMs

        for (i in events.indices) {
            tapGuard.resetForNewTick()

            _gameState.update {
                it.copy(
                    roundIndex = i,
                    currentEvent = events[i],
                    isWarmup = i < n,
                )
            }
            delay(visibleMs)

            _gameState.update { it.copy(currentEvent = null) }

            // SPEC §2.1 "Misses": a real match the user never registered.
            if (i >= n) {
                val wasVisualMatch =
                    events[i].visualIndex == events[i - n].visualIndex
                if (wasVisualMatch && !tapGuard.visualWasTapped) {
                    _gameState.update { it.copy(misses = it.misses + 1) }
                }
            }
            delay(gapMs)
        }
        endRound()
    }

    private fun endRound() {
        _gameState.update { it.copy(isRoundActive = false, currentEvent = null) }
        val finalScore = _score.value
        if (finalScore > _highscore.value) {
            viewModelScope.launch {
                userPreferencesRepository.saveHighScore(finalScore)
            }
        }
    }

    /** Delegates to [ScoringEngine] so the formula lives in one testable place. */
    private fun recomputeScore() {
        val s = _gameState.value
        _score.value = ScoringEngine.scoreOf(hits = s.hits, falseAlarms = s.falseAlarms)
    }

    companion object {
        private const val TAG = "GameVM"
        private const val STIMULUS_VISIBLE_FRACTION: Double = 0.70

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.settings.collect { incoming ->
                _settings.value = incoming
                // Keep activeSettings in sync while no round is running so
                // the first startGame() after a settings change picks up
                // the new values without delay.
                if (!_gameState.value.isRoundActive) {
                    activeSettings = incoming
                }
            }
        }
    }
}

enum class GameType { Audio, Visual, AudioVisual }

/**
 * Snapshot of a round the GameScreen renders from.
 *
 * [visualFalseAlarmPulse] and [audioFalseAlarmPulse] are monotonic
 * counters: the UI observes them via `LaunchedEffect(pulse) { ... }` to
 * trigger a short color flash on the matching button without the VM
 * carrying any animation state.
 */
data class GameState(
    val gameType: GameType = GameType.Visual,
    val totalRounds: Int = 0,
    val roundIndex: Int = -1,
    val currentEvent: StimulusEvent? = null,
    val isWarmup: Boolean = false,
    val isRoundActive: Boolean = false,
    val hits: Int = 0,
    val misses: Int = 0,
    val falseAlarms: Int = 0,
    val visualFalseAlarmPulse: Int = 0,
    val audioFalseAlarmPulse: Int = 0,
)

/** No-op implementation for @Preview and preview-only wiring. */
class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState> =
        MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int> = MutableStateFlow(0).asStateFlow()
    override val highscore: StateFlow<Int> = MutableStateFlow(42).asStateFlow()
    override val settings: StateFlow<GameSettings> =
        MutableStateFlow(GameSettings()).asStateFlow()
    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun abortRound() {}
    override fun registerVisualMatch() {}
    override fun registerAudioMatch() {}
    override fun saveSettings(settings: GameSettings) {}
}
