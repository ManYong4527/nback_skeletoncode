package mobappdev.example.nback_cimpl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One historical round result.
 *
 * Schema matches SPEC §3.2 to the letter: id, playerName, score,
 * nValue, timestamp. Stored in Room so the History screen (Advanced
 * feature #2) can list and sort rounds across app restarts.
 *
 * `timestamp` is epoch milliseconds — simpler than java.time types for
 * Room/SQLite without a TypeConverter.
 */
@Entity(tableName = "scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playerName: String,
    val score: Int,
    val nValue: Int,
    val timestamp: Long,
)
