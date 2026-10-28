package com.stronglifts5x5.app

import com.stronglifts5x5.app.data.local.AppDatabase
import com.stronglifts5x5.app.data.local.BodyweightEntryEntity
import com.stronglifts5x5.app.data.local.WorkoutSessionEntity
import com.stronglifts5x5.app.data.local.WorkoutSessionWithSets
import com.stronglifts5x5.app.data.local.WorkoutSetEntity
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WeightUnit
import com.stronglifts5x5.app.data.model.WorkoutType
import com.stronglifts5x5.app.domain.AppBackupSnapshot
import com.stronglifts5x5.app.domain.BackupExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAndCloudSyncTest {

    @Test
    fun `json roundtrip preserves workout sessions and progression states`() {
        val sessionA = WorkoutSessionWithSets(
            session = WorkoutSessionEntity(
                id = 1L,
                workoutType = WorkoutType.WORKOUT_A,
                startedAtEpochMillis = 1_700_000_000_000L,
                completedAtEpochMillis = 1_700_000_003_600L,
                bodyweightLb = 175.0,
                notes = "Felt strong",
                isCompleted = true
            ),
            sets = listOf(
                WorkoutSetEntity(
                    id = 10L,
                    sessionId = 1L,
                    exercise = ExerciseType.SQUAT,
                    setNumber = 1,
                    isWarmup = false,
                    targetWeightLb = 185.0,
                    targetReps = 5,
                    completedReps = 5
                )
            )
        )

        val snapshot = AppBackupSnapshot(
            exportedAtEpochMillis = 1_700_000_010_000L,
            weightUnit = WeightUnit.LB,
            barbellWeightLb = 45.0,
            exerciseStates = AppDatabase.defaultExerciseStates(),
            sessions = listOf(sessionA),
            bodyweightEntries = listOf(
                BodyweightEntryEntity(id = 1L, recordedAtEpochMillis = 1_700_000_003_600L, weightLb = 175.0)
            )
        )

        val json = BackupExportManager.toJson(snapshot)
        val restored = BackupExportManager.fromJson(json)

        assertEquals(1, restored.sessions.size)
        assertEquals(WorkoutType.WORKOUT_A, restored.sessions.first().session.workoutType)
        assertEquals(185.0, restored.sessions.first().sets.first().targetWeightLb, 0.001)
    }

    @Test
    fun `smart cloud merge combines unique sessions from old phone and new phone without duplicates`() {
        val session1 = WorkoutSessionWithSets(
            session = WorkoutSessionEntity(
                id = 1L,
                workoutType = WorkoutType.WORKOUT_A,
                startedAtEpochMillis = 1_000L,
                completedAtEpochMillis = 2_000L,
                isCompleted = true
            ),
            sets = emptyList()
        )
        val session2 = WorkoutSessionWithSets(
            session = WorkoutSessionEntity(
                id = 2L,
                workoutType = WorkoutType.WORKOUT_B,
                startedAtEpochMillis = 3_000L,
                completedAtEpochMillis = 4_000L,
                isCompleted = true
            ),
            sets = emptyList()
        )

        val cloudSnapshot = AppBackupSnapshot(
            exportedAtEpochMillis = 5_000L,
            exerciseStates = AppDatabase.defaultExerciseStates(),
            sessions = listOf(session1, session2),
            bodyweightEntries = emptyList()
        )

        val session3OnNewPhone = WorkoutSessionWithSets(
            session = WorkoutSessionEntity(
                id = 1L,
                workoutType = WorkoutType.WORKOUT_A,
                startedAtEpochMillis = 6_000L,
                completedAtEpochMillis = 7_000L,
                isCompleted = true
            ),
            sets = emptyList()
        )

        val localSnapshot = AppBackupSnapshot(
            exportedAtEpochMillis = 8_000L,
            exerciseStates = AppDatabase.defaultExerciseStates(),
            sessions = listOf(session1, session3OnNewPhone),
            bodyweightEntries = emptyList()
        )

        val merged = BackupExportManager.mergeSnapshots(localSnapshot, cloudSnapshot)
        assertEquals(3, merged.sessions.size)

        val csv = BackupExportManager.toCsv(merged.sessions, WeightUnit.LB)
        assertTrue(csv.startsWith("Date,Workout,Exercise,SetNumber,SetType"))
    }
}
