package com.fivebyfivelifting.app.domain

import com.fivebyfivelifting.app.data.model.WeightUnit
import kotlin.math.roundToInt

data class PlateSpec(
    val weightDisplay: Double,
    val label: String,
    val colorHex: Long,
    val heightFraction: Float
)

data class PlateLoadout(
    val targetWeightDisplay: Double,
    val barWeightDisplay: Double,
    val unit: WeightUnit,
    val platesPerSide: List<PlateSpec>,
    val remainderPerSideDisplay: Double
) {
    fun summaryText(): String {
        if (platesPerSide.isEmpty()) {
            return "Empty bar (${formatNumber(barWeightDisplay)} ${unit.label})"
        }
        val grouped = platesPerSide.joinToString(" + ") { it.label }
        return "$grouped ${unit.label} / side"
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}

object PlateCalculator {

    private val IMPERIAL_PLATES = listOf(
        PlateSpec(45.0, "45", 0xFF2563EB, 1.00f), // Olympic Blue
        PlateSpec(35.0, "35", 0xFFEAB308, 0.88f), // Olympic Yellow
        PlateSpec(25.0, "25", 0xFF16A34A, 0.76f), // Olympic Green
        PlateSpec(10.0, "10", 0xFFE5E7EB, 0.62f), // White / Silver
        PlateSpec(5.0, "5", 0xFFEF4444, 0.50f),   // Red small
        PlateSpec(2.5, "2.5", 0xFF9CA3AF, 0.42f)  // Micro gray
    )

    private val METRIC_PLATES = listOf(
        PlateSpec(20.0, "20", 0xFF2563EB, 1.00f),   // IWF Blue
        PlateSpec(15.0, "15", 0xFFEAB308, 0.86f),   // IWF Yellow
        PlateSpec(10.0, "10", 0xFF16A34A, 0.72f),   // IWF Green
        PlateSpec(5.0, "5", 0xFFE5E7EB, 0.58f),     // IWF White
        PlateSpec(2.5, "2.5", 0xFF374151, 0.48f),   // Black
        PlateSpec(1.25, "1.25", 0xFF9CA3AF, 0.40f)  // Chrome
    )

    fun calculatePlates(
        targetWeightLb: Double,
        barWeightLb: Double = 45.0,
        unit: WeightUnit = WeightUnit.LB
    ): PlateLoadout {
        val targetDisplay = unit.fromLb(targetWeightLb)
        val barDisplay = if (unit == WeightUnit.KG && barWeightLb == 45.0) {
            20.0
        } else {
            unit.fromLb(barWeightLb)
        }

        if (targetDisplay <= barDisplay) {
            return PlateLoadout(
                targetWeightDisplay = targetDisplay,
                barWeightDisplay = barDisplay,
                unit = unit,
                platesPerSide = emptyList(),
                remainderPerSideDisplay = 0.0
            )
        }

        var remainingPerSide = (targetDisplay - barDisplay) / 2.0
        val availableCatalog = if (unit == WeightUnit.LB) IMPERIAL_PLATES else METRIC_PLATES
        val chosen = mutableListOf<PlateSpec>()

        for (plate in availableCatalog) {
            while (remainingPerSide + 1e-6 >= plate.weightDisplay) {
                chosen.add(plate)
                remainingPerSide -= plate.weightDisplay
            }
        }

        val cleanRemainder = if (remainingPerSide < 0.01) 0.0 else remainingPerSide

        return PlateLoadout(
            targetWeightDisplay = targetDisplay,
            barWeightDisplay = barDisplay,
            unit = unit,
            platesPerSide = chosen,
            remainderPerSideDisplay = cleanRemainder
        )
    }
}
