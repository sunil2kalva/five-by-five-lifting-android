package com.stronglifts5x5.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stronglifts5x5.app.data.model.ExerciseType

@Database(
    entities = [
        ExerciseStateEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyweightEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val DATABASE_NAME = "stronglifts_5x5.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // TRUNCATE journal mode keeps all changes committed inside the main .db file
                    // so Android Auto-Backup always captures a consistent SQLite snapshot.
                    .setJournalMode(JournalMode.TRUNCATE)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun defaultExerciseStates(): List<ExerciseStateEntity> =
            ExerciseType.entries.map { exercise ->
                ExerciseStateEntity(
                    exercise = exercise,
                    currentWeightLb = exercise.defaultStartWeightLb,
                    incrementLb = exercise.defaultIncrementLb,
                    targetSets = exercise.defaultSets,
                    targetReps = exercise.defaultReps,
                    consecutiveFailures = 0,
                    deloadCount = 0
                )
            }
    }
}
