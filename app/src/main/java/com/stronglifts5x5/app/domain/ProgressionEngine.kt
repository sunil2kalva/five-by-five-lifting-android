package com.stronglifts5x5.app.domain

import com.stronglifts5x5.app.data.local.ExerciseStateEntity
import com.stronglifts5x5.app.data.local.WorkoutSetEntity
import com.stronglifts5x5.app.data.model.ExerciseType
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

enum class ProgressionAction {
    INCREMENTED,
    REPEATED_AFTER_FAILURE,
    DELOADED_10_PERCENT
}

data class ExerciseProgressionResult(
    val exercise: ExerciseType,
    val previousWeightLb: Double,
    val nextWeightLb: Double,
    val previousFailures: Int,
    val nextConsecutiveFailures: Int,
    val nextDeloadCount: Int,
    val action: ProgressionAction,
    val updatedEntity: ExerciseStateEntity
)

object ProgressionEngine {

    const val MAX_FAILURES_BEFORE_DELOAD = 3
    const val DELOAD_FACTOR = 0.90
    const val STANDARD_ROUNDING_STEP_LB = 5.0

    /**
     * Cycles a set's completed reps when the user taps the circular bubble:
     * null (unlogged) -> 5 -> 4 -> 3 -> 2 -> 1 -> 0 -> null
     */
    fun nextRepState(currentReps: Int?, targetReps: Int = 5): Int? {
        return when {
            currentReps == null -> targetReps
            currentReps > 0 -> currentReps - 1
            else -> null
        }
    }

    /**
     * Context-aware rest timer duration in seconds:
     * - Completed all 5 reps (normal): 90s (1:30)
     * - Completed all 5 reps (hard): 180s (3:00)
     * - Failed set (0..4 reps): 300s (5:00)
     */
    fun recommendedRestSeconds(
        completedReps: Int?,
        targetReps: Int = 5,
        feltHeavy: Boolean = false
    ): Int {
        if (completedReps == null) return 0
        return when {
            completedReps >= targetReps && !feltHeavy -> 90
            completedReps >= targetReps && feltHeavy -> 180
            else -> 300
        }
    }

    /**
     * Evaluates an exercise's working sets upon finishing a workout and computes
     * the next session's weight, consecutive failure count, or 10% deload.
     */
    fun evaluateExerciseSession(
        state: ExerciseStateEntity,
        workingSets: List<WorkoutSetEntity>,
        minBarWeightLb: Double = 45.0
    ): ExerciseProgressionResult {
        val relevantSets = workingSets.filter { !it.isWarmup && it.exercise == state.exercise }
        val completedTargetSets = relevantSets.count { (it.completedReps ?: -1) >= state.targetReps }
        val allSetsSucceeded = relevantSets.isNotEmpty() && completedTargetSets >= state.targetSets

        if (allSetsSucceeded) {
            // Deadlift progresses by +10 lb up to 225 lb, then +5 lb above 225 lb if using default 10 lb increment
            val effectiveIncrement = if (
                state.exercise == ExerciseType.DEADLIFT &&
                state.currentWeightLb >= 225.0 &&
                state.incrementLb >= 10.0
            ) {
                5.0
            } else {
                state.incrementLb
            }

            val nextWeight = state.currentWeightLb + effectiveIncrement
            val updated = state.copy(
                currentWeightLb = nextWeight,
                consecutiveFailures = 0
            )
            return ExerciseProgressionResult(
                exercise = state.exercise,
                previousWeightLb = state.currentWeightLb,
                nextWeightLb = nextWeight,
                previousFailures = state.consecutiveFailures,
                nextConsecutiveFailures = 0,
                nextDeloadCount = state.deloadCount,
                action = ProgressionAction.INCREMENTED,
                updatedEntity = updated
            )
        } else {
            val nextFailures = state.consecutiveFailures + 1
            return if (nextFailures >= MAX_FAILURES_BEFORE_DELOAD) {
                val deloadedWeight = calculateTenPercentDeloadLb(
                    currentWeightLb = state.currentWeightLb,
                    minBarWeightLb = minBarWeightLb
                )
                val updated = state.copy(
                    currentWeightLb = deloadedWeight,
                    consecutiveFailures = 0,
                    deloadCount = state.deloadCount + 1
                )
                ExerciseProgressionResult(
                    exercise = state.exercise,
                    previousWeightLb = state.currentWeightLb,
                    nextWeightLb = deloadedWeight,
                    previousFailures = state.consecutiveFailures,
                    nextConsecutiveFailures = 0,
                    nextDeloadCount = state.deloadCount + 1,
                    action = ProgressionAction.DELOADED_10_PERCENT,
                    updatedEntity = updated
                )
            } else {
                val updated = state.copy(
                    consecutiveFailures = nextFailures
                )
                ExerciseProgressionResult(
                    exercise = state.exercise,
                    previousWeightLb = state.currentWeightLb,
                    nextWeightLb = state.currentWeightLb,
                    previousFailures = state.consecutiveFailures,
                    nextConsecutiveFailures = nextFailures,
                    nextDeloadCount = state.deloadCount,
                    action = ProgressionAction.REPEATED_AFTER_FAILURE,
                    updatedEntity = updated
                )
            }
        }
    }

    /**
     * Reduces current working weight by 10%, rounded down to the nearest 5 lb increment,
     * and never below the empty barbell weight.
     */
    fun calculateTenPercentDeloadLb(
        currentWeightLb: Double,
        minBarWeightLb: Double = 45.0,
        roundingStepLb: Double = STANDARD_ROUNDING_STEP_LB
    ): Double {
        if (currentWeightLb <= minBarWeightLb) return minBarWeightLb
        val rawReduced = currentWeightLb * DELOAD_FACTOR
        val roundedDown = floor((rawReduced + 1e-6) / roundingStepLb) * roundingStepLb
        // Ensure at least 1 step reduction if currentWeight > minBarWeight
        val effectiveReduced = if (roundedDown >= currentWeightLb) {
            currentWeightLb - roundingStepLb
        } else {
            roundedDown
        }
        return max(minBarWeightLb, effectiveReduced)
    }

    /**
     * Epley 1-Rep Max estimation formula: weight * (1 + reps / 30.0)
     */
    fun estimateOneRepMaxLb(weightLb: Double, reps: Int): Double {
        if (reps <= 0 || weightLb <= 0.0) return 0.0
        if (reps == 1) return weightLb
        val raw = weightLb * (1.0 + reps / 30.0)
        return (raw * 10.0).roundToInt() / 10.0
    }
}
