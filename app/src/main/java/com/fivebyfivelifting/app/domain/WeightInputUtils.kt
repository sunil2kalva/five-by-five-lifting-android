package com.fivebyfivelifting.app.domain

import com.fivebyfivelifting.app.data.model.WeightUnit
import java.util.Locale
import kotlin.math.round
import kotlin.math.roundToLong

/**
 * Pure utilities for parsing, sanitizing, stepping, and formatting live user-entered
 * barbell/exercise weights in display units (lb or kg).
 */
object WeightInputUtils {
    private const val MIN_STEP_WEIGHT_LB = 5.0
    private const val MIN_STEP_WEIGHT_KG = 2.5
    private const val MAX_WEIGHT_LB = 2000.0
    private const val MAX_WEIGHT_KG = 1000.0
    private const val MAX_DECIMAL_PLACES = 2

    fun minStepFloorDisplay(unit: WeightUnit): Double = when (unit) {
        WeightUnit.LB -> MIN_STEP_WEIGHT_LB
        WeightUnit.KG -> MIN_STEP_WEIGHT_KG
    }

    fun maxWeightDisplay(unit: WeightUnit): Double = when (unit) {
        WeightUnit.LB -> MAX_WEIGHT_LB
        WeightUnit.KG -> MAX_WEIGHT_KG
    }

    /**
     * Filters raw text field input to digits and at most one decimal separator ('.' or ','),
     * limiting fractional precision to 2 decimal places so plate math stays clean.
     */
    fun sanitizeWeightInput(raw: String): String {
        val out = StringBuilder(raw.length)
        var seenDot = false
        var decimalsCount = 0

        for (ch in raw) {
            when {
                ch.isDigit() -> {
                    if (seenDot) {
                        if (decimalsCount < MAX_DECIMAL_PLACES) {
                            out.append(ch)
                            decimalsCount++
                        }
                    } else {
                        out.append(ch)
                    }
                }
                (ch == '.' || ch == ',') && !seenDot -> {
                    seenDot = true
                    out.append('.')
                }
            }
        }
        return out.toString()
    }

    /**
     * Parses a user-typed weight string into a positive, bounded display weight.
     * Returns null for blank, partial ".", zero, or negative inputs so editing mid-keystroke
     * never overwrites a saved lift weight with zero.
     */
    fun parseWeightDisplay(raw: String, unit: WeightUnit): Double? {
        val sanitized = sanitizeWeightInput(raw).trim()
        if (sanitized.isEmpty() || sanitized == ".") return null
        val parsed = sanitized.toDoubleOrNull() ?: return null
        if (!parsed.isFinite() || parsed <= 0.0) return null
        val clamped = parsed.coerceAtMost(maxWeightDisplay(unit))
        return roundToTwoDecimals(clamped)
    }

    /**
     * Applies a +/- increment (e.g. -5/+5 lb or -2.5/+2.5 kg) bounded to safe limits.
     */
    fun stepWeightDisplay(
        currentDisplay: Double,
        deltaDisplay: Double,
        unit: WeightUnit
    ): Double {
        val floor = minStepFloorDisplay(unit)
        val ceiling = maxWeightDisplay(unit)
        val stepped = (currentDisplay + deltaDisplay).coerceIn(floor, ceiling)
        return roundToTwoDecimals(stepped)
    }

    /**
     * Formats a display weight cleanly for an editable text field or button label
     * (e.g. 135.0 -> "135", 137.5 -> "137.5", 2.5 -> "2.5").
     */
    fun formatEditableWeight(displayWeight: Double): String {
        val rounded = roundToTwoDecimals(displayWeight)
        return if (rounded % 1.0 == 0.0) {
            rounded.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0
}
