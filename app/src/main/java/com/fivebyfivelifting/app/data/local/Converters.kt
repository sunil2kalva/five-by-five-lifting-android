package com.fivebyfivelifting.app.data.local

import androidx.room.TypeConverter
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WorkoutType

class Converters {
    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String = value.name

    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)
}
