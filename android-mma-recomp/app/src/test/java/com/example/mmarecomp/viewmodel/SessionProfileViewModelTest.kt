package com.example.mmarecomp.viewmodel

import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionProfileViewModelTest {

    private fun profile(phase: Phase) = Profile(
        id = "u",
        poidsObjectifKg = 75.0,
        bfObjectifPct = 12.0,
        phase = phase,
        objectifCalorieMode = CalorieMode.Recomposition,
    )

    @Test
    fun `la phase chargee n est jamais Ete avant la fin du chargement`() = runBlocking {
        var phasesVues = mutableListOf<Phase?>()
        val vm = SessionProfileViewModel(userId = "u") {
            profile(Phase.CurriculumMma)
        }
        phasesVues += when (val s = vm.phaseState) {
            is PhaseState.Ready -> s.phase
            else -> null
        }
        assertTrue(phasesVues.none { it == Phase.Ete })
        vm.loadProfile()
        val ready = vm.phaseState as PhaseState.Ready
        assertEquals(Phase.CurriculumMma, ready.phase)
        assertNotEquals(Phase.Ete, ready.phase)
    }

    @Test
    fun `une erreur de chargement n impose pas une phase par defaut`() = runBlocking {
        val vm = SessionProfileViewModel(userId = "u") {
            throw java.io.IOException("offline")
        }
        vm.loadProfile()
        assertTrue(vm.phaseState is PhaseState.Error)
    }
}
