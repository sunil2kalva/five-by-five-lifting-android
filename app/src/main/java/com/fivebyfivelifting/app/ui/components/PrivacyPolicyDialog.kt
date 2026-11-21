package com.fivebyfivelifting.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fivebyfivelifting.app.domain.HealthConnectManager

/**
 * In-app privacy & Health Connect permissions rationale dialog shown both when launched from
 * Android Health Connect (`ACTION_SHOW_PERMISSIONS_RATIONALE` / `VIEW_PERMISSION_USAGE`) and
 * directly from Settings.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Privacy & Health Data Usage",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Five By Five Lifting stores all workout logs locally on your device. " +
                        "There are no ads, no backend servers, and no third-party tracking SDKs.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Health Connect (Write-Only)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Only requests WRITE_EXERCISE permission — we never read your existing health records.\n" +
                        "• When enabled, completed workouts write the session title, start/end times, " +
                        "and set/volume summary to Health Connect.\n" +
                        "• Data is never sold, shared for advertising, or sent to any external server.\n" +
                        "• Disable sync anytime in Settings or revoke access inside Android Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Google Sign-In & Drive Backup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Optional Google Sign-In uses only basic name & email locally on-device to label and pre-select your backup account.\n" +
                        "• JSON/CSV backups go strictly to the location you choose via Android's file picker or Google Drive app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Deleting Data",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Delete single workouts in History, sign out in Settings, clear app storage/uninstall in Android Settings, " +
                        "or delete synced records directly inside Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(HealthConnectManager.PRIVACY_POLICY_URL))
                        )
                    }
                }
            ) {
                Text("Web Policy")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
