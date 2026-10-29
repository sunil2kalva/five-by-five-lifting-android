package com.fivebyfivelifting.app

import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.domain.PlateCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `empty barbell requires zero plates`() {
        val loadout = PlateCalculator.calculatePlates(
            targetWeightLb = 45.0,
            barWeightLb = 45.0,
            unit = WeightUnit.LB
        )
        assertTrue(loadout.platesPerSide.isEmpty())
        assertEquals("Empty bar (45 lb)", loadout.summaryText())
    }

    @Test
    fun `185 lb with 45 lb bar loads one 45 and one 25 per side`() {
        val loadout = PlateCalculator.calculatePlates(
            targetWeightLb = 185.0,
            barWeightLb = 45.0,
            unit = WeightUnit.LB
        )
        assertEquals(listOf(45.0, 25.0), loadout.platesPerSide.map { it.weightDisplay })
        assertEquals("45 + 25 lb / side", loadout.summaryText())
    }

    @Test
    fun `metric 100 kg with 20 kg bar loads two 20 kg plates per side`() {
        val targetLb = WeightUnit.KG.toLb(100.0)
        val barLb = WeightUnit.KG.toLb(20.0)
        val loadout = PlateCalculator.calculatePlates(
            targetWeightLb = targetLb,
            barWeightLb = barLb,
            unit = WeightUnit.KG
        )
        assertEquals(listOf(20.0, 20.0), loadout.platesPerSide.map { it.weightDisplay })
        assertEquals("20 + 20 kg / side", loadout.summaryText())
    }
}
