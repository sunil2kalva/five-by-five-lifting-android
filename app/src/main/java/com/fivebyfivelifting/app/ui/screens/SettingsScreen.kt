package com.fivebyfivelifting.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.domain.HealthConnectManager
import com.fivebyfivelifting.app.ui.StrengthUiState
import com.fivebyfivelifting.app.ui.components.PlateCalculatorSheet
import com.fivebyfivelifting.app.ui.theme.AmberWarning
import com.fivebyfivelifting.app.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingDriveAction {
    NONE,
    CONFIGURE_BACKUP_IN_DRIVE,
    OPEN_RESTORE_FILE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: StrengthUiState,
    onSelectWeightUnit: (WeightUnit) -> Unit,
    onSelectBarbellWeightDisplay: (Double) -> Unit,
    onSelectRestTimerSeconds: (Int) -> Unit,
    onToggleHealthConnectSync: (Boolean, String?) -> Unit,
    onUpdateExerciseWeight: (ExerciseType, Double) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onPrepareGoogleSignInIntent: (Boolean, (Intent) -> Unit) -> Unit,
    onHandleGoogleSignInResult: (Intent?, (String) -> Unit, () -> Unit) -> Unit,
    onSignOutGoogleAccount: () -> Unit,
    buildOpenGoogleDriveFileIntent: (String?) -> Intent,
    onLaunchDirectSaveToGoogleDriveApp: (String?, (Intent) -> Unit) -> Unit,
    onRestoreFromGoogleDriveUri: (Uri) -> Unit,
    onExportCsvToUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = uiState.preferences
    val unit = prefs.weightUnit
    var editingExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var pendingDriveAction by remember { mutableStateOf(PendingDriveAction.NONE) }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectManager.createPermissionRequestContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(HealthConnectManager.requiredPermissions)) {
            onToggleHealthConnectSync(true, "Health Connect workout sync enabled!")
        } else {
            // Even if Android 14+ returned partial or empty set on some OEM flows, check actual granted state
            coroutineScope.launch {
                if (HealthConnectManager.hasAllPermissions(context)) {
                    onToggleHealthConnectSync(true, "Health Connect workout sync enabled!")
                } else {
                    onToggleHealthConnectSync(
                        true,
                        "Health Connect sync enabled — grant Exercise permission in Health Connect if prompted"
                    )
                }
            }
        }
    }

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

    // Fallback Google Sign-In launcher without ID token if OAuth Client ID is Android-only
    val fallbackGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onHandleGoogleSignInResult(
            result.data,
            { signedInEmail ->
                when (pendingDriveAction) {
                    PendingDriveAction.CONFIGURE_BACKUP_IN_DRIVE -> {
                        pendingDriveAction = PendingDriveAction.NONE
                        onLaunchDirectSaveToGoogleDriveApp(signedInEmail) { driveIntent ->
                            context.startActivity(driveIntent)
                        }
                    }
                    PendingDriveAction.OPEN_RESTORE_FILE -> {
                        pendingDriveAction = PendingDriveAction.NONE
                        openDriveDocLauncher.launch(buildOpenGoogleDriveFileIntent(signedInEmail))
                    }
                    PendingDriveAction.NONE -> {}
                }
            },
            {}
        )
    }

    // Primary Native Google Sign-In Launcher (with OAuth Client ID)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onHandleGoogleSignInResult(
            result.data,
            { signedInEmail ->
                when (pendingDriveAction) {
                    PendingDriveAction.CONFIGURE_BACKUP_IN_DRIVE -> {
                        pendingDriveAction = PendingDriveAction.NONE
                        onLaunchDirectSaveToGoogleDriveApp(signedInEmail) { driveIntent ->
                            context.startActivity(driveIntent)
                        }
                    }
                    PendingDriveAction.OPEN_RESTORE_FILE -> {
                        pendingDriveAction = PendingDriveAction.NONE
                        openDriveDocLauncher.launch(buildOpenGoogleDriveFileIntent(signedInEmail))
                    }
                    PendingDriveAction.NONE -> {}
                }
            },
            {
                // Automatic retry with Android default sign-in if ID token client type differs
                onPrepareGoogleSignInIntent(false) { fallbackIntent ->
                    fallbackGoogleSignInLauncher.launch(fallbackIntent)
                }
            }
        )
    }

    fun startGoogleSignInFlow(afterSignInAction: PendingDriveAction = PendingDriveAction.NONE) {
        pendingDriveAction = afterSignInAction
        onPrepareGoogleSignInIntent(true) { signInIntent ->
            googleSignInLauncher.launch(signInIntent)
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
            text = "Settings & Google Sign-In",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // 1. Google Sign-In & Google Drive Backup Card
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
                        text = "Google Sign-In & Drive Backup",
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
                    // STATE A: Signed in with Google AND Backup File is configured
                    Text(
                        text = "Signed in with Google and linked to your Google Drive backup file.",
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
                            // Row 1: Signed-in Google Profile + Switch/Sign Out
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Signed-In Google Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!prefs.googleDisplayName.isNullOrBlank()) {
                                        Text(
                                            text = "✓ ${prefs.googleDisplayName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                        Text(
                                            text = prefs.googleAccountEmail ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "✓ ${prefs.googleAccountEmail}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    OutlinedButton(
                                        onClick = { startGoogleSignInFlow(PendingDriveAction.NONE) }
                                    ) {
                                        Text("Change User")
                                    }
                                    TextButton(onClick = onSignOutGoogleAccount) {
                                        Text("Sign Out", style = MaterialTheme.typography.labelSmall)
                                    }
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
                    // STATE B: Not yet configured (either Google Sign-In or Backup File is missing)
                    Text(
                        text = "Sign in with your Google Account and choose a folder in Google Drive to store your workout backup file.",
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
                            // Step 1: Google Sign-In Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Step 1 • Google Account Login",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (prefs.isSignedInWithGoogle) {
                                        val displayLabel = prefs.googleDisplayName?.let {
                                            "✓ $it (${prefs.googleAccountEmail})"
                                        } ?: "✓ ${prefs.googleAccountEmail}"
                                        Text(
                                            text = displayLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    } else {
                                        Text(
                                            text = "Not signed in",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { startGoogleSignInFlow(PendingDriveAction.NONE) }
                                ) {
                                    Text(if (prefs.isSignedInWithGoogle) "Change User" else "Sign in with Google")
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            // Step 2: Backup File Status
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
                            if (!prefs.isSignedInWithGoogle) {
                                startGoogleSignInFlow(PendingDriveAction.CONFIGURE_BACKUP_IN_DRIVE)
                            } else {
                                onLaunchDirectSaveToGoogleDriveApp(prefs.googleAccountEmail) { driveIntent ->
                                    context.startActivity(driveIntent)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (!prefs.isSignedInWithGoogle) {
                                "Sign in with Google & Select Backup File"
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
                            if (!prefs.isSignedInWithGoogle) {
                                startGoogleSignInFlow(PendingDriveAction.OPEN_RESTORE_FILE)
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

        // 2. Rest Timer Between Sets Card (Default = 3:00 / 180 seconds)
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
                val mins = prefs.restTimerSeconds / 60
                val secs = prefs.restTimerSeconds % 60
                val formattedCurrent = String.format("%d:%02d (%ds)", mins, secs, prefs.restTimerSeconds)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rest Timer Between Sets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = formattedCurrent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Default countdown timer started automatically after logging a work set (defaults to 3:00 / 180s):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val timerPresets = listOf(
                    60 to "1:00",
                    90 to "1:30",
                    120 to "2:00",
                    150 to "2:30",
                    180 to "3:00 (Default)",
                    300 to "5:00"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    timerPresets.forEach { (secondsValue, label) ->
                        FilterChip(
                            selected = prefs.restTimerSeconds == secondsValue,
                            onClick = { onSelectRestTimerSeconds(secondsValue) },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onSelectRestTimerSeconds((prefs.restTimerSeconds - 15).coerceAtLeast(30))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-15s")
                    }
                    OutlinedButton(
                        onClick = {
                            onSelectRestTimerSeconds((prefs.restTimerSeconds + 15).coerceAtMost(600))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+15s")
                    }
                }
            }
        }

        // 3. Android Health Connect Sync Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Android Health Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (prefs.healthConnectSyncEnabled) {
                            EmeraldSuccess.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (prefs.healthConnectSyncEnabled) "● Sync Enabled" else "Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (prefs.healthConnectSyncEnabled) {
                                EmeraldSuccess
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Automatically sync completed strength workouts (exercise session, duration, sets, and total volume) to the Android Health Connect ecosystem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync workouts to Health Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = prefs.healthConnectSyncEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                onToggleHealthConnectSync(false, "Health Connect workout sync disabled")
                            } else {
                                if (!HealthConnectManager.isAvailable(context)) {
                                    onToggleHealthConnectSync(
                                        false,
                                        "Health Connect is not available or needs to be installed on this device"
                                    )
                                } else {
                                    coroutineScope.launch {
                                        if (HealthConnectManager.hasAllPermissions(context)) {
                                            onToggleHealthConnectSync(true, "Health Connect workout sync enabled!")
                                        } else {
                                            runCatching {
                                                healthConnectPermissionLauncher.launch(
                                                    HealthConnectManager.requiredPermissions
                                                )
                                            }.onFailure {
                                                onToggleHealthConnectSync(
                                                    true,
                                                    "Health Connect sync enabled — check permissions in Health Connect settings"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (prefs.healthConnectSyncEnabled) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(HealthConnectManager.buildHealthConnectSettingsIntent())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Health Connect Permissions & Settings")
                    }
                }
            }
        }

        // 4. Weight Unit & Barbell Configuration Card
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
