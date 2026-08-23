package com.example.mmarecomp.util

import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTargetCalculatorTest {

    @Test
    fun `training day target sits in the 2000-2100kcal, 130-140g protein range`() {
        val target = NutritionTargetCalculator.target(TypeJour.Training)
        assertEquals(2050, target.calories)
        assertEquals(135.0, target.proteinesG, 0.0001)
    }

    @Test
    fun `rest day target is lower in calories but keeps protein high (calorie cycling)`() {
        val target = NutritionTargetCalculator.target(TypeJour.Repos)
        assertEquals(1800, target.calories)
        assertEquals(130.0, target.proteinesG, 0.0001)
        assertTrue("rest day calories must be lower than training day", target.calories < NutritionTargetCalculator.target(TypeJour.Training).calories)
    }

    @Test
    fun `indicative split respects each slot's share and sums close to the daily total`() {
        val split = NutritionTargetCalculator.indicativeSplit(
            calories = 2000,
            proteinesG = 130.0,
            slots = RepasSlot.entries,
        )

        assertEquals(500, split.getValue(RepasSlot.Matin).calories)
        assertEquals(600, split.getValue(RepasSlot.PostTraining).calories)
        assertEquals(400, split.getValue(RepasSlot.ApresMidi).calories)
        assertEquals(500, split.getValue(RepasSlot.Soir).calories)

        val totalCalories = split.values.sumOf { it.calories }
        assertEquals(2000, totalCalories)
    }

    @Test
    fun `indicative split works with a partial slot list (progressive 1-to-4 meal transition)`() {
        val split = NutritionTargetCalculator.indicativeSplit(
            calories = 2000,
            proteinesG = 130.0,
            slots = listOf(RepasSlot.Matin),
        )
        assertEquals(1, split.size)
        assertEquals(500, split.getValue(RepasSlot.Matin).calories)
    }
}
