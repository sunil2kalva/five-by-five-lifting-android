package com.stronglifts5x5.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stronglifts5x5.app.data.local.WorkoutSessionWithSets
import com.stronglifts5x5.app.data.model.WorkoutType
import com.stronglifts5x5.app.ui.StrengthUiState
import com.stronglifts5x5.app.ui.theme.AccentBlue
import com.stronglifts5x5.app.ui.theme.StrongRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: StrengthUiState,
    onDeleteSession: (Long) -> Unit
) {
    val unit = uiState.preferences.weightUnit
    var monthOffset by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Workout Calendar & History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Monthly Calendar Grid Card
        item {
            MonthlyWorkoutCalendar(
                completedSessions = uiState.completedSessions,
                monthOffset = monthOffset,
                onPreviousMonth = { monthOffset -= 1 },
                onNextMonth = { monthOffset += 1 }
            )
        }

        item {
            Text(
                text = "Completed Sessions (${uiState.completedSessions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.completedSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No completed workouts yet. Start your first session from the Home tab!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.completedSessions, key = { it.session.id }) { sessionWithSets ->
                val session = sessionWithSets.session
                val dateStr = remember(session.completedAtEpochMillis, session.startedAtEpochMillis) {
                    SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.US).format(
                        Date(session.completedAtEpochMillis ?: session.startedAtEpochMillis)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${session.workoutType.title} • $dateStr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.workoutType == WorkoutType.WORKOUT_A) {
                                        StrongRed
                                    } else {
                                        AccentBlue
                                    }
                                )
                                val volumeText = "Volume: ${unit.format(sessionWithSets.totalVolumeLb())}"
                                val bwText = session.bodyweightLb?.let { " • BW: ${unit.format(it)}" } ?: ""
                                Text(
                                    text = volumeText + bwText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(onClick = { onDeleteSession(session.id) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }

                        session.workoutType.exercises().forEach { exercise ->
                            val workSets = sessionWithSets.workingSetsFor(exercise)
                            val weightLb = workSets.firstOrNull()?.targetWeightLb ?: 0.0
                            val repsString = workSets.joinToString("/") {
                                (it.completedReps ?: 0).toString()
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exercise.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${unit.format(weightLb)} ($repsString)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (session.notes.isNotBlank()) {
                            Text(
                                text = "Note: ${session.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyWorkoutCalendar(
    completedSessions: List<WorkoutSessionWithSets>,
    monthOffset: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val calendar = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthTitle = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0

    // Map day-of-month -> WorkoutType
    val dayWorkouts = remember(completedSessions, year, month) {
        val map = mutableMapOf<Int, WorkoutType>()
        val cal = Calendar.getInstance()
        completedSessions.forEach { s ->
            cal.timeInMillis = s.session.completedAtEpochMillis ?: s.session.startedAtEpochMillis
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                map[cal.get(Calendar.DAY_OF_MONTH)] = s.session.workoutType
            }
        }
        map
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                TextButton(onClick = onPreviousMonth) { Text("◀ Prev") }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNextMonth) { Text("Next ▶") }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayHeader ->
                    Text(
                        text = dayHeader,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val totalCells = ((firstDayOfWeekOffset + daysInMonth + 6) / 7) * 7
            for (week in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    for (dow in 0..6) {
                        val cellIndex = week * 7 + dow
                        val dayNum = cellIndex - firstDayOfWeekOffset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val workoutType = dayWorkouts[dayNum]
                                val circleColor = when (workoutType) {
                                    WorkoutType.WORKOUT_A -> StrongRed
                                    WorkoutType.WORKOUT_B -> AccentBlue
                                    null -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(circleColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (workoutType != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (workoutType != null) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
