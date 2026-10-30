package com.fivebyfivelifting.app.data.repository

import android.net.Uri
import com.fivebyfivelifting.app.data.local.AppDatabase
import com.fivebyfivelifting.app.data.local.BodyweightEntryEntity
import com.fivebyfivelifting.app.data.local.ExerciseStateEntity
import com.fivebyfivelifting.app.data.local.WorkoutDao
import com.fivebyfivelifting.app.data.local.WorkoutSessionEntity
import com.fivebyfivelifting.app.data.local.WorkoutSessionWithSets
import com.fivebyfivelifting.app.data.local.WorkoutSetEntity
import com.fivebyfivelifting.app.data.local.WorkoutTemplateEntity
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WorkoutType
import com.fivebyfivelifting.app.data.preferences.UserPreferencesRepository
import com.fivebyfivelifting.app.domain.AppBackupSnapshot
import com.fivebyfivelifting.app.domain.BackupExportManager
import com.fivebyfivelifting.app.domain.CloudSyncManager
import com.fivebyfivelifting.app.domain.ExerciseProgressionResult
import com.fivebyfivelifting.app.domain.HealthConnectManager
import com.fivebyfivelifting.app.domain.ProgressionEngine
import com.fivebyfivelifting.app.domain.WarmupCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class WorkoutCompletionSummary(
    val workoutType: WorkoutType,
    val workoutTitle: String,
    val progressionResults: List<ExerciseProgressionResult>,
    val totalVolumeLb: Double,
    val cloudSynced: Boolean,
    val cloudSyncError: String? = null,
    val healthConnectSynced: Boolean = false,
    val healthConnectError: String? = null
)

class WorkoutRepository(
    private val dao: WorkoutDao,
    val preferencesRepository: UserPreferencesRepository,
    val cloudSyncManager: CloudSyncManager
) {

    val workoutTemplatesFlow: Flow<List<WorkoutTemplateEntity>> = dao.observeWorkoutTemplates()
    val exerciseStatesFlow: Flow<List<ExerciseStateEntity>> = dao.observeExerciseStates()
    val activeSessionFlow: Flow<WorkoutSessionWithSets?> = dao.observeActiveSession()
    val completedSessionsFlow: Flow<List<WorkoutSessionWithSets>> = dao.observeCompletedSessions()
    val bodyweightEntriesFlow: Flow<List<BodyweightEntryEntity>> = dao.observeBodyweightEntries()

    suspend fun ensureDefaultExerciseStates() {
        val currentStates = dao.getExerciseStates().associateBy { it.exercise }
        val missingStates = AppDatabase.defaultExerciseStates().filter { it.exercise !in currentStates }
        if (missingStates.isNotEmpty()) {
            dao.upsertExerciseStates(missingStates)
        }

        val currentTemplates = dao.getWorkoutTemplates().associateBy { it.workoutType }
        val missingTemplates = AppDatabase.defaultWorkoutTemplates().filter { it.workoutType !in currentTemplates }
        if (missingTemplates.isNotEmpty()) {
            dao.upsertWorkoutTemplates(missingTemplates)
        }
    }

    suspend fun getTemplateExercises(workoutType: WorkoutType): List<ExerciseType> {
        val template = dao.getWorkoutTemplate(workoutType)
        return template?.exerciseList() ?: workoutType.exercises()
    }

    suspend fun startWorkout(workoutType: WorkoutType): Long {
        ensureDefaultExerciseStates()
        val existingActive = dao.getActiveSession()
        if (existingActive != null) {
            if (existingActive.session.workoutType != workoutType) {
                switchActiveWorkoutType(workoutType)
            }
            return existingActive.session.id
        }

        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                workoutType = workoutType,
                startedAtEpochMillis = System.currentTimeMillis(),
                isCompleted = false
            )
        )

        populateSetsForSession(sessionId, workoutType)
        return sessionId
    }

    /**
     * Switches an already-active workout session from e.g. Workout A to Workout B or Workout C
     * via the pull-down selector, or updates the upcoming workout if no session is active yet.
     */
    suspend fun switchActiveWorkoutType(newWorkoutType: WorkoutType) {
        ensureDefaultExerciseStates()
        preferencesRepository.setNextWorkoutOverride(newWorkoutType)
        val existingActive = dao.getActiveSession() ?: return

        dao.deleteSetsForSession(existingActive.session.id)
        dao.updateSession(existingActive.session.copy(workoutType = newWorkoutType))
        populateSetsForSession(existingActive.session.id, newWorkoutType)
    }

    /**
     * Saves or updates a Workout Template (Workout A, B, C, D, E) with a custom title
     * and custom list of exercises, and refreshes the active workout if it matches this type.
     */
    suspend fun saveWorkoutTemplate(
        workoutType: WorkoutType,
        customTitle: String,
        exercises: List<ExerciseType>,
        isEnabled: Boolean = true
    ) {
        ensureDefaultExerciseStates()
        val cleanExercises = exercises.distinct().ifEmpty { listOf(ExerciseType.SQUAT) }
        val entity = WorkoutTemplateEntity(
            workoutType = workoutType,
            customTitle = customTitle.trim().ifEmpty { workoutType.title },
            exercisesCsv = cleanExercises.joinToString(",") { it.name },
            isEnabled = isEnabled
        )
        dao.upsertWorkoutTemplate(entity)

        // If the currently active workout is using this template, rebuild its sets to match
        val active = dao.getActiveSession()
        if (active != null && active.session.workoutType == workoutType) {
            dao.deleteSetsForSession(active.session.id)
            populateSetsForSession(active.session.id, workoutType)
        }
    }

    suspend fun updateExerciseConfig(
        exercise: ExerciseType,
        customName: String,
        targetSets: Int,
        targetReps: Int,
        workingWeightLb: Double,
        incrementLb: Double
    ) {
        ensureDefaultExerciseStates()
        val current = dao.getExerciseState(exercise)
        val updated = (current ?: ExerciseStateEntity(
            exercise = exercise,
            currentWeightLb = workingWeightLb,
            incrementLb = incrementLb,
            targetSets = targetSets,
            targetReps = targetReps
        )).copy(
            customName = customName.trim(),
            targetSets = targetSets.coerceIn(1, 10),
            targetReps = targetReps.coerceIn(1, 30),
            currentWeightLb = workingWeightLb.coerceAtLeast(5.0),
            incrementLb = incrementLb.coerceAtLeast(0.0)
        )
        dao.upsertExerciseState(updated)
    }

    private suspend fun populateSetsForSession(sessionId: Long, workoutType: WorkoutType) {
        val prefs = preferencesRepository.preferencesFlow.first()
        val statesByExercise = dao.getExerciseStates().associateBy { it.exercise }
        val exercises = getTemplateExercises(workoutType)

        val allSets = mutableListOf<WorkoutSetEntity>()
        for (exercise in exercises) {
            val state = statesByExercise[exercise] ?: ExerciseStateEntity(
                exercise = exercise,
                currentWeightLb = exercise.defaultStartWeightLb,
                incrementLb = exercise.defaultIncrementLb,
                targetSets = exercise.defaultSets,
                targetReps = exercise.defaultReps
            )

            // 1. Generate progressive warm-up sets
            val warmups = WarmupCalculator.generateWarmupSets(
                exercise = exercise,
                workingWeightLb = state.currentWeightLb,
                barWeightLb = prefs.barbellWeightLb
            )
            for (warmup in warmups) {
                allSets.add(
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exercise = exercise,
                        setNumber = warmup.setNumber,
                        isWarmup = true,
                        targetWeightLb = warmup.weightLb,
                        targetReps = warmup.targetReps,
                        completedReps = null
                    )
                )
            }

            // 2. Generate working sets
            for (setIndex in 1..state.targetSets) {
                allSets.add(
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exercise = exercise,
                        setNumber = setIndex,
                        isWarmup = false,
                        targetWeightLb = state.currentWeightLb,
                        targetReps = state.targetReps,
                        completedReps = null
                    )
                )
            }
        }
        dao.insertSets(allSets)
    }

    suspend fun tapSetBubble(setId: Long): WorkoutSetEntity? {
        val existing = dao.getSetById(setId) ?: return null
        val nextReps = ProgressionEngine.nextRepState(
            currentReps = existing.completedReps,
            targetReps = existing.targetReps
        )
        val updated = existing.copy(completedReps = nextReps)
        dao.updateSet(updated)
        return updated
    }

    suspend fun cancelActiveWorkout() {
        val active = dao.getActiveSession() ?: return
        dao.deleteSession(active.session.id)
    }

    suspend fun deleteCompletedSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    suspend fun updateExerciseWorkingWeight(exercise: ExerciseType, newWeightLb: Double) {
        ensureDefaultExerciseStates()
        val current = dao.getExerciseState(exercise) ?: return
        val clamped = newWeightLb.coerceAtLeast(5.0)
        dao.upsertExerciseState(
            current.copy(
                currentWeightLb = clamped,
                consecutiveFailures = 0
            )
        )

        val active = dao.getActiveSession() ?: return
        val workingSets = active.workingSetsFor(exercise)
        for (set in workingSets) {
            dao.updateSet(set.copy(targetWeightLb = clamped))
        }
    }

    suspend fun logStandaloneBodyweight(weightLb: Double) {
        dao.insertBodyweightEntry(
            BodyweightEntryEntity(
                recordedAtEpochMillis = System.currentTimeMillis(),
                weightLb = weightLb,
                sessionId = null
            )
        )
    }

    suspend fun completeActiveWorkout(
        bodyweightLb: Double?,
        notes: String
    ): WorkoutCompletionSummary? {
        val active = dao.getActiveSession() ?: return null
        val prefs = preferencesRepository.preferencesFlow.first()
        val statesMap = dao.getExerciseStates().associateBy { it.exercise }
        val template = dao.getWorkoutTemplate(active.session.workoutType)
        val exercisesInSession = active.distinctWorkingExercises()
            .ifEmpty { template?.exerciseList() ?: active.session.workoutType.exercises() }

        val results = mutableListOf<ExerciseProgressionResult>()
        val updatedEntities = mutableListOf<ExerciseStateEntity>()

        for (exercise in exercisesInSession) {
            val state = statesMap[exercise] ?: continue
            val workingSets = active.workingSetsFor(exercise)
            val outcome = ProgressionEngine.evaluateExerciseSession(
                state = state,
                workingSets = workingSets,
                minBarWeightLb = prefs.barbellWeightLb
            )
            results.add(outcome)
            updatedEntities.add(outcome.updatedEntity)
        }

        dao.upsertExerciseStates(updatedEntities)

        val now = System.currentTimeMillis()
        val completedSession = active.session.copy(
            completedAtEpochMillis = now,
            bodyweightLb = bodyweightLb,
            notes = notes.trim(),
            isCompleted = true
        )
        dao.updateSession(completedSession)

        if (bodyweightLb != null && bodyweightLb > 0.0) {
            dao.insertBodyweightEntry(
                BodyweightEntryEntity(
                    recordedAtEpochMillis = now,
                    weightLb = bodyweightLb,
                    sessionId = active.session.id
                )
            )
        }

        preferencesRepository.setNextWorkoutOverride(null)

        var cloudSynced = false
        var cloudError: String? = null
        val driveUriStr = prefs.cloudSyncUriString
        if (prefs.autoSyncEnabled && prefs.isBackupFileConfigured) {
            if (!driveUriStr.isNullOrBlank()) {
                val syncResult = syncToCloudUri(Uri.parse(driveUriStr))
                cloudSynced = syncResult.isSuccess
                cloudError = syncResult.exceptionOrNull()?.localizedMessage
            } else {
                val snapshot = buildBackupSnapshot()
                runCatching { cloudSyncManager.writeLocalBackupMirror(snapshot) }
                    .onSuccess {
                        preferencesRepository.recordCloudSyncSuccess(snapshot.exportedAtEpochMillis)
                        cloudSynced = true
                    }
                    .onFailure { err ->
                        cloudError = err.localizedMessage
                    }
            }
        }

        val workoutTitle = template?.customTitle ?: active.session.workoutType.title
        val totalVolumeLb = active.totalVolumeLb()
        var healthConnectSynced = false
        var healthConnectError: String? = null

        if (prefs.healthConnectSyncEnabled) {
            val unit = prefs.weightUnit
            val exerciseSummaryLines = exercisesInSession.map { ex ->
                val exName = statesMap[ex]?.effectiveName() ?: ex.displayName
                val wSets = active.workingSetsFor(ex)
                val weightStr = wSets.firstOrNull()?.let { unit.format(it.targetWeightLb) } ?: ""
                val repsStr = wSets.joinToString("/") { (it.completedReps ?: 0).toString() }
                "$exName: $weightStr ($repsStr)"
            }
            val hcNotes = buildString {
                append(exerciseSummaryLines.joinToString(" • "))
                append(" | Volume: ${unit.format(totalVolumeLb)}")
            }
            val hcResult = HealthConnectManager.writeCompletedWorkoutSession(
                context = cloudSyncManager.appContext,
                workoutTitle = workoutTitle,
                startEpochMillis = active.session.startedAtEpochMillis,
                endEpochMillis = now,
                summaryNotes = hcNotes
            )
            healthConnectSynced = hcResult.isSuccess
            healthConnectError = hcResult.exceptionOrNull()?.localizedMessage
        }

        return WorkoutCompletionSummary(
            workoutType = active.session.workoutType,
            workoutTitle = workoutTitle,
            progressionResults = results,
            totalVolumeLb = totalVolumeLb,
            cloudSynced = cloudSynced,
            cloudSyncError = cloudError,
            healthConnectSynced = healthConnectSynced,
            healthConnectError = healthConnectError
        )
    }

    suspend fun buildBackupSnapshot(): AppBackupSnapshot {
        ensureDefaultExerciseStates()
        val prefs = preferencesRepository.preferencesFlow.first()
        return AppBackupSnapshot(
            schemaVersion = 1,
            exportedAtEpochMillis = System.currentTimeMillis(),
            weightUnit = prefs.weightUnit,
            barbellWeightLb = prefs.barbellWeightLb,
            exerciseStates = dao.getExerciseStates(),
            sessions = dao.getAllSessionsWithSets().filter { it.session.isCompleted },
            bodyweightEntries = dao.getBodyweightEntries()
        )
    }

    suspend fun syncToCloudUri(uri: Uri): Result<Unit> {
        val snapshot = buildBackupSnapshot()
        val writeResult = cloudSyncManager.writeSnapshotToUri(uri, snapshot)
        if (writeResult.isSuccess) {
            preferencesRepository.recordCloudSyncSuccess(snapshot.exportedAtEpochMillis)
        }
        return writeResult
    }

    suspend fun restoreFromCloudUri(uri: Uri, mergeWithLocal: Boolean = true): Result<Int> {
        val readResult = cloudSyncManager.readSnapshotFromUri(uri)
        if (readResult.isFailure) {
            return Result.failure(readResult.exceptionOrNull() ?: Exception("Failed to read backup"))
        }
        val cloudSnapshot = readResult.getOrThrow()
        val finalSnapshot = if (mergeWithLocal) {
            val localSnapshot = buildBackupSnapshot()
            BackupExportManager.mergeSnapshots(localSnapshot, cloudSnapshot)
        } else {
            cloudSnapshot
        }
        applySnapshotToDatabase(finalSnapshot)
        preferencesRepository.recordCloudSyncSuccess(System.currentTimeMillis())
        return Result.success(finalSnapshot.sessions.size)
    }

    private suspend fun applySnapshotToDatabase(snapshot: AppBackupSnapshot) {
        dao.clearAllSets()
        dao.clearAllSessions()
        dao.clearAllBodyweightEntries()

        if (snapshot.exerciseStates.isNotEmpty()) {
            dao.upsertExerciseStates(snapshot.exerciseStates)
        } else {
            dao.upsertExerciseStates(AppDatabase.defaultExerciseStates())
        }

        for (sessionWithSets in snapshot.sessions) {
            val newSessionId = dao.insertSession(sessionWithSets.session.copy(id = 0))
            val remappedSets = sessionWithSets.sets.map {
                it.copy(id = 0, sessionId = newSessionId)
            }
            dao.insertSets(remappedSets)
        }

        val remappedBw = snapshot.bodyweightEntries.map {
            it.copy(id = 0, sessionId = null)
        }
        dao.insertBodyweightEntries(remappedBw)
    }
}
