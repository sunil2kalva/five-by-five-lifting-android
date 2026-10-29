package com.fivebyfivelifting.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_templates ORDER BY workoutType ASC")
    fun observeWorkoutTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates ORDER BY workoutType ASC")
    suspend fun getWorkoutTemplates(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_templates WHERE workoutType = :workoutType LIMIT 1")
    suspend fun getWorkoutTemplate(workoutType: WorkoutType): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutTemplates(templates: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutTemplate(template: WorkoutTemplateEntity)

    @Query("SELECT * FROM exercise_states ORDER BY exercise ASC")
    fun observeExerciseStates(): Flow<List<ExerciseStateEntity>>

    @Query("SELECT * FROM exercise_states ORDER BY exercise ASC")
    suspend fun getExerciseStates(): List<ExerciseStateEntity>

    @Query("SELECT * FROM exercise_states WHERE exercise = :exercise LIMIT 1")
    suspend fun getExerciseState(exercise: ExerciseType): ExerciseStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseStates(states: List<ExerciseStateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseState(state: ExerciseStateEntity)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 0 ORDER BY startedAtEpochMillis DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 0 ORDER BY startedAtEpochMillis DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionWithSets?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY completedAtEpochMillis DESC")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis ASC")
    suspend fun getAllSessionsWithSets(): List<WorkoutSessionWithSets>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun updateSet(workoutSet: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getSetById(setId: Long): WorkoutSetEntity?

    @Query("SELECT * FROM bodyweight_entries ORDER BY recordedAtEpochMillis ASC")
    fun observeBodyweightEntries(): Flow<List<BodyweightEntryEntity>>

    @Query("SELECT * FROM bodyweight_entries ORDER BY recordedAtEpochMillis ASC")
    suspend fun getBodyweightEntries(): List<BodyweightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyweightEntry(entry: BodyweightEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyweightEntries(entries: List<BodyweightEntryEntity>)

    @Query("DELETE FROM workout_sets")
    suspend fun clearAllSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM bodyweight_entries")
    suspend fun clearAllBodyweightEntries()
}
