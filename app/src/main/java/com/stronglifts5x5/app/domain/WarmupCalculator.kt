package com.stronglifts5x5.app.domain

import com.stronglifts5x5.app.data.model.ExerciseType
import kotlin.math.roundToInt

data class WarmupSetSpec(
    val setNumber: Int,
    val weightLb: Double,
    val targetReps: Int
)

object WarmupCalculator {

    /**
     * Generates StrongLifts 5x5 progressive warm-up sets from the empty bar
     * (45 lb) up to the target working weight.
     *
     * Rules:
     * - If workingWeight <= barWeight + 5 lb: no separate warm-up sets needed.
     * - Deadlift starts warm-ups at 135 lb (1x5) if working weight >= 155 lb,
     *   or 95 lb (1x5) for lighter deadlifts.
     * - Squat, Bench, OHP, Row start with 2 sets x 5 reps with the empty barbell (45 lb),
     *   followed by 1x5, 1x3, and 1x2 progressive ramps rounded to 5 lb.
     */
    fun generateWarmupSets(
        exercise: ExerciseType,
        workingWeightLb: Double,
        barWeightLb: Double = 45.0
    ): List<WarmupSetSpec> {
        val startWeight = if (exercise == ExerciseType.DEADLIFT) {
            if (workingWeightLb >= 155.0) 135.0 else 65.0
        } else {
            barWeightLb
        }

        if (workingWeightLb <= startWeight + 5.0) {
            return emptyList()
        }

        val warmups = mutableListOf<WarmupSetSpec>()
        var nextSetNum = 1

        if (exercise == ExerciseType.DEADLIFT) {
            warmups.add(WarmupSetSpec(nextSetNum++, startWeight, 5))
        } else {
            // Standard StrongLifts 2x5 with empty bar
            warmups.add(WarmupSetSpec(nextSetNum++, startWeight, 5))
            warmups.add(WarmupSetSpec(nextSetNum++, startWeight, 5))
        }

        val diff = workingWeightLb - startWeight
        val intermediateSteps: List<Pair<Double, Int>> = when {
            diff <= 40.0 -> listOf(
                0.55 to 3
            )
            diff <= 90.0 -> listOf(
                0.45 to 5,
                0.75 to 3
            )
            else -> listOf(
                0.40 to 5,
                0.65 to 3,
                0.85 to 2
            )
        }

        var lastWeight = startWeight
        for ((fraction, reps) in intermediateSteps) {
            val rawWeight = startWeight + diff * fraction
            val rounded = roundToFiveLb(rawWeight)
            if (rounded > lastWeight && rounded < workingWeightLb) {
                warmups.add(WarmupSetSpec(nextSetNum++, rounded, reps))
                lastWeight = rounded
            }
        }

        return warmups
    }

    private fun roundToFiveLb(weightLb: Double): Double {
        return (weightLb / 5.0).roundToInt() * 5.0
    }
}
