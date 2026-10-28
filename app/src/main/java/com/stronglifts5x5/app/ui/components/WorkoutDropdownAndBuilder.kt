package com.stronglifts5x5.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WorkoutType
import com.stronglifts5x5.app.ui.StrengthUiState

@Composable
fun WorkoutPullDownSelector(
    currentWorkoutType: WorkoutType,
    uiState: StrengthUiState,
    onSelectWorkoutType: (WorkoutType) -> Unit,
    onOpenWorkoutBuilder: (WorkoutType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentTemplate = uiState.templateFor(currentWorkoutType)
    val customNames = remember(uiState.exerciseStates) {
        uiState.exerciseStates.mapValues { it.value.effectiveName() }
    }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = currentTemplate.customTitle,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Workout"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val enabledTemplates = uiState.enabledTemplates()
            enabledTemplates.forEach { template ->
                val isSelected = template.workoutType == currentWorkoutType
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = template.customTitle,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                            Text(
                                text = template.subtitle(customNames),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectWorkoutType(template.workoutType)
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Edit ${currentTemplate.customTitle} Exercises...",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Workout")
                },
                onClick = {
                    expanded = false
                    onOpenWorkoutBuilder(currentWorkoutType)
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ Create / Manage Workouts (A–E)...",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Workout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    // Default to the first un-enabled template (e.g. D or E), or C
                    val nextUnused = WorkoutType.entries.firstOrNull {
                        !(uiState.workoutTemplates[it]?.isEnabled ?: false)
                    } ?: WorkoutType.WORKOUT_C
                    onOpenWorkoutBuilder(nextUnused)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderSheet(
    initialWorkoutType: WorkoutType,
    uiState: StrengthUiState,
    onDismiss: () -> Unit,
    onSaveTemplate: (WorkoutType, String, List<ExerciseType>, Boolean) -> Unit,
    onSaveCustomExercise: (ExerciseType, String, Int, Int, Double, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialWorkoutType) }
    val template = uiState.templateFor(selectedType)

    var titleInput by remember(selectedType, template.customTitle) {
        mutableStateOf(template.customTitle)
    }
    var isEnabledInput by remember(selectedType, template.isEnabled) {
        mutableStateOf(true)
    }
    val selectedExercises = remember(selectedType, template.exercisesCsv) {
        mutableStateListOf<ExerciseType>().apply {
            addAll(template.exerciseList())
        }
    }

    var configuringExercise by remember { mutableStateOf<ExerciseType?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create / Customize Workouts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Select a Workout Slot (A, B, C, D, or E):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text("Workout ${type.code}") }
                    )
                }
            }

            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Workout Name (e.g., Workout ${selectedType.code} - Upper)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedType != WorkoutType.WORKOUT_A && selectedType != WorkoutType.WORKOUT_B) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show in Pull-Down Workout List",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isEnabledInput,
                        onCheckedChange = { isEnabledInput = it }
                    )
                }
            }

            Text(
                text = "Select Exercises in ${titleInput.ifBlank { selectedType.title }} (Tap 'Edit' to change Sets × Reps or rename):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExerciseType.entries.forEach { exercise ->
                val isChecked = exercise in selectedExercises
                val exState = uiState.exerciseStates[exercise]
                val sets = exState?.targetSets ?: exercise.defaultSets
                val reps = exState?.targetReps ?: exercise.defaultReps
                val weightLb = exState?.currentWeightLb ?: exercise.defaultStartWeightLb
                val displayName = uiState.exerciseDisplayName(exercise)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (exercise !in selectedExercises) {
                                        selectedExercises.add(exercise)
                                    }
                                } else if (selectedExercises.size > 1) {
                                    selectedExercises.remove(exercise)
                                }
                            }
                        )
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${sets}×${reps} @ ${uiState.preferences.weightUnit.format(weightLb)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(onClick = { configuringExercise = exercise }) {
                        Text("Edit Lift")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    onSaveTemplate(
                        selectedType,
                        titleInput.ifBlank { selectedType.title },
                        selectedExercises.toList(),
                        isEnabledInput
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Save ${titleInput.ifBlank { selectedType.title }} & Use Now",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    configuringExercise?.let { exercise ->
        val exState = uiState.exerciseStates[exercise]
        val unit = uiState.preferences.weightUnit
        var nameField by remember(exercise) {
            mutableStateOf(exState?.effectiveName() ?: exercise.displayName)
        }
        var setsField by remember(exercise) {
            mutableStateOf((exState?.targetSets ?: exercise.defaultSets).toString())
        }
        var repsField by remember(exercise) {
            mutableStateOf((exState?.targetReps ?: exercise.defaultReps).toString())
        }
        var weightField by remember(exercise) {
            mutableStateOf(unit.fromLb(exState?.currentWeightLb ?: exercise.defaultStartWeightLb).toString())
        }
        var incField by remember(exercise) {
            mutableStateOf(unit.fromLb(exState?.incrementLb ?: exercise.defaultIncrementLb).toString())
        }

        AlertDialog(
            onDismissRequest = { configuringExercise = null },
            title = { Text("Configure Exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("Exercise Name") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = setsField,
                            onValueChange = { setsField = it },
                            label = { Text("Sets (e.g. 5)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsField,
                            onValueChange = { repsField = it },
                            label = { Text("Reps (e.g. 5)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weightField,
                            onValueChange = { weightField = it },
                            label = { Text("Weight (${unit.label})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = incField,
                            onValueChange = { incField = it },
                            label = { Text("+Step (${unit.label})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomExercise(
                            exercise,
                            nameField,
                            setsField.toIntOrNull() ?: 5,
                            repsField.toIntOrNull() ?: 5,
                            weightField.toDoubleOrNull() ?: 45.0,
                            incField.toDoubleOrNull() ?: 5.0
                        )
                        if (exercise !in selectedExercises) {
                            selectedExercises.add(exercise)
                        }
                        configuringExercise = null
                    }
                ) {
                    Text("Save Lift")
                }
            },
            dismissButton = {
                TextButton(onClick = { configuringExercise = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
