package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.Workout
import java.time.LocalDate

/** Calcule la modulation du jour à partir du check-in et de la charge récente. */
object ReadinessCalculator {
    fun modulation(
        checkInToday: DailyCheckIn?,
        checkInsRecents: List<DailyCheckIn>,
        workouts: List<Workout>,
        mmaSessions: List<MmaSession> = emptyList(),
        today: LocalDate = LocalDate.now(),
    ): ModulationSeance {
        val acwr = TrainingLoad.acwr(TrainingLoad.chargesParJour(workouts, mmaSessions), today)
        val score = checkInToday?.score
        val ecartHrv = TrainingLoad.ecartHrvEnSigma(checkInsRecents, today)
        val joursRouge = TrainingLoad.joursConsecutifsEnRouge(checkInsRecents, today)
        return TrainingLoad.moduler(score, acwr, ecartHrv, joursRouge)
    }
}
