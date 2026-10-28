package com.stronglifts5x5.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stronglifts5x5.app.data.model.ExerciseType
import com.stronglifts5x5.app.domain.ProgressionEngine
import com.stronglifts5x5.app.ui.StrengthUiState
import com.stronglifts5x5.app.ui.theme.AccentBlue
import com.stronglifts5x5.app.ui.theme.EmeraldSuccess
import com.stronglifts5x5.app.ui.theme.StrongRed

private enum class GraphTarget(val label: String, val exercise: ExerciseType?) {
    SQUAT("Squat", ExerciseType.SQUAT),
    BENCH("Bench", ExerciseType.BENCH_PRESS),
    ROW("Row", ExerciseType.BARBELL_ROW),
    OHP("OHP", ExerciseType.OVERHEAD_PRESS),
    DEADLIFT("Deadlift", ExerciseType.DEADLIFT),
    BODYWEIGHT("Bodyweight", null)
}

private enum class TimeRange(val label: String, val days: Int?) {
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    ALL("ALL", null)
}

data class ChartPoint(
    val timestampMillis: Long,
    val weightLb: Double
)

@Composable
fun GraphsScreen(
    uiState: StrengthUiState,
    onLogBodyweight: (Double) -> Unit
) {
    val unit = uiState.preferences.weightUnit
    var selectedTarget by remember { mutableStateOf(GraphTarget.SQUAT) }
    var selectedRange by remember { mutableStateOf(TimeRange.ALL) }
    var newBwInput by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val cutoffMillis = selectedRange.days?.let { now - it * 86_400_000L } ?: 0L

    val points: List<ChartPoint> = remember(
        uiState.completedSessions,
        uiState.bodyweightEntries,
        uiState.exerciseStates,
        selectedTarget,
        cutoffMillis
    ) {
        val exercise = selectedTarget.exercise
        if (exercise != null) {
            val sessionPoints = uiState.completedSessions
                .sortedBy { it.session.startedAtEpochMillis }
                .mapNotNull { s ->
                    val ts = s.session.completedAtEpochMillis ?: s.session.startedAtEpochMillis
                    if (ts < cutoffMillis) return@mapNotNull null
                    val workSets = s.workingSetsFor(exercise)
                    val topWeight = workSets.maxOfOrNull { it.targetWeightLb }
                    if (topWeight != null) ChartPoint(ts, topWeight) else null
                }
            if (sessionPoints.isNotEmpty()) {
                sessionPoints
            } else {
                val currentLb = uiState.exerciseStates[exercise]?.currentWeightLb
                    ?: exercise.defaultStartWeightLb
                listOf(ChartPoint(now, currentLb))
            }
        } else {
            uiState.bodyweightEntries
                .filter { it.recordedAtEpochMillis >= cutoffMillis }
                .sortedBy { it.recordedAtEpochMillis }
                .map { ChartPoint(it.recordedAtEpochMillis, it.weightLb) }
        }
    }

    val currentWeightLb = points.lastOrNull()?.weightLb
        ?: selectedTarget.exercise?.let { uiState.exerciseStates[it]?.currentWeightLb }
        ?: 0.0
    val maxWeightLb = points.maxOfOrNull { it.weightLb } ?: currentWeightLb
    val estimatedOneRepMaxLb = if (selectedTarget.exercise != null) {
        ProgressionEngine.estimateOneRepMaxLb(maxWeightLb, 5)
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Progress Charts & Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // Target selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GraphTarget.entries.forEach { target ->
                FilterChip(
                    selected = selectedTarget == target,
                    onClick = { selectedTarget = target },
                    label = { Text(target.label) }
                )
            }
        }

        // Time range filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(range.label) }
                )
            }
        }

        // Stat Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                label = "Current",
                value = if (currentWeightLb > 0) unit.format(currentWeightLb) else "—",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Peak / PR",
                value = if (maxWeightLb > 0) unit.format(maxWeightLb) else "—",
                modifier = Modifier.weight(1f)
            )
            if (selectedTarget.exercise != null) {
                MetricCard(
                    label = "Est. 1RM",
                    value = if (estimatedOneRepMaxLb > 0) unit.format(estimatedOneRepMaxLb) else "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Line Chart Canvas Card
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
                Text(
                    text = "${selectedTarget.label} Progression (${unit.label})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (points.isEmpty()) {
                    Text(
                        text = "No entries recorded for this timeframe yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 40.dp)
                    )
                } else {
                    val lineColor = if (selectedTarget == GraphTarget.BODYWEIGHT) {
                        EmeraldSuccess
                    } else {
                        StrongRed
                    }
                    ProgressionLineChart(
                        points = points.map { unit.fromLb(it.weightLb) },
                        lineColor = lineColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }

        // Quick Bodyweight Log Card when Bodyweight is selected
        if (selectedTarget == GraphTarget.BODYWEIGHT) {
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
                        text = "Log Today's Bodyweight",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBwInput,
                            onValueChange = { newBwInput = it },
                            label = { Text("Weight (${unit.label})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                newBwInput.trim().toDoubleOrNull()?.let { bw ->
                                    if (bw > 0) {
                                        onLogBodyweight(bw)
                                        newBwInput = ""
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionLineChart(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val minVal = (points.minOrNull() ?: 0.0) - 5.0
    val maxVal = (points.maxOrNull() ?: 100.0) + 5.0
    val range = (maxVal - minVal).coerceAtLeast(10.0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pad = 16.dp.toPx()
        val usableW = (width - pad * 2).coerceAtLeast(1f)
        val usableH = (height - pad * 2).coerceAtLeast(1f)

        // Draw horizontal reference grid lines
        for (i in 0..4) {
            val y = pad + (usableH * i / 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(pad, y),
                end = Offset(width - pad, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val coords = points.mapIndexed { idx, value ->
            val x = if (points.size <= 1) {
                width / 2f
            } else {
                pad + usableW * (idx.toFloat() / (points.size - 1).toFloat())
            }
            val normalizedY = ((value - minVal) / range).toFloat().coerceIn(0f, 1f)
            val y = pad + usableH * (1f - normalizedY)
            Offset(x, y)
        }

        if (coords.size >= 2) {
            val path = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    lineTo(coords[i].x, coords[i].y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.5.dp.toPx())
            )
        }

        coords.forEach { pt ->
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = AccentBlue,
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
