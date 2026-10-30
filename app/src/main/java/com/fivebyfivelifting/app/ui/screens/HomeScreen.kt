package com.fivebyfivelifting.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WorkoutType
import com.fivebyfivelifting.app.domain.PlateCalculator
import com.fivebyfivelifting.app.ui.StrengthUiState
import com.fivebyfivelifting.app.ui.components.PlateCalculatorSheet
import com.fivebyfivelifting.app.ui.components.WorkoutBuilderSheet
import com.fivebyfivelifting.app.ui.components.WorkoutPullDownSelector
import com.fivebyfivelifting.app.ui.theme.AmberWarning
import com.fivebyfivelifting.app.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: StrengthUiState,
    onSelectWorkoutType: (WorkoutType) -> Unit,
    onSaveWorkoutTemplate: (WorkoutType, String, List<ExerciseType>, Boolean) -> Unit,
    onSaveCustomExercise: (ExerciseType, String, Int, Int, Double, Double) -> Unit,
    onStartOrResumeWorkout: () -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val unit = uiState.preferences.weightUnit
    val barWeightLb = uiState.preferences.barbellWeightLb
    val upcomingType = uiState.activeSession?.session?.workoutType ?: uiState.nextWorkoutType
    val upcomingTemplate = uiState.templateFor(upcomingType)
    val customNames = remember(uiState.exerciseStates) {
        uiState.exerciseStates.mapValues { it.value.effectiveName() }
    }

    var selectedPlateExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var editingWorkoutType by remember { mutableStateOf<WorkoutType?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Five By Five Lifting",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Custom A / B / C Progression Strength Tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onNavigateToSettings,
                    label = {
                        val prefs = uiState.preferences
                        val labelText = when {
                            prefs.isBackupSetupComplete -> {
                                val shortName = prefs.googleDisplayName?.split(" ")?.firstOrNull()
                                    ?: prefs.googleAccountEmail?.substringBefore("@")
                                    ?: "Signed In"
                                "✓ $shortName • Sync ON"
                            }
                            prefs.isSignedInWithGoogle -> {
                                val shortName = prefs.googleDisplayName?.split(" ")?.firstOrNull()
                                    ?: prefs.googleAccountEmail?.substringBefore("@")
                                    ?: "Signed In"
                                "✓ $shortName"
                            }
                            else -> "Sign in with Google"
                        }
                        Text(
                            text = labelText,
                            color = if (prefs.isSignedInWithGoogle) EmeraldSuccess else AmberWarning
                        )
                    }
                )
            }
        }

        // Summary Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatHighlightCard(
                    title = "Completed",
                    value = "${uiState.completedSessions.size} Workouts",
                    modifier = Modifier.weight(1f)
                )
                val lastSessionDate = uiState.completedSessions.firstOrNull()
                    ?.session?.completedAtEpochMillis?.let {
                        SimpleDateFormat("MMM d", Locale.US).format(Date(it))
                    } ?: "None yet"
                StatHighlightCard(
                    title = "Last Session",
                    value = lastSessionDate,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Next Workout Banner Card with Pull-Down Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                text = if (uiState.activeSession != null) {
                                    "ACTIVE WORKOUT (PULL DOWN TO SWITCH)"
                                } else {
                                    "SELECT WORKOUT (A / B / C...)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = upcomingTemplate.subtitle(customNames),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        WorkoutPullDownSelector(
                            currentWorkoutType = upcomingType,
                            uiState = uiState,
                            onSelectWorkoutType = onSelectWorkoutType,
                            onOpenWorkoutBuilder = { typeToEdit ->
                                editingWorkoutType = typeToEdit
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartOrResumeWorkout,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (uiState.activeSession != null) {
                                    "Resume ${upcomingTemplate.customTitle}"
                                } else {
                                    "Start ${upcomingTemplate.customTitle}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { editingWorkoutType = upcomingType },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Create / Edit")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "${upcomingTemplate.customTitle} Exercises (Tap any lift for Plate Calculator)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(upcomingTemplate.exerciseList()) { exercise ->
            val state = uiState.exerciseStates[exercise]
            val displayName = uiState.exerciseDisplayName(exercise)
            val weightLb = state?.currentWeightLb ?: exercise.defaultStartWeightLb
            val sets = state?.targetSets ?: exercise.defaultSets
            val reps = state?.targetReps ?: exercise.defaultReps
            val failures = state?.consecutiveFailures ?: 0
            val loadout = PlateCalculator.calculatePlates(
                targetWeightLb = weightLb,
                barWeightLb = barWeightLb,
                unit = unit
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPlateExercise = exercise },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plates: ${loadout.summaryText()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (failures > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Failure Strike $failures/3 — 3rd miss triggers 10% deload",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${sets}×${reps} • ${unit.format(weightLb)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    selectedPlateExercise?.let { exercise ->
        val state = uiState.exerciseStates[exercise]
        val initialWeightLb = state?.currentWeightLb ?: exercise.defaultStartWeightLb
        PlateCalculatorSheet(
            exercise = exercise,
            initialWeightLb = initialWeightLb,
            barWeightLb = barWeightLb,
            unit = unit,
            onDismiss = { selectedPlateExercise = null },
            onSaveWeightDisplay = { newDisplayWeight ->
                onUpdateExerciseWeight(exercise, newDisplayWeight)
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
}

@Composable
private fun StatHighlightCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
