package com.fivebyfivelifting.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivebyfivelifting.app.data.local.WorkoutSetEntity
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.data.model.WorkoutType
import com.fivebyfivelifting.app.domain.PlateCalculator
import com.fivebyfivelifting.app.domain.ProgressionAction
import com.fivebyfivelifting.app.ui.StrengthUiState
import com.fivebyfivelifting.app.ui.components.PlateCalculatorSheet
import com.fivebyfivelifting.app.ui.components.RestTimerBanner
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
    onAddThirtySecondsRest: () -> Unit,
    onSwitchRestToThreeMinutes: () -> Unit,
    onSkipRestTimer: () -> Unit,
    onFinishWorkout: (String, String) -> Unit,
    onDiscardWorkout: () -> Unit,
    onDismissSummary: () -> Unit
) {
    val active = uiState.activeSession
    val unit = uiState.preferences.weightUnit
    val barWeightLb = uiState.preferences.barbellWeightLb

    val showWarmupMap = remember { mutableStateMapOf<ExerciseType, Boolean>() }
    var selectedPlateExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var editingWorkoutType by remember { mutableStateOf<WorkoutType?>(null) }
    var bodyweightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

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

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE WORKOUT (PULL DOWN TO CHANGE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkoutPullDownSelector(
                                currentWorkoutType = workoutType,
                                uiState = uiState,
                                onSelectWorkoutType = onSelectWorkoutType,
                                onOpenWorkoutBuilder = { editingWorkoutType = it }
                            )
                            TextButton(onClick = { editingWorkoutType = workoutType }) {
                                Text("+ Edit Lifts")
                            }
                        }
                    }
                    TextButton(onClick = onDiscardWorkout) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Plates: ${loadout.summaryText()} (Tap to adjust)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        selectedPlateExercise = exercise
                                    }
                                )
                            }

                            Text(
                                text = "${workSets.size}×$targetReps ${unit.format(workWeightLb)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.clickable {
                                    selectedPlateExercise = exercise
                                }
                            )
                        }

                        if (warmupSets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !showingWarmups,
                                    onClick = { showWarmupMap[exercise] = false },
                                    label = { Text("Work Sets (${workSets.size}×$targetReps)") }
                                )
                                FilterChip(
                                    selected = showingWarmups,
                                    onClick = { showWarmupMap[exercise] = true },
                                    label = { Text("Warm-up (${warmupSets.size} sets)") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (showingWarmups && warmupSets.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = wuPlates.summaryText(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        SetBubbleButton(
                                            set = warmupSet,
                                            unit = unit,
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
                                            unit = unit,
                                            onTap = { onTapSetBubble(workSet.id) }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Set ${workSet.setNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Optional Bodyweight & Notes card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Bodyweight & Session Notes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = bodyweightInput,
                            onValueChange = { bodyweightInput = it },
                            label = { Text("Today's Bodyweight (${unit.label}) — optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Workout notes (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { onFinishWorkout(bodyweightInput, notesInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Finish ${activeTemplate.customTitle} & Save Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Docked Smart Rest Timer Banner
        RestTimerBanner(
            timerState = uiState.restTimer,
            onAddThirtySeconds = onAddThirtySecondsRest,
            onSwitchToThreeMinutes = onSwitchRestToThreeMinutes,
            onSkip = onSkipRestTimer
        )
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

@Composable
private fun SetBubbleButton(
    set: WorkoutSetEntity,
    unit: WeightUnit,
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
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = reps?.toString() ?: set.targetReps.toString(),
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
