import Foundation

@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var planThisWeek: [TrainingPlanDay] = []
    @Published var workoutsThisWeek: [Workout] = []
    @Published var mealsLast7Days: [Meal] = []
    @Published var morningWeighIns: [WeighIn] = []
    @Published var todayTarget: NutritionTarget?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let trainingPlanService = TrainingPlanService()
    private let workoutService = WorkoutService()
    private let mealService = MealService()
    private let weighInService = WeighInService()
    private let nutritionTargetService = NutritionTargetService()

    var avgCaloriesLast7Days: Int {
        guard !mealsLast7Days.isEmpty else { return 0 }
        let total = mealsLast7Days.reduce(0) { $0 + $1.calories }
        let days = Set(mealsLast7Days.map(\.date)).count
        guard days > 0 else { return 0 }
        return total / days
    }

    var weightTrend7Day: [(date: Date, value: Double)] {
        let points = morningWeighIns.compactMap { weighIn -> (date: Date, value: Double)? in
            guard let date = DateUtils.date(from: weighIn.date) else { return nil }
            return (date, weighIn.poidsKg)
        }
        return MovingAverage.sevenDay(points: points)
    }

    var seancesFaitesCount: Int { workoutsThisWeek.count }
    var seancesPlanifieesCount: Int { planThisWeek.filter { $0.type != .repos }.count }

    var plateauStatus: PlateauStatus {
        let points = morningWeighIns.compactMap { weighIn -> (date: Date, poidsKg: Double)? in
            guard let d = DateUtils.date(from: weighIn.date) else { return nil }
            return (d, weighIn.poidsKg)
        }
        // Sans historique de charges chargé ici, on reste positif par défaut ;
        // ProgressViewModel affine ce signal avec les vraies charges loggées.
        return PlateauDetector.detect(morningWeighIns: points, performanceTrendUp: true)
    }

    func load(phase: Phase) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let mondayOfWeek = DateUtils.startOfWeek()
        let sevenDaysAgo = DateUtils.daysAgo(7)
        let today = DateUtils.today()

        do {
            async let plan = trainingPlanService.fetchWeek(phase: phase)
            async let workouts = workoutService.fetchWeek(from: mondayOfWeek)
            async let meals = mealService.fetch(since: sevenDaysAgo)
            async let weighIns = weighInService.fetch(since: sevenDaysAgo)
            async let target = nutritionTargetService.fetch(forDate: today)

            planThisWeek = try await plan
            workoutsThisWeek = try await workouts
            mealsLast7Days = try await meals
            morningWeighIns = try await weighIns.filter { $0.type == .matinJeun }
            todayTarget = try await target
        } catch {
            errorMessage = "Impossible de charger le dashboard pour le moment."
        }
    }
}
