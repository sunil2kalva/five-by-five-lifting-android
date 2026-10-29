package com.fivebyfivelifting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.domain.PlateCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorSheet(
    exercise: ExerciseType,
    initialWeightLb: Double,
    barWeightLb: Double,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSaveWeightDisplay: (Double) -> Unit
) {
    val stepDisplay = unit.minStepDisplay
    var currentDisplayWeight by remember(initialWeightLb, unit) {
        mutableDoubleStateOf(unit.fromLb(initialWeightLb))
    }

    val currentWeightLb = unit.toLb(currentDisplayWeight)
    val loadout = remember(currentWeightLb, barWeightLb, unit) {
        PlateCalculator.calculatePlates(
            targetWeightLb = currentWeightLb,
            barWeightLb = barWeightLb,
            unit = unit
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${exercise.displayName} — Barbell Plate Loader",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Total: ${unit.format(currentWeightLb)} (Bar: ${unit.format(barWeightLb)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Visual Barbell One-Side Sleeve Diagram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Barbell shaft + collar + sleeve
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF94A3B8))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Inner barbell collar
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF64748B))
                    )

                    if (loadout.platesPerSide.isEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Empty Barbell — No Plates Needed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        loadout.platesPerSide.forEach { plate ->
                            val plateHeight = (110f * plate.heightFraction).dp
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(plateHeight)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(plate.colorHex))
                                    .border(
                                        width = 1.dp,
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = plate.label,
                                    color = if (plate.weightDisplay == 10.0 && unit == WeightUnit.LB) {
                                        Color.Black
                                    } else {
                                        Color.White
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Load per side: ${loadout.summaryText()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Weight Adjuster Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        currentDisplayWeight = (currentDisplayWeight - stepDisplay)
                            .coerceAtLeast(unit.defaultBarWeightDisplay)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "-$stepDisplay ${unit.label}")
                }

                OutlinedButton(
                    onClick = {
                        currentDisplayWeight += stepDisplay
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "+$stepDisplay ${unit.label}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onSaveWeightDisplay(currentDisplayWeight)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply ${unit.format(currentWeightLb)} to ${exercise.displayName}")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
