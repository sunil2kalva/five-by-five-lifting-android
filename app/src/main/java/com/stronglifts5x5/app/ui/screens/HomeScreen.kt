package com.stronglifts5x5.app.ui.screens

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
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.domain.PlateCalculator
import com.stronglifts5x5.app.ui.StrengthUiState
import com.stronglifts5x5.app.ui.components.PlateCalculatorSheet
import com.stronglifts5x5.app.ui.theme.AmberWarning
import com.stronglifts5x5.app.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: StrengthUiState,
    onToggleWorkoutType: () -> Unit,
    onStartOrResumeWorkout: () -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val unit = uiState.preferences.weightUnit
    val barWeightLb = uiState.preferences.barbellWeightLb
    val upcomingType = uiState.activeSession?.session?.workoutType ?: uiState.nextWorkoutType
    var selectedPlateExercise by remember { mutableStateOf<ExerciseType?>(null) }

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
                        text = "StrongLifts 5×5",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "3x/week Linear Progression Strength Tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onNavigateToSettings,
                    label = {
                        val isDriveLinked = !uiState.preferences.cloudSyncUriString.isNullOrBlank()
                        Text(
                            text = if (isDriveLinked) "Drive Auto-Sync ON" else "Android Cloud Backup",
                            color = EmeraldSuccess
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

        // Next Workout Banner Card
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
                        Column {
                            Text(
                                text = if (uiState.activeSession != null) {
                                    "IN PROGRESS • ${upcomingType.title.uppercase()}"
                                } else {
                                    "NEXT UP • ${upcomingType.title.uppercase()}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = upcomingType.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.activeSession == null) {
                            OutlinedButton(onClick = onToggleWorkoutType) {
                                Text("Switch A ↔ B")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartOrResumeWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (uiState.activeSession != null) {
                                "Resume ${upcomingType.title}"
                            } else {
                                "Start ${upcomingType.title}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "${upcomingType.title} Exercises (Tap any lift for Plate Calculator)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(upcomingType.exercises()) { exercise ->
            val state = uiState.exerciseStates[exercise]
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
                            text = exercise.displayName,
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
