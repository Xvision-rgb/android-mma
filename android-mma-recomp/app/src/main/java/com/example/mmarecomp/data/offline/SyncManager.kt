package com.example.mmarecomp.data.offline

import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.NewDailyCheckIn
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.NewWorkout
import kotlinx.serialization.json.Json

data class SyncResult(
    val synced: Int,
    val failed: Int,
    val remaining: Int,
)

/** Traite la file d'attente des écritures en attente de réseau. */
class SyncManager(
    private val offline: OfflineCoordinator,
    private val workoutRepository: WorkoutRepository = WorkoutRepository(offline),
    private val mealRepository: MealRepository = MealRepository(offline),
    private val weighInRepository: WeighInRepository = WeighInRepository(offline),
    private val dailyCheckInRepository: DailyCheckInRepository = DailyCheckInRepository(offline),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun offlinePendingCount(): Int {
        offline.refreshPendingCount()
        return offline.pendingCount.value
    }

    suspend fun syncAll(): SyncResult {
        var synced = 0
        var failed = 0
        val entries = offline.pendingEntries()
        for (entry in entries) {
            val ok = runCatching {
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
                    else -> error("Type inconnu: ${entry.entityType}")
                }
            }.isSuccess
            if (ok) {
                offline.removeOutboxEntry(entry.id)
                synced++
            } else {
                offline.incrementRetry(entry.id)
                failed++
            }
        }
        offline.refreshPendingCount()
        return SyncResult(synced = synced, failed = failed, remaining = offline.pendingCount.value)
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
