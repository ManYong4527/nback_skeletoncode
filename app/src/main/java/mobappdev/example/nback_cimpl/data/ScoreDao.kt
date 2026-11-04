package mobappdev.example.nback_cimpl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Three sort orders map to three `@Query` methods so SQLite can plan each
 * one with proper indexing. Room does not allow a column name in an
 * `ORDER BY` placeholder without `RawQuery`, which this scope doesn't
 * need.
 */
@Dao
interface ScoreDao {

    @Insert
    suspend fun insert(score: ScoreRecord): Long

    @Query("SELECT * FROM scores ORDER BY score DESC, timestamp DESC")
    fun observeOrderedByScore(): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM scores ORDER BY playerName COLLATE NOCASE ASC, timestamp DESC")
    fun observeOrderedByName(): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    fun observeOrderedByDate(): Flow<List<ScoreRecord>>
}
