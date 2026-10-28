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
    ),
    WORKOUT_C(
        code = "C",
        title = "Workout C",
        subtitle = "Front Squat • Incline Bench • Pull-ups • Barbell Curl"
    ),
    WORKOUT_D(
        code = "D",
        title = "Workout D",
        subtitle = "Custom Workout D"
    ),
    WORKOUT_E(
        code = "E",
        title = "Workout E",
        subtitle = "Custom Workout E"
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
        WORKOUT_C -> listOf(
            ExerciseType.FRONT_SQUAT,
            ExerciseType.INCLINE_BENCH_PRESS,
            ExerciseType.PULL_UPS,
            ExerciseType.BARBELL_CURL
        )
        WORKOUT_D -> listOf(
            ExerciseType.ROMANIAN_DEADLIFT,
            ExerciseType.CLOSE_GRIP_BENCH,
            ExerciseType.DIPS
        )
        WORKOUT_E -> listOf(
            ExerciseType.SQUAT,
            ExerciseType.BENCH_PRESS,
            ExerciseType.DEADLIFT
        )
    }

    fun next(): WorkoutType = when (this) {
        WORKOUT_A -> WORKOUT_B
        WORKOUT_B -> WORKOUT_A
        WORKOUT_C -> WORKOUT_A
        WORKOUT_D -> WORKOUT_A
        WORKOUT_E -> WORKOUT_A
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
    ),
    FRONT_SQUAT(
        displayName = "Front Squat",
        shortName = "FSQ",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    INCLINE_BENCH_PRESS(
        displayName = "Incline Bench Press",
        shortName = "IBP",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    ROMANIAN_DEADLIFT(
        displayName = "Romanian Deadlift",
        shortName = "RDL",
        defaultStartWeightLb = 95.0,
        defaultIncrementLb = 5.0,
        defaultSets = 3,
        defaultReps = 8
    ),
    CLOSE_GRIP_BENCH(
        displayName = "Close-Grip Bench",
        shortName = "CGBP",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 3,
        defaultReps = 8
    ),
    PULL_UPS(
        displayName = "Pull-ups / Chin-ups",
        shortName = "PULL",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 2.5,
        defaultSets = 3,
        defaultReps = 8
    ),
    DIPS(
        displayName = "Parallel Bar Dips",
        shortName = "DIP",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 2.5,
        defaultSets = 3,
        defaultReps = 8
    ),
    BARBELL_CURL(
        displayName = "Barbell Curl",
        shortName = "CURL",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 2.5,
        defaultSets = 3,
        defaultReps = 10
    ),
    SKULLCRUSHERS(
        displayName = "Skullcrushers",
        shortName = "SKULL",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 2.5,
        defaultSets = 3,
        defaultReps = 10
    ),
    LATERAL_RAISE(
        displayName = "Lateral Raise",
        shortName = "LAT",
        defaultStartWeightLb = 15.0,
        defaultIncrementLb = 2.5,
        defaultSets = 3,
        defaultReps = 12
    ),
    CUSTOM_1(
        displayName = "Custom Lift 1",
        shortName = "C1",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    CUSTOM_2(
        displayName = "Custom Lift 2",
        shortName = "C2",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 5,
        defaultReps = 5
    ),
    CUSTOM_3(
        displayName = "Custom Lift 3",
        shortName = "C3",
        defaultStartWeightLb = 45.0,
        defaultIncrementLb = 5.0,
        defaultSets = 3,
        defaultReps = 8
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
