package mobappdev.example.nback_cimpl.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.data.ScoreHistoryRepository
import mobappdev.example.nback_cimpl.data.ScoreRecord
import mobappdev.example.nback_cimpl.data.ScoreSort

/**
 * Public surface for the History ViewModel. Preview composables use
 * [FakeHistoryVM] so they don't need a real Room database.
 */
interface HistoryViewModel {
    val sortBy: StateFlow<ScoreSort>
    val scores: StateFlow<List<ScoreRecord>>
    fun setSortBy(sort: ScoreSort)
}

/**
 * Loads score rows from Room, re-queried whenever the player picks a
 * different sort. `flatMapLatest` cancels the previous DAO flow so the
 * UI never has to deduplicate between sort switches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryVM(
    private val repository: ScoreHistoryRepository,
) : HistoryViewModel, ViewModel() {

    private val _sortBy = MutableStateFlow(ScoreSort.Score)
    override val sortBy: StateFlow<ScoreSort> = _sortBy.asStateFlow()

    override val scores: StateFlow<List<ScoreRecord>> = _sortBy
        .flatMapLatest { sort -> repository.scores(sort) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = emptyList(),
        )

    override fun setSortBy(sort: ScoreSort) {
        _sortBy.value = sort
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                HistoryVM(application.scoreHistoryRepository)
            }
        }
    }
}

/** Preview-only no-op with a couple of seeded rows so the LazyColumn isn't empty. */
class FakeHistoryVM : HistoryViewModel {
    override val sortBy: StateFlow<ScoreSort> =
        MutableStateFlow(ScoreSort.Score).asStateFlow()
    override val scores: StateFlow<List<ScoreRecord>> = MutableStateFlow(
        listOf(
            ScoreRecord(id = 1, playerName = "Ada", score = 80, nValue = 2, timestamp = 1_700_000_000_000L),
            ScoreRecord(id = 2, playerName = "Bo", score = 45, nValue = 3, timestamp = 1_700_000_500_000L),
        )
    ).asStateFlow()
    override fun setSortBy(sort: ScoreSort) {}
}
