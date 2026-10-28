package com.stronglifts5x5.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stronglifts5x5.app.data.local.AppDatabase
import com.stronglifts5x5.app.data.local.BodyweightEntryEntity
import com.stronglifts5x5.app.data.local.ExerciseStateEntity
import com.stronglifts5x5.app.data.local.WorkoutSessionWithSets
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WeightUnit
import com.stronglifts5x5.app.data.model.WorkoutType
import com.stronglifts5x5.app.data.preferences.UserPreferences
import com.stronglifts5x5.app.data.repository.WorkoutCompletionSummary
import com.stronglifts5x5.app.data.repository.WorkoutRepository
import com.stronglifts5x5.app.domain.BackupExportManager
import com.stronglifts5x5.app.domain.ProgressionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RestTimerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val exerciseName: String = "",
    val completedReps: Int = 5,
    val reasonLabel: String = ""
)

data class StrengthUiState(
    val exerciseStates: Map<ExerciseType, ExerciseStateEntity> = emptyMap(),
    val activeSession: WorkoutSessionWithSets? = null,
    val completedSessions: List<WorkoutSessionWithSets> = emptyList(),
    val bodyweightEntries: List<BodyweightEntryEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val nextWorkoutType: WorkoutType = WorkoutType.WORKOUT_A,
    val restTimer: RestTimerState = RestTimerState(),
    val completionSummary: WorkoutCompletionSummary? = null,
    val statusMessage: String? = null
)

class StrengthViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _restTimer = MutableStateFlow(RestTimerState())
    private val _completionSummary = MutableStateFlow<WorkoutCompletionSummary?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultExerciseStates()
        }
    }

    val uiState: StateFlow<StrengthUiState> = combine(
        repository.exerciseStatesFlow,
        repository.activeSessionFlow,
        repository.completedSessionsFlow,
        repository.bodyweightEntriesFlow,
        repository.preferencesRepository.preferencesFlow
    ) { states, active, completed, bodyweights, prefs ->
        val defaultMap = AppDatabase.defaultExerciseStates().associateBy { it.exercise }
        val mergedStates = defaultMap + states.associateBy { it.exercise }

        val lastCompletedType = completed.firstOrNull()?.session?.workoutType
        val autoNextType = lastCompletedType?.next() ?: WorkoutType.WORKOUT_A
        val nextType = prefs.nextWorkoutOverride ?: autoNextType

        StrengthUiState(
            exerciseStates = mergedStates,
            activeSession = active,
            completedSessions = completed,
            bodyweightEntries = bodyweights,
            preferences = prefs,
            nextWorkoutType = nextType
        )
    }.combine(_restTimer) { base, timer ->
        base.copy(restTimer = timer)
    }.combine(_completionSummary) { base, summary ->
        base.copy(completionSummary = summary)
    }.combine(_statusMessage) { base, msg ->
        base.copy(statusMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrengthUiState(
            exerciseStates = AppDatabase.defaultExerciseStates().associateBy { it.exercise }
        )
    )

    fun toggleNextWorkoutType() {
        val currentNext = uiState.value.nextWorkoutType
        viewModelScope.launch {
            repository.preferencesRepository.setNextWorkoutOverride(currentNext.next())
        }
    }

    fun startOrResumeWorkout(onReady: () -> Unit) {
        val targetType = uiState.value.activeSession?.session?.workoutType
            ?: uiState.value.nextWorkoutType
        viewModelScope.launch {
            repository.startWorkout(targetType)
            onReady()
        }
    }

    fun onTapSetBubble(setId: Long, onVibrateOrSound: () -> Unit = {}) {
        viewModelScope.launch {
            val updatedSet = repository.tapSetBubble(setId) ?: return@launch
            if (updatedSet.isWarmup) return@launch

            val reps = updatedSet.completedReps
            if (reps == null) {
                stopRestTimer()
            } else {
                val seconds = ProgressionEngine.recommendedRestSeconds(
                    completedReps = reps,
                    targetReps = updatedSet.targetReps,
                    feltHeavy = false
                )
                val reason = if (reps >= updatedSet.targetReps) {
                    "Completed ${updatedSet.targetReps} reps — Rest 1:30 (or 3:00 if heavy)"
                } else {
                    "Missed rep ($reps/${updatedSet.targetReps}) — Rest 5:00 to recover"
                }
                startRestTimer(
                    durationSeconds = seconds,
                    exerciseName = updatedSet.exercise.displayName,
                    completedReps = reps,
                    reasonLabel = reason,
                    onTimerFinished = onVibrateOrSound
                )
            }
        }
    }

    fun startRestTimer(
        durationSeconds: Int,
        exerciseName: String,
        completedReps: Int,
        reasonLabel: String,
        onTimerFinished: () -> Unit = {}
    ) {
        timerJob?.cancel()
        if (durationSeconds <= 0) {
            _restTimer.value = RestTimerState()
            return
        }
        _restTimer.value = RestTimerState(
            isRunning = true,
            remainingSeconds = durationSeconds,
            totalSeconds = durationSeconds,
            exerciseName = exerciseName,
            completedReps = completedReps,
            reasonLabel = reasonLabel
        )
        timerJob = viewModelScope.launch {
            while (_restTimer.value.remainingSeconds > 0) {
                delay(1_000L)
                val nextRemaining = (_restTimer.value.remainingSeconds - 1).coerceAtLeast(0)
                _restTimer.value = _restTimer.value.copy(remainingSeconds = nextRemaining)
            }
            _restTimer.value = _restTimer.value.copy(isRunning = false)
            if (uiState.value.preferences.soundAndVibrationEnabled) {
                onTimerFinished()
            }
        }
    }

    fun adjustRestTimerSeconds(deltaSeconds: Int) {
        val current = _restTimer.value
        if (!current.isRunning) return
        val newRemaining = (current.remainingSeconds + deltaSeconds).coerceAtLeast(5)
        val newTotal = maxOf(current.totalSeconds, newRemaining)
        _restTimer.value = current.copy(
            remainingSeconds = newRemaining,
            totalSeconds = newTotal
        )
    }

    fun switchRestTimerToThreeMinutes() {
        val current = _restTimer.value
        if (!current.isRunning) return
        _restTimer.value = current.copy(
            remainingSeconds = 180,
            totalSeconds = 180,
            reasonLabel = "Heavy set — Resting 3:00 before next set"
        )
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimer.value = RestTimerState()
    }

    fun finishActiveWorkout(
        bodyweightDisplayInput: String,
        notes: String,
        onCompleted: () -> Unit
    ) {
        stopRestTimer()
        viewModelScope.launch {
            val unit = uiState.value.preferences.weightUnit
            val parsedBwDisplay = bodyweightDisplayInput.trim().toDoubleOrNull()
            val bwLb = parsedBwDisplay?.let { unit.toLb(it) }
            val summary = repository.completeActiveWorkout(
                bodyweightLb = bwLb,
                notes = notes
            )
            _completionSummary.value = summary
            onCompleted()
        }
    }

    fun discardActiveWorkout(onDiscarded: () -> Unit) {
        stopRestTimer()
        viewModelScope.launch {
            repository.cancelActiveWorkout()
            onDiscarded()
        }
    }

    fun dismissCompletionSummary() {
        _completionSummary.value = null
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteCompletedSession(sessionId)
            _statusMessage.value = "Workout session deleted"
        }
    }

    fun updateWorkingWeight(exercise: ExerciseType, displayWeight: Double) {
        viewModelScope.launch {
            val unit = uiState.value.preferences.weightUnit
            val weightLb = unit.toLb(displayWeight)
            repository.updateExerciseWorkingWeight(exercise, weightLb)
            _statusMessage.value = "Updated ${exercise.displayName} to ${unit.format(weightLb)}"
        }
    }

    fun logStandaloneBodyweight(displayWeight: Double) {
        viewModelScope.launch {
            val unit = uiState.value.preferences.weightUnit
            val weightLb = unit.toLb(displayWeight)
            repository.logStandaloneBodyweight(weightLb)
            _statusMessage.value = "Logged bodyweight: ${unit.format(weightLb)}"
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            repository.preferencesRepository.setWeightUnit(unit)
        }
    }

    fun setBarbellWeightDisplay(displayWeight: Double) {
        viewModelScope.launch {
            val unit = uiState.value.preferences.weightUnit
            repository.preferencesRepository.setBarbellWeightLb(unit.toLb(displayWeight))
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.setAutoSyncEnabled(enabled)
        }
    }

    fun linkGoogleDriveSyncFile(uri: Uri) {
        viewModelScope.launch {
            repository.cloudSyncManager.persistDriveUriPermission(uri)
            repository.preferencesRepository.setCloudSyncUri(uri.toString())
            val syncResult = repository.syncToCloudUri(uri)
            _statusMessage.value = if (syncResult.isSuccess) {
                "Google Drive Auto-Sync linked & backed up!"
            } else {
                "Linked URI, but initial sync failed: ${syncResult.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun triggerManualCloudSync() {
        val uriStr = uiState.value.preferences.cloudSyncUriString ?: return
        viewModelScope.launch {
            val result = repository.syncToCloudUri(Uri.parse(uriStr))
            _statusMessage.value = if (result.isSuccess) {
                "Synced all workouts to Google Drive!"
            } else {
                "Cloud sync failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun restoreOrMergeFromGoogleDriveUri(uri: Uri, mergeWithLocal: Boolean = true) {
        viewModelScope.launch {
            repository.cloudSyncManager.persistDriveUriPermission(uri)
            repository.preferencesRepository.setCloudSyncUri(uri.toString())
            val result = repository.restoreFromCloudUri(uri, mergeWithLocal = mergeWithLocal)
            _statusMessage.value = result.fold(
                onSuccess = { count -> "Restored & merged $count workouts from Google Drive!" },
                onFailure = { err -> "Restore failed: ${err.localizedMessage}" }
            )
        }
    }

    fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch {
            val state = uiState.value
            val csv = BackupExportManager.toCsv(
                sessions = state.completedSessions,
                unit = state.preferences.weightUnit
            )
            val result = repository.cloudSyncManager.writeCsvToUri(uri, csv)
            _statusMessage.value = if (result.isSuccess) {
                "Exported ${state.completedSessions.size} workouts to CSV!"
            } else {
                "CSV export failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StrengthViewModel(repository) as T
        }
    }
}
