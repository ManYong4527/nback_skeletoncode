package mobappdev.example.nback_cimpl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the app.
 *
 * Only one entity today ([ScoreRecord]). `exportSchema = false` because
 * this is a student project — no migration history to preserve across
 * schema changes. In production the schema would be exported and
 * migrations written.
 */
@Database(
    entities = [ScoreRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scoreDao(): ScoreDao

    companion object {
        private const val DB_NAME = "nback.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    // Dev-only convenience: wipe and recreate on schema change.
                    // Fine for a student project; a real app would write migrations.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
