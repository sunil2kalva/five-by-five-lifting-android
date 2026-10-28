package com.stronglifts5x5.app.data.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class WorkoutType(
    val code: String,
    val title: String,
    val subtitle: String
) {
    WORKOUT_A(
        code = "A",
        title = "Workout A",
        subtitle = "Squat • Bench Press • Barbell Row"
    ),
    WORKOUT_B(
        code = "B",
        title = "Workout B",
        subtitle = "Squat • Overhead Press • Deadlift"
    );

    fun exercises(): List<ExerciseType> = when (this) {
        WORKOUT_A -> listOf(
            ExerciseType.SQUAT,
            ExerciseType.BENCH_PRESS,
            ExerciseType.BARBELL_ROW
        )
        WORKOUT_B -> listOf(
            ExerciseType.SQUAT,
            ExerciseType.OVERHEAD_PRESS,
            ExerciseType.DEADLIFT
        )
    }

    fun next(): WorkoutType = when (this) {
        WORKOUT_A -> WORKOUT_B
        WORKOUT_B -> WORKOUT_A
    }
}

@Serializable
enum class ExerciseType(
    val displayName: String,
    val shortName: String,
    val defaultStartWeightLb: Double,
    val defaultIncrementLb: Double,
    val defaultSets: Int,
    val defaultReps: Int
) {
    SQUAT(
        displayName = "Squat",
        shortName = "SQ",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    BENCH_PRESS(
        displayName = "Bench Press",
        shortName = "BP",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    BARBELL_ROW(
        displayName = "Barbell Row",
        shortName = "ROW",
        defaultStartWeightLb = 65.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    OVERHEAD_PRESS(
        displayName = "Overhead Press",
        shortName = "OHP",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    DEADLIFT(
        displayName = "Deadlift",
        shortName = "DL",
        defaultStartWeightLb = 95.0,
        defaultIncrementLb = 10.0,
        defaultSets = 1,
        defaultReps = 5
    )
}

@Serializable
enum class WeightUnit(
    val label: String,
    val defaultBarWeightLb: Double,
    val defaultBarWeightDisplay: Double,
    val minStepDisplay: Double
) {
    LB(
        label = "lb",
        defaultBarWeightLb = 45.0,
        defaultBarWeightDisplay = 45.0,
        minStepDisplay = 5.0
    ),
    KG(
        label = "kg",
        defaultBarWeightLb = 44.09245, // 20 kg
        defaultBarWeightDisplay = 20.0,
        minStepDisplay = 2.5
    );

    fun fromLb(weightLb: Double): Double = when (this) {
        LB -> roundToStep(weightLb, 2.5)
        KG -> roundToStep(weightLb * LB_TO_KG, 1.25)
    }

    fun toLb(displayWeight: Double): Double = when (this) {
        LB -> displayWeight
        KG -> displayWeight / LB_TO_KG
    }

    fun format(weightLb: Double, includeUnit: Boolean = true): String {
        val converted = fromLb(weightLb)
        val numberStr = if (converted % 1.0 == 0.0) {
            converted.roundToInt().toString()
        } else {
            String.format("%.1f", converted).trimEnd('0').trimEnd('.')
        }
        return if (includeUnit) "$numberStr $label" else numberStr
    }

    companion object {
        const val LB_TO_KG = 0.45359237

        fun roundToStep(value: Double, step: Double): Double {
            if (step <= 0.0) return value
            return (value / step).roundToInt() * step
        }
    }
}
