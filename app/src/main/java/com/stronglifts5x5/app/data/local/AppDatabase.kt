package com.stronglifts5x5.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WorkoutType

@Database(
    entities = [
        WorkoutTemplateEntity::class,
        ExerciseStateEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyweightEntryEntity::class
    ],
    version = 2,
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

        fun defaultWorkoutTemplates(): List<WorkoutTemplateEntity> =
            listOf(
                WorkoutTemplateEntity(
                    workoutType = WorkoutType.WORKOUT_A,
                    customTitle = "Workout A",
                    exercisesCsv = WorkoutType.WORKOUT_A.exercises().joinToString(",") { it.name },
                    isEnabled = true
                ),
                WorkoutTemplateEntity(
                    workoutType = WorkoutType.WORKOUT_B,
                    customTitle = "Workout B",
                    exercisesCsv = WorkoutType.WORKOUT_B.exercises().joinToString(",") { it.name },
                    isEnabled = true
                ),
                WorkoutTemplateEntity(
                    workoutType = WorkoutType.WORKOUT_C,
                    customTitle = "Workout C",
                    exercisesCsv = WorkoutType.WORKOUT_C.exercises().joinToString(",") { it.name },
                    isEnabled = true
                ),
                WorkoutTemplateEntity(
                    workoutType = WorkoutType.WORKOUT_D,
                    customTitle = "Workout D",
                    exercisesCsv = WorkoutType.WORKOUT_D.exercises().joinToString(",") { it.name },
                    isEnabled = false
                ),
                WorkoutTemplateEntity(
                    workoutType = WorkoutType.WORKOUT_E,
                    customTitle = "Workout E",
                    exercisesCsv = WorkoutType.WORKOUT_E.exercises().joinToString(",") { it.name },
                    isEnabled = false
                )
            )
    }
}
