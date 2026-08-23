import Foundation

@MainActor
final class MealLogViewModel: ObservableObject {
    @Published var date: Date = Date()
    @Published var mealsForDay: [Meal] = []
    @Published var target: NutritionTarget?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let mealService = MealService()
    private let targetService = NutritionTargetService()

    var totalCalories: Int { mealsForDay.reduce(0) { $0 + $1.calories } }
    var totalProteines: Double { mealsForDay.reduce(0) { $0 + $1.proteinesG } }

    var indicativeSplit: [RepasSlot: (calories: Int, proteinesG: Double)] {
        guard let target else { return [:] }
        return NutritionTargetCalculator.indicativeSplit(
            calories: target.caloriesCible,
            proteinesG: target.proteinesCibleG,
            slots: RepasSlot.allCases
        )
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        let dateString = DateUtils.string(from: date)
        do {
            async let meals = mealService.fetch(forDate: dateString)
            async let existingTarget = targetService.fetch(forDate: dateString)
            mealsForDay = try await meals
            target = try await existingTarget
        } catch {
            errorMessage = "Impossible de charger les repas du jour."
        }
    }

    func setTarget(typeJour: TypeJour) async {
        let dateString = DateUtils.string(from: date)
        let computed = NutritionTargetCalculator.target(for: typeJour)
        let newTarget = NewNutritionTarget(
            date: dateString,
            typeJour: typeJour,
            caloriesCible: computed.calories,
            proteinesCibleG: computed.proteinesG
        )
        target = try? await targetService.set(newTarget)
    }

    func logMeal(
        slot: RepasSlot,
        calories: Int,
        proteinesG: Double,
        glucidesG: Double,
        lipidesG: Double,
        description: String
    ) async -> Bool {
        let newMeal = NewMeal(
            date: DateUtils.string(from: date),
            repas: slot.rawValue,
            calories: calories,
            proteinesG: proteinesG,
            glucidesG: glucidesG,
            lipidesG: lipidesG,
            description: description.isEmpty ? nil : description
        )
        do {
            let saved = try await mealService.log(newMeal)
            mealsForDay.removeAll { $0.repas == slot.rawValue }
            mealsForDay.append(saved)
            mealsForDay.sort { $0.repas < $1.repas }
            return true
        } catch {
            errorMessage = "Impossible d'enregistrer ce repas."
            return false
        }
    }

    /// Alerte douce : plusieurs jours d'affilée nettement en dessous de la
    /// cible. Ne culpabilise jamais un jour isolé en dessous de l'objectif.
    func softUnderTargetAlert(recentDailyTotals: [(date: String, calories: Int, cible: Int)]) -> Bool {
        let lastThree = recentDailyTotals.suffix(3)
        guard lastThree.count == 3 else { return false }
        return lastThree.allSatisfy { $0.calories < Int(Double($0.cible) * 0.85) }
    }
}
