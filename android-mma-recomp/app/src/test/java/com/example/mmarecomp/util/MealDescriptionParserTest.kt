package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDescriptionParserTest {

    private fun meal(description: String?, calories: Int = 600) = Meal(
        id = "1",
        userId = "u",
        date = "2026-08-31",
        repas = 1,
        calories = calories,
        proteinesG = 40.0,
        glucidesG = 60.0,
        lipidesG = 15.0,
        description = description,
    )

    @Test
    fun `repartit les macros sur chaque aliment de la description`() {
        val lines = MealDescriptionParser.linesFromMeal(meal("Poulet, Riz, Brocoli"))
        assertEquals(3, lines.size)
        assertEquals("Poulet", lines[0].label)
        assertEquals(200, lines[0].calories)
        assertEquals(40.0 / 3, lines[0].proteinesG, 0.001)
    }

    @Test
    fun `sans description une seule ligne repas porte tout`() {
        val lines = MealDescriptionParser.linesFromMeal(meal(null))
        assertEquals(1, lines.size)
        assertEquals("Repas", lines[0].label)
        assertEquals(600, lines[0].calories)
    }

    @Test
    fun `summary affiche la description quand elle existe`() {
        assertTrue(MealDescriptionParser.summaryLabel(meal("Oeufs, Avoine")).contains("Oeufs"))
    }
}
