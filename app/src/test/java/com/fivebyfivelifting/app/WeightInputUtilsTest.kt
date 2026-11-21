package com.fivebyfivelifting.app

import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.domain.WeightInputUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightInputUtilsTest {

    @Test
    fun `sanitize strips non numeric characters and limits decimal places`() {
        assertEquals("185", WeightInputUtils.sanitizeWeightInput("185"))
        assertEquals("137.5", WeightInputUtils.sanitizeWeightInput("137.5"))
        assertEquals("137.5", WeightInputUtils.sanitizeWeightInput("137,5"))
        assertEquals("137.52", WeightInputUtils.sanitizeWeightInput("137.5.2"))
        assertEquals("137.56", WeightInputUtils.sanitizeWeightInput("137.5678"))
        assertEquals("135", WeightInputUtils.sanitizeWeightInput("-135 lb"))
        assertEquals("", WeightInputUtils.sanitizeWeightInput("abc"))
        assertEquals("135.", WeightInputUtils.sanitizeWeightInput("135."))
    }

    @Test
    fun `parseWeightDisplay returns null for blank zero or dot only`() {
        assertNull(WeightInputUtils.parseWeightDisplay("", WeightUnit.LB))
        assertNull(WeightInputUtils.parseWeightDisplay(".", WeightUnit.LB))
        assertNull(WeightInputUtils.parseWeightDisplay("0", WeightUnit.LB))
        assertNull(WeightInputUtils.parseWeightDisplay("0.0", WeightUnit.KG))
    }

    @Test
    fun `parseWeightDisplay parses positive numbers including trailing decimal and caps extreme values`() {
        assertEquals(185.0, WeightInputUtils.parseWeightDisplay("185", WeightUnit.LB)!!, 0.001)
        assertEquals(137.5, WeightInputUtils.parseWeightDisplay("137.5", WeightUnit.LB)!!, 0.001)
        assertEquals(135.0, WeightInputUtils.parseWeightDisplay("135.", WeightUnit.LB)!!, 0.001)
        assertEquals(2000.0, WeightInputUtils.parseWeightDisplay("999999", WeightUnit.LB)!!, 0.001)
        assertEquals(1000.0, WeightInputUtils.parseWeightDisplay("999999", WeightUnit.KG)!!, 0.001)
    }

    @Test
    fun `stepWeightDisplay increments and decrements by 5 lb or 2_5 kg cleanly`() {
        assertEquals(140.0, WeightInputUtils.stepWeightDisplay(135.0, 5.0, WeightUnit.LB), 0.001)
        assertEquals(130.0, WeightInputUtils.stepWeightDisplay(135.0, -5.0, WeightUnit.LB), 0.001)
        assertEquals(5.0, WeightInputUtils.stepWeightDisplay(5.0, -5.0, WeightUnit.LB), 0.001)
        assertEquals(2000.0, WeightInputUtils.stepWeightDisplay(1998.0, 5.0, WeightUnit.LB), 0.001)

        assertEquals(62.5, WeightInputUtils.stepWeightDisplay(60.0, 2.5, WeightUnit.KG), 0.001)
        assertEquals(57.5, WeightInputUtils.stepWeightDisplay(60.0, -2.5, WeightUnit.KG), 0.001)
        assertEquals(2.5, WeightInputUtils.stepWeightDisplay(2.5, -2.5, WeightUnit.KG), 0.001)
    }

    @Test
    fun `formatEditableWeight strips trailing decimal zeroes`() {
        assertEquals("135", WeightInputUtils.formatEditableWeight(135.0))
        assertEquals("5", WeightInputUtils.formatEditableWeight(5.0))
        assertEquals("137.5", WeightInputUtils.formatEditableWeight(137.5))
        assertEquals("61.25", WeightInputUtils.formatEditableWeight(61.25))
    }

    @Test
    fun `WeightUnit preserves arbitrary typed weights without snapping to 2_5 lb or 1_25 kg`() {
        assertEquals("132 lb", WeightUnit.LB.format(132.0))
        assertEquals("137.5 lb", WeightUnit.LB.format(137.5))
        assertEquals("185 lb", WeightUnit.LB.format(185.0))

        val kg62Lb = WeightUnit.KG.toLb(62.0)
        assertEquals(62.0, WeightUnit.KG.fromLb(kg62Lb), 0.001)
        assertEquals("62 kg", WeightUnit.KG.format(kg62Lb))

        val kg6125Lb = WeightUnit.KG.toLb(61.25)
        assertEquals(61.25, WeightUnit.KG.fromLb(kg6125Lb), 0.001)
        assertEquals("61.25 kg", WeightUnit.KG.format(kg6125Lb))

        assertEquals(20.0, WeightUnit.KG.fromLb(WeightUnit.KG.defaultBarWeightLb), 0.001)
    }
}
