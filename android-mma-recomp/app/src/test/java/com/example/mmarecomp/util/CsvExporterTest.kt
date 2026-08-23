package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private fun meal(description: String?) = Meal(
        id = "id",
        userId = "user",
        date = "2026-01-01",
        repas = 1,
        calories = 500,
        proteinesG = 30.0,
        glucidesG = 40.0,
        lipidesG = 10.0,
        description = description,
    )

    @Test
    fun `header line lists every column`() {
        val csv = CsvExporter.mealsToCsv(emptyList())
        assertEquals("date,repas,calories,proteines_g,glucides_g,lipides_g,description\n", csv)
    }

    @Test
    fun `one row per meal, in order`() {
        val csv = CsvExporter.mealsToCsv(listOf(meal("Poulet riz"), meal("Œufs")))
        val lines = csv.trim().lines()
        assertEquals(3, lines.size) // header + 2 rows
        assertTrue(lines[1].contains("Poulet riz"))
        assertTrue(lines[2].contains("Œufs"))
    }

    @Test
    fun `a description containing a comma gets quoted`() {
        val csv = CsvExporter.mealsToCsv(listOf(meal("Poulet, riz, brocolis")))
        assertTrue(csv.contains("\"Poulet, riz, brocolis\""))
    }

    @Test
    fun `a description containing a quote is escaped by doubling it`() {
        val csv = CsvExporter.mealsToCsv(listOf(meal("Barre \"protéinée\"")))
        assertTrue(csv.contains("\"Barre \"\"protéinée\"\"\""))
    }

    @Test
    fun `a null description becomes an empty field, not the literal null`() {
        val csv = CsvExporter.mealsToCsv(listOf(meal(null)))
        assertTrue(csv.trim().lines()[1].endsWith(","))
    }
}
