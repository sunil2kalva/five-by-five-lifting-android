package com.stronglifts5x5.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WorkoutType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exercise_states")
data class ExerciseStateEntity(
    @PrimaryKey val exercise: ExerciseType,
    val currentWeightLb: Double,
    val incrementLb: Double,
    val targetSets: Int,
    val targetReps: Int,
    val consecutiveFailures: Int = 0,
    val deloadCount: Int = 0
)

@Serializable
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutType: WorkoutType,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val bodyweightLb: Double? = null,
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exercise: ExerciseType,
    val setNumber: Int,
    val isWarmup: Boolean = false,
    val targetWeightLb: Double,
    val targetReps: Int = 5,
    val completedReps: Int? = null // null = unlogged, 5..0 = tapped state
)

@Serializable
@Entity(tableName = "bodyweight_entries")
data class BodyweightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAtEpochMillis: Long,
    val weightLb: Double,
    val sessionId: Long? = null
)

@Serializable
data class WorkoutSessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<WorkoutSetEntity>
) {
    fun workingSetsFor(exercise: ExerciseType): List<WorkoutSetEntity> =
        sets.filter { !it.isWarmup && it.exercise == exercise }
            .sortedBy { it.setNumber }

    fun warmupSetsFor(exercise: ExerciseType): List<WorkoutSetEntity> =
        sets.filter { it.isWarmup && it.exercise == exercise }
            .sortedBy { it.setNumber }

    fun totalVolumeLb(): Double =
        sets.filter { !it.isWarmup && (it.completedReps ?: 0) > 0 }
            .sumOf { it.targetWeightLb * (it.completedReps ?: 0) }
}
