package com.fivebyfivelifting.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivebyfivelifting.app.data.local.WorkoutSetEntity
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.data.model.WorkoutType
import com.fivebyfivelifting.app.domain.PlateCalculator
import com.fivebyfivelifting.app.domain.ProgressionAction
import com.fivebyfivelifting.app.ui.RestTimerState
import com.fivebyfivelifting.app.ui.StrengthUiState
import com.fivebyfivelifting.app.ui.components.PlateCalculatorSheet
import com.fivebyfivelifting.app.ui.components.WorkoutBuilderSheet
import com.fivebyfivelifting.app.ui.components.WorkoutPullDownSelector
import com.fivebyfivelifting.app.ui.theme.AmberWarning
import com.fivebyfivelifting.app.ui.theme.EmeraldSuccess

@Composable
fun WorkoutScreen(
    uiState: StrengthUiState,
    onSelectWorkoutType: (WorkoutType) -> Unit,
    onSaveWorkoutTemplate: (WorkoutType, String, List<ExerciseType>, Boolean) -> Unit,
    onSaveCustomExercise: (ExerciseType, String, Int, Int, Double, Double) -> Unit,
    onStartWorkout: () -> Unit,
    onTapSetBubble: (Long) -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onSkipRestTimer: () -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onDismissSummary: () -> Unit
) {
    val active = uiState.activeSession
    val unit = uiState.preferences.weightUnit
    val barWeightLb = uiState.preferences.barbellWeightLb

    val showWarmupMap = remember { mutableStateMapOf<ExerciseType, Boolean>() }
    var selectedPlateExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var editingWorkoutType by remember { mutableStateOf<WorkoutType?>(null) }

    // Post-workout progression summary dialog
    uiState.completionSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = onDismissSummary,
            title = {
                Text(
                    text = "${summary.workoutTitle} Complete!",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total Volume: ${unit.format(summary.totalVolumeLb)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    summary.progressionResults.forEach { res ->
                        val name = uiState.exerciseDisplayName(res.exercise)
                        val statusLine = when (res.action) {
                            ProgressionAction.INCREMENTED ->
                                "$name: ${unit.format(res.previousWeightLb)} → ${unit.format(res.nextWeightLb)} (Progress!)"
                            ProgressionAction.REPEATED_AFTER_FAILURE ->
                                "$name: Repeat ${unit.format(res.nextWeightLb)} next time (Strike ${res.nextConsecutiveFailures}/3)"
                            ProgressionAction.DELOADED_10_PERCENT ->
                                "$name: 10% Deload → ${unit.format(res.nextWeightLb)} (3 strikes reached)"
                        }
                        Text(
                            text = "• $statusLine",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (summary.cloudSynced) {
                            "✓ Automatically backed up to your Google Drive"
                        } else {
                            "✓ Saved locally (Android Cloud Backup enabled)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess
                    )
                    if (summary.healthConnectSynced) {
                        Text(
                            text = "✓ Synced workout to Android Health Connect",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                    } else if (!summary.healthConnectError.isNullOrBlank()) {
                        Text(
                            text = "Health Connect: ${summary.healthConnectError}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissSummary) {
                    Text("Awesome!")
                }
            }
        )
    }

    editingWorkoutType?.let { typeToEdit ->
        WorkoutBuilderSheet(
            initialWorkoutType = typeToEdit,
            uiState = uiState,
            onDismiss = { editingWorkoutType = null },
            onSaveTemplate = { type, title, exercises, enabled ->
                onSaveWorkoutTemplate(type, title, exercises, enabled)
            },
            onSaveCustomExercise = onSaveCustomExercise
        )
    }

    if (active == null) {
        val nextTemplate = uiState.templateFor(uiState.nextWorkoutType)
        val customNames = remember(uiState.exerciseStates) {
            uiState.exerciseStates.mapValues { it.value.effectiveName() }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Workout to Start:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            WorkoutPullDownSelector(
                currentWorkoutType = uiState.nextWorkoutType,
                uiState = uiState,
                onSelectWorkoutType = onSelectWorkoutType,
                onOpenWorkoutBuilder = { editingWorkoutType = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = nextTemplate.subtitle(customNames),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onStartWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Start ${nextTemplate.customTitle} Now")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { editingWorkoutType = uiState.nextWorkoutType },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create / Customize Workouts (A, B, C...)")
            }
        }
        return
    }

    val workoutType = active.session.workoutType
    val activeTemplate = uiState.templateFor(workoutType)
    val exercisesInWorkout = active.distinctWorkingExercises()
        .ifEmpty { activeTemplate.exerciseList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 1. Compact Top Header Row + Innovative Inline HUD Rest Timer Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                WorkoutPullDownSelector(
                    currentWorkoutType = workoutType,
                    uiState = uiState,
                    onSelectWorkoutType = onSelectWorkoutType,
                    onOpenWorkoutBuilder = { editingWorkoutType = it }
                )
                TextButton(
                    onClick = { editingWorkoutType = workoutType }
                ) {
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Innovative Zero-Clutter HUD Rest Timer Pill integrated right into the header bar
            AnimatedVisibility(
                visible = uiState.restTimer.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                InlineHeaderRestTimerPill(
                    timerState = uiState.restTimer,
                    onDismiss = onSkipRestTimer
                )
            }

            TextButton(onClick = onDiscardWorkout) {
                Text(
                    text = "Discard",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Subtle 3dp full-width progress bar directly beneath the header when timer is active
        AnimatedVisibility(
            visible = uiState.restTimer.isRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val progress = if (uiState.restTimer.totalSeconds > 0) {
                (uiState.restTimer.remainingSeconds.toFloat() / uiState.restTimer.totalSeconds.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // 2. Compact Exercise Cards List (fits 3 exercises on one screen without scrolling)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exercisesInWorkout) { exercise ->
                val displayName = uiState.exerciseDisplayName(exercise)
                val workSets = active.workingSetsFor(exercise)
                val warmupSets = active.warmupSetsFor(exercise)
                val targetReps = workSets.firstOrNull()?.targetReps
                    ?: uiState.exerciseStates[exercise]?.targetReps
                    ?: exercise.defaultReps
                val workWeightLb = workSets.firstOrNull()?.targetWeightLb
                    ?: uiState.exerciseStates[exercise]?.currentWeightLb
                    ?: exercise.defaultStartWeightLb
                val showingWarmups = showWarmupMap[exercise] ?: false

                val loadout = PlateCalculator.calculatePlates(
                    targetWeightLb = workWeightLb,
                    barWeightLb = barWeightLb,
                    unit = unit
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Compact Exercise Header: Name + Working Weight + Inline Warm-up Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${workSets.size}×$targetReps ${unit.format(workWeightLb)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            selectedPlateExercise = exercise
                                        }
                                    )
                                }
                                Text(
                                    text = "Plates: ${loadout.summaryText()} (Tap to adjust)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        selectedPlateExercise = exercise
                                    }
                                )
                            }

                            if (warmupSets.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (showingWarmups) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.clickable {
                                        showWarmupMap[exercise] = !showingWarmups
                                    }
                                ) {
                                    Text(
                                        text = if (showingWarmups) "← Work Sets" else "Warm-up (${warmupSets.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (showingWarmups) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showingWarmups && warmupSets.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                warmupSets.forEach { warmupSet ->
                                    val wuPlates = PlateCalculator.calculatePlates(
                                        targetWeightLb = warmupSet.targetWeightLb,
                                        barWeightLb = barWeightLb,
                                        unit = unit
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Warm-up #${warmupSet.setNumber}: 1×${warmupSet.targetReps} @ ${unit.format(warmupSet.targetWeightLb)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = wuPlates.summaryText(),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        SetBubbleButton(
                                            set = warmupSet,
                                            onTap = { onTapSetBubble(warmupSet.id) }
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                workSets.forEach { workSet ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        SetBubbleButton(
                                            set = workSet,
                                            onTap = { onTapSetBubble(workSet.id) }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "S${workSet.setNumber}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Docked Bottom Finish Button ("Finish Workout B" pattern)
        Button(
            onClick = onFinishWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Finish ${activeTemplate.customTitle}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    selectedPlateExercise?.let { exercise ->
        val currentWeightLb = active.workingSetsFor(exercise).firstOrNull()?.targetWeightLb
            ?: uiState.exerciseStates[exercise]?.currentWeightLb
            ?: exercise.defaultStartWeightLb
        PlateCalculatorSheet(
            exercise = exercise,
            initialWeightLb = currentWeightLb,
            barWeightLb = barWeightLb,
            unit = unit,
            onDismiss = { selectedPlateExercise = null },
            onSaveWeightDisplay = { newDisplay ->
                onUpdateExerciseWeight(exercise, newDisplay)
            }
        )
    }
}

/**
 * Innovative, ultra-compact HUD Rest Timer Pill displayed inline in the Workout top bar.
 * Shows an animated circular countdown ring + MM:SS timer + tap-to-dismiss (×) with zero preset buttons.
 */
@Composable
private fun InlineHeaderRestTimerPill(
    timerState: RestTimerState,
    onDismiss: () -> Unit
) {
    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val formatted = String.format("%02d:%02d", mins, secs)
    val progress = if (timerState.totalSeconds > 0) {
        (timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onDismiss)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    strokeWidth = 2.2.dp,
                    strokeCap = StrokeCap.Round
                )
            }
            Text(
                text = formatted,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "✕",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetBubbleButton(
    set: WorkoutSetEntity,
    onTap: () -> Unit
) {
    val reps = set.completedReps
    val bgColor = when {
        reps == null -> Color.Transparent
        reps >= set.targetReps -> MaterialTheme.colorScheme.primary
        else -> AmberWarning
    }
    val borderColor = when {
        reps == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        reps >= set.targetReps -> MaterialTheme.colorScheme.primary
        else -> AmberWarning
    }
    val textColor = when (reps) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = reps?.toString() ?: set.targetReps.toString(),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
