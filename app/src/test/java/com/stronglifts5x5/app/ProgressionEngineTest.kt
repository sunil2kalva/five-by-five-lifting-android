package com.stronglifts5x5.app

import com.stronglifts5x5.app.data.local.ExerciseStateEntity
import com.stronglifts5x5.app.data.local.WorkoutSetEntity
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.domain.ProgressionAction
import com.stronglifts5x5.app.domain.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun `tap-to-log cycles null to 5 down to 0 and back to null`() {
        var state: Int? = null
        state = ProgressionEngine.nextRepState(state, 5)
        assertEquals(5, state)

        for (expected in 4 downTo 0) {
            state = ProgressionEngine.nextRepState(state, 5)
            assertEquals(expected, state)
        }

        state = ProgressionEngine.nextRepState(state, 5)
        assertNull(state)
    }

    @Test
    fun `recommended rest timer returns 90s on easy 5 reps, 180s on heavy 5 reps, 300s on missed rep`() {
        assertEquals(90, ProgressionEngine.recommendedRestSeconds(completedReps = 5, feltHeavy = false))
        assertEquals(180, ProgressionEngine.recommendedRestSeconds(completedReps = 5, feltHeavy = true))
        assertEquals(300, ProgressionEngine.recommendedRestSeconds(completedReps = 4, feltHeavy = false))
        assertEquals(300, ProgressionEngine.recommendedRestSeconds(completedReps = 0, feltHeavy = false))
        assertEquals(0, ProgressionEngine.recommendedRestSeconds(completedReps = null))
    }

    @Test
    fun `squat 5x5 success increments weight by 5 lb and resets failures`() {
        val squatState = ExerciseStateEntity(
            exercise = ExerciseType.SQUAT,
            currentWeightLb = 185.0,
            incrementLb = 5.0,
            targetSets = 5,
            targetReps = 5,
            consecutiveFailures = 2,
            deloadCount = 0
        )
        val sets = (1..5).map { idx ->
            WorkoutSetEntity(
                sessionId = 1L,
                exercise = ExerciseType.SQUAT,
                setNumber = idx,
                isWarmup = false,
                targetWeightLb = 185.0,
                targetReps = 5,
                completedReps = 5
            )
        }

        val outcome = ProgressionEngine.evaluateExerciseSession(squatState, sets)
        assertEquals(ProgressionAction.INCREMENTED, outcome.action)
        assertEquals(190.0, outcome.nextWeightLb, 0.001)
        assertEquals(0, outcome.nextConsecutiveFailures)
    }

    @Test
    fun `deadlift 1x5 success increments by 10 lb below 225 lb and 5 lb at or above 225 lb`() {
        val dlLight = ExerciseStateEntity(
            exercise = ExerciseType.DEADLIFT,
            currentWeightLb = 185.0,
            incrementLb = 10.0,
            targetSets = 1,
            targetReps = 5
        )
        val setLight = listOf(
            WorkoutSetEntity(
                sessionId = 1L,
                exercise = ExerciseType.DEADLIFT,
                setNumber = 1,
                isWarmup = false,
                targetWeightLb = 185.0,
                targetReps = 5,
                completedReps = 5
            )
        )
        val resLight = ProgressionEngine.evaluateExerciseSession(dlLight, setLight)
        assertEquals(195.0, resLight.nextWeightLb, 0.001)

        val dlHeavy = dlLight.copy(currentWeightLb = 225.0)
        val setHeavy = listOf(setLight.first().copy(targetWeightLb = 225.0))
        val resHeavy = ProgressionEngine.evaluateExerciseSession(dlHeavy, setHeavy)
        assertEquals(230.0, resHeavy.nextWeightLb, 0.001)
    }

    @Test
    fun `three consecutive failures trigger 10 percent deload rounded down to 5 lb`() {
        var ohpState = ExerciseStateEntity(
            exercise = ExerciseType.OVERHEAD_PRESS,
            currentWeightLb = 135.0,
            incrementLb = 5.0,
            targetSets = 5,
            targetReps = 5,
            consecutiveFailures = 0,
            deloadCount = 0
        )
        val failedSets = (1..5).map { idx ->
            WorkoutSetEntity(
                sessionId = 1L,
                exercise = ExerciseType.OVERHEAD_PRESS,
                setNumber = idx,
                isWarmup = false,
                targetWeightLb = 135.0,
                targetReps = 5,
                completedReps = if (idx < 5) 5 else 3 // missed last set
            )
        }

        // Strike 1
        val strike1 = ProgressionEngine.evaluateExerciseSession(ohpState, failedSets)
        assertEquals(ProgressionAction.REPEATED_AFTER_FAILURE, strike1.action)
        assertEquals(135.0, strike1.nextWeightLb, 0.001)
        assertEquals(1, strike1.nextConsecutiveFailures)
        ohpState = strike1.updatedEntity

        // Strike 2
        val strike2 = ProgressionEngine.evaluateExerciseSession(ohpState, failedSets)
        assertEquals(ProgressionAction.REPEATED_AFTER_FAILURE, strike2.action)
        assertEquals(135.0, strike2.nextWeightLb, 0.001)
        assertEquals(2, strike2.nextConsecutiveFailures)
        ohpState = strike2.updatedEntity

        // Strike 3 -> 135 * 0.9 = 121.5 -> rounded down to nearest 5 lb = 120.0 lb
        val strike3 = ProgressionEngine.evaluateExerciseSession(ohpState, failedSets)
        assertEquals(ProgressionAction.DELOADED_10_PERCENT, strike3.action)
        assertEquals(120.0, strike3.nextWeightLb, 0.001)
        assertEquals(0, strike3.nextConsecutiveFailures)
        assertEquals(1, strike3.nextDeloadCount)
    }
}
