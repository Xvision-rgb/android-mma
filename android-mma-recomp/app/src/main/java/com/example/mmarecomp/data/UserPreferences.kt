package com.example.mmarecomp.data

import kotlinx.serialization.Serializable

@Serializable
enum class WeightUnit(val label: String) { KG("kg"), LB("lb") }

@Serializable
enum class MovingAverageWindow(val days: Int, val label: String) {
    FIVE(5, "5 jours"),
    SEVEN(7, "7 jours"),
    TEN(10, "10 jours"),
}

@Serializable
enum class WeekStart(val label: String) { MONDAY("Lundi"), SUNDAY("Dimanche") }

@Serializable
enum class TextScale(val multiplier: Float, val label: String) {
    SMALL(0.9f, "Petit"),
    NORMAL(1f, "Normal"),
    LARGE(1.15f, "Grand"),
}

@Serializable
enum class AccentPreset(val label: String) {
    STEEL("Acier"),
    CLAY("Argile"),
    MOSS("Mousse"),
    EMBER("Braise"),
    OCEAN("Océan"),
}

@Serializable
enum class DefaultTab(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    WORKOUT("workout", "Séance"),
    MEALS("meals", "Repas"),
    WEIGHIN("weighin", "Pesée"),
    PROGRESS("progress", "Progression"),
}

@Serializable
enum class DashboardCard(val label: String) {
    SEANCES("Séances"),
    CONSTANCE("Constance"),
    POIDS("Tendance poids"),
    NUTRITION("Nutrition"),
}

@Serializable
data class SeriesReps(val series: Int, val reps: Int)

@Serializable
data class QuickAddSnack(val name: String, val calories: Int, val proteinesG: Double)

/**
 * Préférences propres à cet appareil (pas des données de suivi à
 * synchroniser), stockées en local — voir [UserPreferencesStore]. Tous les
 * champs ont une valeur par défaut qui reproduit le comportement actuel de
 * l'app, pour qu'une installation existante reste inchangée après mise à jour.
 */
@Serializable
data class UserPreferences(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val movingAverageWindow: MovingAverageWindow = MovingAverageWindow.SEVEN,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val showBodyFat: Boolean = true,
    val textScale: TextScale = TextScale.NORMAL,
    val accent: AccentPreset = AccentPreset.STEEL,
    val defaultTab: DefaultTab = DefaultTab.DASHBOARD,
    val visibleDashboardCards: Set<DashboardCard> = DashboardCard.entries.toSet(),
    val defaultWorkoutType: String? = null,
    val autoNutritionTargets: Boolean = true,
    val showVoiceInput: Boolean = true,
    val celebratePrWithVibration: Boolean = true,
    val confirmBeforeDelete: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 19,
    val dailyReminderMinute: Int = 0,
    val appLockEnabled: Boolean = false,
    val phaseLabelOverrides: Map<String, String> = emptyMap(),
    val mealSlotLabelOverrides: Map<String, String> = emptyMap(),
    val restTimerEnabled: Boolean = false,
    val restTimerSeconds: Int = 90,
    val defaultSeriesRepsByType: Map<String, SeriesReps> = emptyMap(),
    val autoFillLastDuration: Boolean = false,
    val showExerciseHistory: Boolean = false,
    val nutritionTargetByPhase: Boolean = false,
    val hydrationEnabled: Boolean = false,
    val macroSplitEnabled: Boolean = false,
    val macroProteinPct: Int = 30,
    val macroCarbsPct: Int = 40,
    val macroFatPct: Int = 30,
    val quickAddSnacks: List<QuickAddSnack> = listOf(
        QuickAddSnack("Shake protéiné", 200, 25.0),
        QuickAddSnack("Fruit", 100, 1.0),
    ),
    val weightGoalEtaEnabled: Boolean = false,
    val plateauSensitivityDays: Int = 14,
    val showTargetsAsPercent: Boolean = false,
)
