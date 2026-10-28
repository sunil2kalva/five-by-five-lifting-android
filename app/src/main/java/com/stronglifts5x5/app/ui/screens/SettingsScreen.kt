package com.stronglifts5x5.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.stronglifts5x5.app.data.model.WeightUnit
import com.stronglifts5x5.app.ui.StrengthUiState
import com.stronglifts5x5.app.ui.components.PlateCalculatorSheet
import com.stronglifts5x5.app.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: StrengthUiState,
    onSelectWeightUnit: (WeightUnit) -> Unit,
    onSelectBarbellWeightDisplay: (Double) -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onLinkGoogleDriveSyncFile: (Uri) -> Unit,
    onTriggerManualCloudSync: () -> Unit,
    onRestoreFromGoogleDriveUri: (Uri) -> Unit,
    onExportCsvToUri: (Uri) -> Unit
) {
    val prefs = uiState.preferences
    val unit = prefs.weightUnit
    var editingExercise by remember { mutableStateOf<ExerciseType?>(null) }

    val linkDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onLinkGoogleDriveSyncFile(uri)
        }
    }

    val restoreDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onRestoreFromGoogleDriveUri(uri)
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportCsvToUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings & Cloud Sync",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // 1. Cloud Backup & Google Drive Auto-Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Cloud Backup & New Phone Restore ($0 Cost)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "✓ Layer 1: Android OS Auto-Backup is active for your Google Account.\n" +
                        "✓ Layer 2: Link a file in your Google Drive below so every finished workout auto-syncs to your cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val lastSyncStr = prefs.lastCloudSyncEpochMillis?.let {
                    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(it))
                } ?: "Not synced yet"

                Text(
                    text = "Last Google Drive Sync: $lastSyncStr",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldSuccess
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-sync after every completed workout",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = prefs.autoSyncEnabled,
                        onCheckedChange = onToggleAutoSync
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            linkDriveLauncher.launch("stronglifts5x5_cloud_sync.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (prefs.cloudSyncUriString.isNullOrBlank()) {
                                "Link Google Drive File"
                            } else {
                                "Change Drive File"
                            }
                        )
                    }

                    if (!prefs.cloudSyncUriString.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onTriggerManualCloudSync,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Now")
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        restoreDriveLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore / Merge Workouts from Google Drive")
                }
            }
        }

        // 2. Weight Unit & Barbell Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Weight Units & Barbell",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeightUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { onSelectWeightUnit(u) },
                            label = {
                                Text(
                                    if (u == WeightUnit.LB) "Imperial (lb)" else "Metric (kg)"
                                )
                            }
                        )
                    }
                }

                val barOptions = if (unit == WeightUnit.LB) {
                    listOf(45.0, 35.0)
                } else {
                    listOf(20.0, 15.0)
                }

                Text(
                    text = "Standard Olympic Barbell Weight",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    barOptions.forEach { optionDisplay ->
                        val currentBarDisplay = unit.fromLb(prefs.barbellWeightLb)
                        FilterChip(
                            selected = currentBarDisplay == optionDisplay,
                            onClick = { onSelectBarbellWeightDisplay(optionDisplay) },
                            label = { Text("${optionDisplay.toInt()} ${unit.label}") }
                        )
                    }
                }
            }
        }

        // 3. Edit Current Working Weights
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current Working Weights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ExerciseType.entries.forEach { exercise ->
                    val weightLb = uiState.exerciseStates[exercise]?.currentWeightLb
                        ?: exercise.defaultStartWeightLb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exercise.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(onClick = { editingExercise = exercise }) {
                            Text(unit.format(weightLb))
                        }
                    }
                }
            }
        }

        // 4. Spreadsheet Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Spreadsheet Export (CSV)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export all completed sets, weights, and bodyweights to a CSV file for Excel or Google Sheets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { exportCsvLauncher.launch("stronglifts5x5_workouts.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Workout History (.CSV)")
                }
            }
        }
    }

    editingExercise?.let { exercise ->
        val initialLb = uiState.exerciseStates[exercise]?.currentWeightLb
            ?: exercise.defaultStartWeightLb
        PlateCalculatorSheet(
            exercise = exercise,
            initialWeightLb = initialLb,
            barWeightLb = prefs.barbellWeightLb,
            unit = unit,
            onDismiss = { editingExercise = null },
            onSaveWeightDisplay = { newDisplay ->
                onUpdateExerciseWeight(exercise, newDisplay)
            }
        )
    }
}
