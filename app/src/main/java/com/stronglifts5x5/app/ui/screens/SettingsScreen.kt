package com.stronglifts5x5.app.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.data.model.WeightUnit
import com.stronglifts5x5.app.ui.StrengthUiState
import com.stronglifts5x5.app.ui.components.PlateCalculatorSheet
import com.stronglifts5x5.app.ui.theme.AccentBlue
import com.stronglifts5x5.app.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingDriveAction {
    NONE,
    CREATE_SYNC_FILE,
    OPEN_RESTORE_FILE
}

@Composable
fun SettingsScreen(
    uiState: StrengthUiState,
    onSelectWeightUnit: (WeightUnit) -> Unit,
    onSelectBarbellWeightDisplay: (Double) -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSelectGoogleAccountEmail: (String) -> Unit,
    buildChooseGoogleAccountIntent: () -> Intent,
    buildCreateGoogleDriveFileIntent: (String?) -> Intent,
    buildOpenGoogleDriveFileIntent: (String?) -> Intent,
    onLaunchDirectSaveToGoogleDriveApp: ((Intent) -> Unit) -> Unit,
    onLinkGoogleDriveSyncFile: (Uri) -> Unit,
    onTriggerManualCloudSync: () -> Unit,
    onRestoreFromGoogleDriveUri: (Uri) -> Unit,
    onExportCsvToUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val prefs = uiState.preferences
    val unit = prefs.weightUnit
    var editingExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var pendingDriveAction by remember { mutableStateOf(PendingDriveAction.NONE) }

    // 1. Launcher for creating a backup file in the selected Google Account's Google Drive
    val createDriveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onLinkGoogleDriveSyncFile(uri)
            }
        }
    }

    // 2. Launcher for opening/restoring a backup file from the selected Google Account's Google Drive
    val openDriveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onRestoreFromGoogleDriveUri(uri)
            }
        }
    }

    // 3. Native Android Google Account Picker Launcher (lists all logged-in Google users on the phone)
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val chosenEmail = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        } else {
            null
        }
        if (!chosenEmail.isNullOrBlank()) {
            onSelectGoogleAccountEmail(chosenEmail)
        }
        val effectiveEmail = chosenEmail ?: prefs.googleAccountEmail
        when (pendingDriveAction) {
            PendingDriveAction.CREATE_SYNC_FILE -> {
                pendingDriveAction = PendingDriveAction.NONE
                createDriveDocLauncher.launch(buildCreateGoogleDriveFileIntent(effectiveEmail))
            }
            PendingDriveAction.OPEN_RESTORE_FILE -> {
                pendingDriveAction = PendingDriveAction.NONE
                openDriveDocLauncher.launch(buildOpenGoogleDriveFileIntent(effectiveEmail))
            }
            PendingDriveAction.NONE -> {}
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

        // 1. Google Account & Google Drive Cloud Backup Card
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
                    text = "Google Account & Drive Cloud Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step 1: Pick the Google Account logged into this phone.\n" +
                        "Step 2: Store or link your backup file directly inside that Google Account's Google Drive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selected Google Account Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected Google User on Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = prefs.googleAccountEmail ?: "Tap 'Pick Account' to choose user",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (prefs.googleAccountEmail != null) {
                                    AccentBlue
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pendingDriveAction = PendingDriveAction.NONE
                                googleAccountPickerLauncher.launch(buildChooseGoogleAccountIntent())
                            }
                        ) {
                            Text(if (prefs.googleAccountEmail == null) "Pick Account" else "Switch User")
                        }
                    }
                }

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

                // Option A: Open Google Drive App Directly (shows Drive's native Account & Folder picker)
                Button(
                    onClick = {
                        onLaunchDirectSaveToGoogleDriveApp { driveIntent ->
                            context.startActivity(driveIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Backup in Google Drive App (Pick Folder)")
                }

                // Option B: Link Persistent Google Drive File for Background Auto-Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (prefs.googleAccountEmail.isNullOrBlank()) {
                                // Ask user to pick their logged-in Google Account first, then open Drive
                                pendingDriveAction = PendingDriveAction.CREATE_SYNC_FILE
                                googleAccountPickerLauncher.launch(buildChooseGoogleAccountIntent())
                            } else {
                                createDriveDocLauncher.launch(
                                    buildCreateGoogleDriveFileIntent(prefs.googleAccountEmail)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (prefs.cloudSyncUriString.isNullOrBlank()) {
                                "Link Drive Auto-Sync File"
                            } else {
                                "Change Auto-Sync File"
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

                // Option C: Restore / Merge from the user's Google Drive
                OutlinedButton(
                    onClick = {
                        if (prefs.googleAccountEmail.isNullOrBlank()) {
                            pendingDriveAction = PendingDriveAction.OPEN_RESTORE_FILE
                            googleAccountPickerLauncher.launch(buildChooseGoogleAccountIntent())
                        } else {
                            openDriveDocLauncher.launch(
                                buildOpenGoogleDriveFileIntent(prefs.googleAccountEmail)
                            )
                        }
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
                            text = uiState.exerciseDisplayName(exercise),
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
