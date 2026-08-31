package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlanCreneau
import com.example.mmarecomp.model.PlannedExerciseUnit
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

    @Test
    fun `parse secondes minutes et metres`() {
        val result = TrainingPlanParser.parse(
            """
            Lundi
            Dead hang 3x45s
            Course 20 min
            Farmer walk 2x40m
            """.trimIndent(),
        )
        val exos = result.days.single().exercices
        assertEquals(3, exos.size)
        assertEquals(PlannedExerciseUnit.Secondes, exos[0].unite)
        assertEquals(45, exos[0].reps)
        assertEquals(PlannedExerciseUnit.Minutes, exos[1].unite)
        assertEquals(20, exos[1].reps)
        assertEquals(1, exos[1].series)
        assertEquals(PlannedExerciseUnit.Metres, exos[2].unite)
        assertEquals(40, exos[2].reps)
    }

    @Test
    fun `deux creneaux le meme jour`() {
        val result = TrainingPlanParser.parse(
            """
            Lundi — Matin
            Squat 4x8 @80kg
            Lundi — Soir
            Dead hang 3x45s
            Course 20 min
            """.trimIndent(),
        )
        assertEquals(2, result.days.size)
        val matin = result.days.first { it.creneau == PlanCreneau.Matin }
        val soir = result.days.first { it.creneau == PlanCreneau.Soir }
        assertEquals(1, matin.jourSemaine)
        assertEquals("Squat", matin.exercices.single().nom)
        assertEquals(1, soir.jourSemaine)
        assertEquals(2, soir.exercices.size)
        assertEquals(PlannedExerciseUnit.Secondes, soir.exercices[0].unite)
    }

    @Test
    fun `sous titre soir sous un jour`() {
        val result = TrainingPlanParser.parse(
            """
            Mardi
            Développé couché 3x8
            Soir
            Pompes 3x15
            """.trimIndent(),
        )
        assertEquals(2, result.days.size)
        assertEquals(PlanCreneau.Matin, result.days[0].creneau)
        assertEquals(PlanCreneau.Soir, result.days[1].creneau)
        assertEquals("Pompes", result.days[1].exercices.single().nom)
    }
}
