package mobappdev.example.nback_cimpl.data

import kotlinx.coroutines.flow.Flow

/**
 * Sort options the history UI exposes. Kept in the data layer so both
 * the HistoryScreen chips and the repository dispatcher pull from the
 * same enum.
 */
enum class ScoreSort { Score, Name, Date }

/**
 * Thin façade over [ScoreDao]: picks the right query for the UI's sort
 * selection and offers a single [recordScore] entry point for the
 * GameVM to call after a round ends.
 */
class ScoreHistoryRepository(private val dao: ScoreDao) {

    fun scores(sortBy: ScoreSort): Flow<List<ScoreRecord>> = when (sortBy) {
        ScoreSort.Score -> dao.observeOrderedByScore()
        ScoreSort.Name -> dao.observeOrderedByName()
        ScoreSort.Date -> dao.observeOrderedByDate()
    }

    suspend fun recordScore(
        playerName: String,
        score: Int,
        nValue: Int,
        timestamp: Long = System.currentTimeMillis(),
    ): Long = dao.insert(
        ScoreRecord(
            playerName = playerName.trim(),
            score = score,
            nValue = nValue,
            timestamp = timestamp,
        )
    )
}
