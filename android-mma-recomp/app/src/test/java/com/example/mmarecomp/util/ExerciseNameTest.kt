package com.example.mmarecomp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseNameTest {

    /** Le cas observé en production : la carte force relative affichait
     *  quatre lignes pour deux mouvements, avec deux 1RM contradictoires
     *  pour le développé couché. */
    @Test
    fun `quatre saisies du meme couple de mouvements se regroupent en deux`() {
        val saisies = listOf("développé couché", "Rowing barre", "Développé couché", "rowing barre ")
        assertEquals(2, saisies.groupBy { ExerciseName.cle(it) }.size)
    }

    @Test
    fun `la casse est ignoree`() {
        assertTrue(ExerciseName.memeExercice("développé couché", "Développé couché"))
        assertTrue(ExerciseName.memeExercice("SQUAT", "squat"))
    }

    @Test
    fun `les espaces de bordure sont ignores`() {
        assertTrue(ExerciseName.memeExercice("rowing barre ", "rowing barre"))
        assertTrue(ExerciseName.memeExercice("  rowing barre", "rowing barre"))
    }

    @Test
    fun `les espaces internes multiples sont reduits`() {
        assertTrue(ExerciseName.memeExercice("développé   couché", "développé couché"))
    }

    /** « é » précomposé (U+00E9) contre « e » + accent combinant (U+0301) :
     *  identiques à l'œil, différents pour equals sans normalisation. */
    @Test
    fun `les accents composes et precomposes sont unifies`() {
        val precompose = "développé couché"
        val decompose = "développé couché"
        assertTrue(ExerciseName.memeExercice(precompose, decompose))
    }

    @Test
    fun `deux mouvements differents restent distincts`() {
        assertFalse(ExerciseName.memeExercice("développé couché", "développé militaire"))
        assertFalse(ExerciseName.memeExercice("squat", "squat bulgare"))
    }

    @Test
    fun `propre preserve la casse choisie par l utilisateur`() {
        assertEquals("Développé Couché", ExerciseName.propre("  Développé  Couché "))
        assertEquals("DÉVELOPPÉ", ExerciseName.propre("DÉVELOPPÉ"))
    }
}
