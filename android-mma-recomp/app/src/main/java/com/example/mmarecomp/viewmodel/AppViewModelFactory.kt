package com.example.mmarecomp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncManager

class AppViewModelFactory(
    private val application: Application,
    private val userId: String,
) : ViewModelProvider.Factory {

    private val offline: OfflineCoordinator by lazy { OfflineCoordinator.get(application) }
    private val syncManager: SyncManager by lazy { SyncManager(offline) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SessionProfileViewModel::class.java) ->
                SessionProfileViewModel(userId) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    userId = userId,
                    trainingPlanRepository = TrainingPlanRepository(),
                    workoutRepository = WorkoutRepository(offline),
                    mealRepository = MealRepository(offline),
                    weighInRepository = WeighInRepository(offline),
                    nutritionTargetRepository = NutritionTargetRepository(),
                    profileRepository = ProfileRepository(),
                    dailyCheckInRepository = DailyCheckInRepository(offline),
                    mmaSessionRepository = MmaSessionRepository(),
                    context = application,
                    syncManager = syncManager,
                ) as T
            modelClass.isAssignableFrom(WorkoutLogViewModel::class.java) ->
                WorkoutLogViewModel(
                    workoutRepository = WorkoutRepository(offline),
                ) as T
            modelClass.isAssignableFrom(MealLogViewModel::class.java) ->
                MealLogViewModel(
                    userId = userId,
                    mealRepository = MealRepository(offline),
                    weighInRepository = WeighInRepository(offline),
                    workoutRepository = WorkoutRepository(offline),
                    context = application,
                ) as T
            modelClass.isAssignableFrom(WeighInViewModel::class.java) ->
                WeighInViewModel(
                    repository = WeighInRepository(offline),
                ) as T
            modelClass.isAssignableFrom(ProgressViewModel::class.java) ->
                ProgressViewModel(
                    weighInRepository = WeighInRepository(offline),
                    workoutRepository = WorkoutRepository(offline),
                    mealRepository = MealRepository(offline),
                    dailyCheckInRepository = DailyCheckInRepository(offline),
                    nutritionTargetRepository = NutritionTargetRepository(),
                    mmaSessionRepository = MmaSessionRepository(),
                    context = application,
                ) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(userId) as T
            modelClass.isAssignableFrom(CalorieGoalViewModel::class.java) ->
                CalorieGoalViewModel(userId = userId, context = application) as T
            modelClass.isAssignableFrom(ImportTrainingPlanViewModel::class.java) ->
                ImportTrainingPlanViewModel(context = application) as T
            modelClass.isAssignableFrom(WeeklyProgramViewModel::class.java) ->
                WeeklyProgramViewModel() as T
            modelClass.isAssignableFrom(TrainingPlanEditViewModel::class.java) ->
                TrainingPlanEditViewModel() as T
            modelClass.isAssignableFrom(MmaSessionViewModel::class.java) ->
                MmaSessionViewModel() as T
            else -> throw IllegalArgumentException("ViewModel inconnu: ${modelClass.name}")
        }
    }
}
