package com.fivebyfivelifting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivebyfivelifting.app.data.model.ExerciseType
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.domain.PlateCalculator
import com.fivebyfivelifting.app.domain.WeightInputUtils
import com.fivebyfivelifting.app.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorSheet(
    exercise: ExerciseType,
    initialWeightLb: Double,
    barWeightLb: Double,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onWeightChangeDisplay: (Double) -> Unit,
    exerciseName: String = exercise.displayName
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val stepDisplay = unit.minStepDisplay
    val stepLabel = WeightInputUtils.formatEditableWeight(stepDisplay)
    val minStepFloor = WeightInputUtils.minStepFloorDisplay(unit)
    val maxDisplay = WeightInputUtils.maxWeightDisplay(unit)

    // Key by (exercise, unit) — NOT initialWeightLb — so live DB updates emitted while typing
    // do not reset the user's in-progress text field or strip a trailing decimal point.
    var appliedDisplayWeight by remember(exercise, unit) {
        mutableDoubleStateOf(unit.fromLb(initialWeightLb))
    }
    var weightText by remember(exercise, unit) {
        mutableStateOf(WeightInputUtils.formatEditableWeight(unit.fromLb(initialWeightLb)))
    }

    fun applyLiveWeight(newDisplay: Double) {
        if (newDisplay != appliedDisplayWeight) {
            appliedDisplayWeight = newDisplay
            onWeightChangeDisplay(newDisplay)
        }
    }

    val currentWeightLb = unit.toLb(appliedDisplayWeight)
    val loadout = remember(currentWeightLb, barWeightLb, unit) {
        PlateCalculator.calculatePlates(
            targetWeightLb = currentWeightLb,
            barWeightLb = barWeightLb,
            unit = unit
        )
    }

    val parsedLive = WeightInputUtils.parseWeightDisplay(weightText, unit)
    val rawNumeric = weightText.toDoubleOrNull()
    val isInvalidNonBlank = weightText.isNotEmpty() && weightText != "." && parsedLive == null
    val isAboveMax = rawNumeric != null && rawNumeric > maxDisplay

    val helperMessage: String? = when {
        weightText.isEmpty() || weightText == "." ->
            "Type a weight — currently keeping ${unit.format(currentWeightLb)}"
        isInvalidNonBlank ->
            "Enter a weight greater than 0 (keeping ${unit.format(currentWeightLb)})"
        isAboveMax ->
            "Capped at ${WeightInputUtils.formatEditableWeight(maxDisplay)} ${unit.label}"
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$exerciseName — Weight & Plates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Active: ${unit.format(currentWeightLb)} • Auto-saved as you change",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldSuccess,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Editable weight input defaulting to current weight; updates take effect immediately.
            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val sanitized = WeightInputUtils.sanitizeWeightInput(raw)
                    weightText = sanitized
                    val parsed = WeightInputUtils.parseWeightDisplay(sanitized, unit)
                    if (parsed != null) {
                        applyLiveWeight(parsed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Working Weight (${unit.label})") },
                placeholder = { Text(WeightInputUtils.formatEditableWeight(appliedDisplayWeight)) },
                suffix = {
                    Text(
                        text = unit.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (weightText.isNotEmpty()) {
                        IconButton(
                            onClick = { weightText = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear weight"
                            )
                        }
                    }
                },
                singleLine = true,
                isError = isInvalidNonBlank,
                supportingText = if (helperMessage != null) {
                    { Text(helperMessage) }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Instant +/- stepper buttons (-5 lb / +5 lb, or -2.5 kg / +2.5 kg in metric)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val base = parsedLive ?: appliedDisplayWeight
                        val next = WeightInputUtils.stepWeightDisplay(base, -stepDisplay, unit)
                        weightText = WeightInputUtils.formatEditableWeight(next)
                        applyLiveWeight(next)
                    },
                    enabled = (parsedLive ?: appliedDisplayWeight) > minStepFloor,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "-$stepLabel ${unit.label}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val base = parsedLive ?: appliedDisplayWeight
                        val next = WeightInputUtils.stepWeightDisplay(base, stepDisplay, unit)
                        weightText = WeightInputUtils.formatEditableWeight(next)
                        applyLiveWeight(next)
                    },
                    enabled = (parsedLive ?: appliedDisplayWeight) < maxDisplay,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+$stepLabel ${unit.label}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Visual Barbell One-Side Sleeve Diagram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Barbell shaft
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF94A3B8))
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
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
                            val plateHeight = (100f * plate.heightFraction).dp
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

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Load per side: ${loadout.summaryText()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = buildString {
                    append("Bar: ${unit.format(barWeightLb)}")
                    if (loadout.remainderPerSideDisplay > 0.0) {
                        val remTotal = WeightInputUtils.formatEditableWeight(
                            loadout.remainderPerSideDisplay * 2.0
                        )
                        append(" • +$remTotal ${unit.label} fractional remainder")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
