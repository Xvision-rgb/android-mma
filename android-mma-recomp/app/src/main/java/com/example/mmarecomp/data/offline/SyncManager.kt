package com.example.mmarecomp.data.offline

import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NewDailyCheckIn
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.model.NewMmaSession
import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.serialization.json.Json

data class SyncResult(
    val synced: Int,
    val failed: Int,
    val abandoned: Int,
    val remaining: Int,
)

/** Traite la file d'attente des écritures en attente de réseau. */
class SyncManager(
    private val offline: OfflineCoordinator,
    private val workoutRepository: WorkoutRepository = WorkoutRepository(offline),
    private val mealRepository: MealRepository = MealRepository(offline),
    private val weighInRepository: WeighInRepository = WeighInRepository(offline),
    private val dailyCheckInRepository: DailyCheckInRepository = DailyCheckInRepository(offline),
    private val mmaSessionRepository: MmaSessionRepository = MmaSessionRepository(offline),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun offlinePendingCount(): Int {
        offline.refreshPendingCount()
        return offline.pendingCount.value
    }

    /** Check-ins locaux encore en outbox — pour hydrater l'UI après restart. */
    suspend fun pendingLocalCheckIns(): List<DailyCheckIn> =
        offline.pendingEntries()
            .filter { it.entityType == SyncEntityType.CHECK_IN && it.operation == SyncOperation.INSERT }
            .mapNotNull { entry ->
                runCatching {
                    val payload = json.decodeFromString<NewDailyCheckIn>(entry.payloadJson)
                    DailyCheckIn(
                        id = entry.id,
                        userId = "",
                        date = payload.date,
                        sommeil = payload.sommeil,
                        courbatures = payload.courbatures,
                        fatigue = payload.fatigue,
                        humeur = payload.humeur,
                        stress = payload.stress,
                        hrvRmssd = payload.hrvRmssd,
                        deadHangSec = payload.deadHangSec,
                    )
                }.getOrNull()
            }

    suspend fun pendingLocalMeals(): List<Meal> =
        offline.pendingEntries()
            .filter { it.entityType == SyncEntityType.MEAL && it.operation == SyncOperation.INSERT }
            .mapNotNull { entry ->
                runCatching {
                    val payload = json.decodeFromString<NewMeal>(entry.payloadJson)
                    Meal(
                        id = entry.id,
                        userId = "",
                        date = payload.date,
                        repas = payload.repas,
                        calories = payload.calories,
                        proteinesG = payload.proteinesG,
                        glucidesG = payload.glucidesG,
                        lipidesG = payload.lipidesG,
                        description = payload.description,
                    )
                }.getOrNull()
            }

    suspend fun pendingLocalWorkouts(): List<Workout> =
        offline.pendingEntries()
            .filter { it.entityType == SyncEntityType.WORKOUT && it.operation == SyncOperation.INSERT }
            .mapNotNull { entry ->
                runCatching {
                    val payload = json.decodeFromString<NewWorkout>(entry.payloadJson)
                    Workout(
                        id = entry.id,
                        userId = "",
                        date = payload.date,
                        type = payload.type,
                        exercices = payload.exercices,
                        dureeMin = payload.dureeMin,
                        rpe = payload.rpe,
                        notes = payload.notes,
                    )
                }.getOrNull()
            }

    suspend fun pendingLocalWeighIns(): List<WeighIn> =
        offline.pendingEntries()
            .filter { it.entityType == SyncEntityType.WEIGH_IN && it.operation == SyncOperation.INSERT }
            .mapNotNull { entry ->
                runCatching {
                    val payload = json.decodeFromString<NewWeighIn>(entry.payloadJson)
                    WeighIn(
                        id = entry.id,
                        userId = "",
                        date = payload.date,
                        heure = payload.heure,
                        type = payload.type,
                        poidsKg = payload.poidsKg,
                        bfPct = payload.bfPct,
                        contexte = payload.contexte,
                    )
                }.getOrNull()
            }

    suspend fun pendingLocalMmaSessions(): List<MmaSession> =
        offline.pendingEntries()
            .filter { it.entityType == SyncEntityType.MMA_SESSION && it.operation == SyncOperation.INSERT }
            .mapNotNull { entry ->
                runCatching {
                    val payload = json.decodeFromString<NewMmaSession>(entry.payloadJson)
                    MmaSession(
                        id = entry.id,
                        userId = "",
                        date = payload.date,
                        wodContent = payload.wodContent,
                        roundsSets = payload.roundsSets,
                        ressenti = payload.ressenti,
                        notesTechnique = payload.notesTechnique,
                    )
                }.getOrNull()
            }

    suspend fun syncAll(): SyncResult {
        var synced = 0
        var failed = 0
        var abandoned = 0
        val entries = offline.pendingEntries()
        for (entry in entries) {
            if (entry.retryCount >= OfflineCoordinator.MAX_RETRY) {
                offline.removeOutboxEntry(entry.id)
                abandoned++
                continue
            }
            val result = runCatching {
                when (entry.entityType) {
                    SyncEntityType.WORKOUT -> when (entry.operation) {
                        SyncOperation.INSERT -> {
                            val payload = json.decodeFromString<NewWorkout>(entry.payloadJson)
                            workoutRepository.logRemote(payload)
                        }
                        SyncOperation.DELETE -> {
                            val id = json.decodeFromString<DeletePayload>(entry.payloadJson).id
                            workoutRepository.deleteRemote(id)
                        }
                        else -> error("Opération inconnue: ${entry.operation}")
                    }
                    SyncEntityType.MEAL -> when (entry.operation) {
                        SyncOperation.INSERT -> {
                            val payload = json.decodeFromString<NewMeal>(entry.payloadJson)
                            mealRepository.logRemote(payload)
                        }
                        SyncOperation.DELETE -> {
                            val id = json.decodeFromString<DeletePayload>(entry.payloadJson).id
                            mealRepository.deleteRemote(id)
                        }
                        else -> error("Opération inconnue: ${entry.operation}")
                    }
                    SyncEntityType.WEIGH_IN -> when (entry.operation) {
                        SyncOperation.INSERT -> {
                            val payload = json.decodeFromString<NewWeighIn>(entry.payloadJson)
                            weighInRepository.logRemote(payload)
                        }
                        SyncOperation.DELETE -> {
                            val id = json.decodeFromString<DeletePayload>(entry.payloadJson).id
                            weighInRepository.deleteRemote(id)
                        }
                        else -> error("Opération inconnue: ${entry.operation}")
                    }
                    SyncEntityType.CHECK_IN -> when (entry.operation) {
                        SyncOperation.INSERT -> {
                            val payload = json.decodeFromString<NewDailyCheckIn>(entry.payloadJson)
                            dailyCheckInRepository.logRemote(payload)
                        }
                        else -> error("Opération inconnue: ${entry.operation}")
                    }
                    SyncEntityType.MMA_SESSION -> when (entry.operation) {
                        SyncOperation.INSERT -> {
                            val payload = json.decodeFromString<NewMmaSession>(entry.payloadJson)
                            mmaSessionRepository.logRemote(payload)
                        }
                        SyncOperation.DELETE -> {
                            val id = json.decodeFromString<DeletePayload>(entry.payloadJson).id
                            mmaSessionRepository.deleteRemote(id)
                        }
                        else -> error("Opération inconnue: ${entry.operation}")
                    }
                    else -> error("Type inconnu: ${entry.entityType}")
                }
            }
            if (result.isSuccess) {
                offline.removeOutboxEntry(entry.id)
                synced++
            } else {
                val error = result.exceptionOrNull()
                if (error != null) {
                    rethrowCancellation(error)
                    // Erreur schéma / métier : abandonner plutôt que retenter indéfiniment.
                    if (!error.isOfflineEnqueueable()) {
                        offline.removeOutboxEntry(entry.id)
                        abandoned++
                        continue
                    }
                }
                offline.incrementRetry(entry.id)
                failed++
            }
        }
        offline.refreshPendingCount()
        return SyncResult(
            synced = synced,
            failed = failed,
            abandoned = abandoned,
            remaining = offline.pendingCount.value,
        )
    }
}

@kotlinx.serialization.Serializable
data class DeletePayload(val id: String)

@kotlinx.serialization.Serializable
data class WorkoutListCache(val items: List<com.example.mmarecomp.model.Workout>)

@kotlinx.serialization.Serializable
data class MealListCache(val items: List<com.example.mmarecomp.model.Meal>)

@kotlinx.serialization.Serializable
data class WeighInListCache(val items: List<com.example.mmarecomp.model.WeighIn>)

@kotlinx.serialization.Serializable
data class CheckInListCache(val items: List<com.example.mmarecomp.model.DailyCheckIn>)

@kotlinx.serialization.Serializable
data class MmaSessionListCache(val items: List<com.example.mmarecomp.model.MmaSession>)
