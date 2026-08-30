package com.example.mmarecomp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPlanParserTest {

    @Test
    fun `parse une charge avec virgule decimale`() {
        val result = TrainingPlanParser.parse("Lundi\nSquat 3x5 @82,5kg")
        assertEquals(1, result.days.size)
        assertEquals(82.5, result.days.first().exercices.first().chargeCibleKg!!, 0.001)
    }

    @Test
    fun `un nouveau jour avec exercices n est pas ignore`() {
        val result = TrainingPlanParser.parse("Mardi\nDéveloppé couché 3x8 @60kg")
        assertEquals(2, result.days.first().jourSemaine)
        assertTrue(result.days.first().exercices.isNotEmpty())
    }

    @Test
    fun `une ligne non reconnue apparait dans ignoredLines`() {
        val result = TrainingPlanParser.parse("Lundi\nSquat 3x5\nrepos actif")
        assertTrue(result.ignoredLines.contains("repos actif"))
    }

    @Test
    fun `la virgule entre exercices separe toujours les segments`() {
        val result = TrainingPlanParser.parse("Lundi\nSquat 3x5 @100kg, Rowing 3x8")
        val exos = result.days.first().exercices
        assertEquals(2, exos.size)
        assertEquals("Squat", exos[0].nom)
        assertEquals("Rowing", exos[1].nom)
    }
}
