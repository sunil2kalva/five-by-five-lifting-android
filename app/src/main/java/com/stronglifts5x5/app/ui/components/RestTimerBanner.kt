package com.stronglifts5x5.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stronglifts5x5.app.ui.RestTimerState

@Composable
fun RestTimerBanner(
    timerState: RestTimerState,
    onAddThirtySeconds: () -> Unit,
    onSwitchToThreeMinutes: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!timerState.isRunning && timerState.totalSeconds == 0) return

    val minutes = timerState.remainingSeconds / 60
    val seconds = timerState.remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progress = if (timerState.totalSeconds > 0) {
        timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
    } else {
        0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                        text = if (timerState.remainingSeconds > 0) {
                            "Rest Timer • ${timerState.exerciseName}"
                        } else {
                            "Rest Complete! Ready for next set"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timerState.reasonLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAddThirtySeconds) {
                        Text("+30s")
                    }
                    if (timerState.completedReps >= 5 && timerState.totalSeconds < 180) {
                        OutlinedButton(onClick = onSwitchToThreeMinutes) {
                            Text("Felt Heavy (3:00)")
                        }
                    }
                }

                TextButton(onClick = onSkip) {
                    Text("Dismiss")
                }
            }
        }
    }
}
