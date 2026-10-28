package com.stronglifts5x5.app

import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.domain.WarmupCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupCalculatorTest {

    @Test
    fun `empty bar working weight generates no extra warmup sets`() {
        val warmups = WarmupCalculator.generateWarmupSets(
            exercise = ExerciseType.SQUAT,
            workingWeightLb = 45.0,
            barWeightLb = 45.0
        )
        assertTrue(warmups.isEmpty())
    }

    @Test
    fun `squat at 185 lb starts with 2x5 empty bar and ramps progressively`() {
        val warmups = WarmupCalculator.generateWarmupSets(
            exercise = ExerciseType.SQUAT,
            workingWeightLb = 185.0,
            barWeightLb = 45.0
        )
        // 2x5 @ 45 lb + 3 progressive sets
        assertEquals(5, warmups.size)
        assertEquals(45.0, warmups[0].weightLb, 0.001)
        assertEquals(5, warmups[0].targetReps)
        assertEquals(45.0, warmups[1].weightLb, 0.001)
        assertEquals(5, warmups[1].targetReps)

        // All intermediate weights must be strictly increasing and less than 185 lb
        for (i in 2 until warmups.size) {
            assertTrue(warmups[i].weightLb > warmups[i - 1].weightLb)
            assertTrue(warmups[i].weightLb < 185.0)
        }
    }
}
