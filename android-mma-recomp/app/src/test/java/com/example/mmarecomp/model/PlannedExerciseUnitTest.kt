package com.example.mmarecomp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannedExerciseUnitTest {

    @Test
    fun `formatPrescription selon unite`() {
        assertEquals("4x8", PlannedExercise("Squat", 4, 8).formatPrescription())
        assertEquals(
            "3x45s",
            PlannedExercise("Hang", 3, 45, unite = PlannedExerciseUnit.Secondes).formatPrescription(),
        )
        assertEquals(
            "20 min",
            PlannedExercise("Course", 1, 20, unite = PlannedExerciseUnit.Minutes).formatPrescription(),
        )
        assertEquals(
            "2x40m",
            PlannedExercise("Farmer", 2, 40, unite = PlannedExerciseUnit.Metres).formatPrescription(),
        )
    }

    @Test
    fun `toLogged mappe minutes et metres en cardio`() {
        val cardioMin = PlannedExercise("Course", 1, 25, unite = PlannedExerciseUnit.Minutes).toLogged()
        assertTrue(cardioMin.isCardio)
        assertEquals(25, cardioMin.dureeMin)

        val cardioM = PlannedExercise("Farmer", 2, 40, unite = PlannedExerciseUnit.Metres).toLogged()
        assertTrue(cardioM.isCardio)
        assertEquals(0.08, cardioM.distanceKm!!, 0.0001)

        val iso = PlannedExercise("Hang", 3, 45, unite = PlannedExerciseUnit.Secondes).toLogged()
        assertEquals(ExerciseModality.Strength, iso.modality)
        assertEquals(3, iso.sets.size)
        assertEquals(45, iso.sets.first().reps)
    }
}
