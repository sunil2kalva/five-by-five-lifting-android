package com.fivebyfivelifting.app.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.ui.StrengthUiState
import com.fivebyfivelifting.app.ui.components.PlateCalculatorSheet
import com.fivebyfivelifting.app.ui.theme.AccentBlue
import com.fivebyfivelifting.app.ui.theme.AmberWarning
import com.fivebyfivelifting.app.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingDriveAction {
    NONE,
    CONFIGURE_BACKUP_IN_DRIVE,
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
    buildOpenGoogleDriveFileIntent: (String?) -> Intent,
    onLaunchDirectSaveToGoogleDriveApp: (String?, (Intent) -> Unit) -> Unit,
    onRestoreFromGoogleDriveUri: (Uri) -> Unit,
    onExportCsvToUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val prefs = uiState.preferences
    val unit = prefs.weightUnit
    var editingExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var pendingDriveAction by remember { mutableStateOf(PendingDriveAction.NONE) }

    // Launcher for opening/restoring an existing backup file from Google Drive
    val openDriveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onRestoreFromGoogleDriveUri(uri)
            }
        }
    }

    // Native Android Google Account Picker Launcher (lists all logged-in Google users on the phone)
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
            PendingDriveAction.CONFIGURE_BACKUP_IN_DRIVE -> {
                pendingDriveAction = PendingDriveAction.NONE
                if (!effectiveEmail.isNullOrBlank()) {
                    onLaunchDirectSaveToGoogleDriveApp(effectiveEmail) { driveIntent ->
                        context.startActivity(driveIntent)
                    }
                }
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

    val isBackupConfigured = prefs.isBackupSetupComplete
    val isSyncConfiguredAndWorking = prefs.isSyncConfiguredAndWorking

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Google Drive Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isBackupConfigured) {
                            EmeraldSuccess.copy(alpha = 0.16f)
                        } else {
                            AmberWarning.copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = if (isBackupConfigured) "● Configured" else "Setup Needed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBackupConfigured) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isBackupConfigured) {
                    // STATE A: User AND Backup File are already configured
                    Text(
                        text = "Your Google Account and backup file are configured. Workouts are backed up to your Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Configured Google User + Change option
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Google Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "✓ ${prefs.googleAccountEmail}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingDriveAction = PendingDriveAction.NONE
                                        googleAccountPickerLauncher.launch(buildChooseGoogleAccountIntent())
                                    }
                                ) {
                                    Text("Change User")
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            // Row 2: Configured Backup File + Change option
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Backup File in Google Drive",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "✓ ${prefs.effectiveBackupFileName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                    val lastSyncStr = prefs.lastCloudSyncEpochMillis?.let {
                                        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(it))
                                    } ?: "Configured"
                                    Text(
                                        text = "Last updated: $lastSyncStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        onLaunchDirectSaveToGoogleDriveApp(prefs.googleAccountEmail) { driveIntent ->
                                            context.startActivity(driveIntent)
                                        }
                                    }
                                ) {
                                    Text("Change File")
                                }
                            }
                        }
                    }

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
                } else {
                    // STATE B: Not yet configured (either Google User or Backup File is missing)
                    Text(
                        text = "Select your Google Account and choose a folder in Google Drive to store your workout backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Step 1: Google User Selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Step 1 • Google Account on Device",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = prefs.googleAccountEmail?.let { "✓ $it" }
                                            ?: "Not selected yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prefs.googleAccountEmail != null) {
                                            EmeraldSuccess
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
                                    Text(if (prefs.googleAccountEmail == null) "Pick User" else "Change User")
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            // Step 2: Backup File Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Step 2 • Google Drive Backup File",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (prefs.isBackupFileConfigured) {
                                            "✓ ${prefs.effectiveBackupFileName}"
                                        } else {
                                            "Not configured yet"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prefs.isBackupFileConfigured) {
                                            EmeraldSuccess
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (prefs.googleAccountEmail.isNullOrBlank()) {
                                pendingDriveAction = PendingDriveAction.CONFIGURE_BACKUP_IN_DRIVE
                                googleAccountPickerLauncher.launch(buildChooseGoogleAccountIntent())
                            } else {
                                onLaunchDirectSaveToGoogleDriveApp(prefs.googleAccountEmail) { driveIntent ->
                                    context.startActivity(driveIntent)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (prefs.googleAccountEmail.isNullOrBlank()) {
                                "Pick Google User & Select Backup File"
                            } else {
                                "Select Backup Folder in Google Drive"
                            }
                        )
                    }
                }

                // Restore / Merge option is ONLY shown when backup/sync is NOT configured and working
                if (!isSyncConfiguredAndWorking) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Already have a backup file in Google Drive? Restore your workout history:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    onClick = { exportCsvLauncher.launch("fivebyfivelifting_workouts.csv") },
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
