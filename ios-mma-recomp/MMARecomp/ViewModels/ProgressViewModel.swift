import Foundation

@MainActor
final class ProgressViewModel: ObservableObject {
    @Published var weighIns: [WeighIn] = []
    @Published var workouts: [Workout] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var windowWeeks: Int = 4

    private let weighInService = WeighInService()
    private let workoutService = WorkoutService()

    var weightTrend: [(date: Date, value: Double)] {
        let points = weighIns
            .filter { $0.type == .matinJeun }
            .compactMap { w -> (date: Date, value: Double)? in
                guard let d = DateUtils.date(from: w.date) else { return nil }
                return (d, w.poidsKg)
            }
        return MovingAverage.sevenDay(points: points)
    }

    var bfTrend: [(date: Date, value: Double)] {
        let points = weighIns
            .filter { $0.type == .matinJeun }
            .compactMap { w -> (date: Date, value: Double)? in
                guard let d = DateUtils.date(from: w.date), let bf = w.bfPct else { return nil }
                return (d, bf)
            }
        return MovingAverage.sevenDay(points: points)
    }

    /// Progression de charge par exercice sur la fenêtre sélectionnée.
    var chargeProgressionByExercise: [String: [(date: Date, chargeKg: Double)]] {
        var result: [String: [(date: Date, chargeKg: Double)]] = [:]
        for workout in workouts {
            guard let date = DateUtils.date(from: workout.date) else { continue }
            for exercice in workout.exercices {
                guard let charge = exercice.chargeReelleKg else { continue }
                result[exercice.nom, default: []].append((date, charge))
            }
        }
        for key in result.keys {
            result[key]?.sort { $0.date < $1.date }
        }
        return result
    }

    var performanceTrendUp: Bool {
        chargeProgressionByExercise.values.contains { series in
            guard let first = series.first?.chargeKg, let last = series.last?.chargeKg else { return false }
            return last > first
        }
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        let since = DateUtils.daysAgo(windowWeeks * 7)
        do {
            async let w = weighInService.fetch(since: since)
            async let wo = workoutService.fetchWeek(from: since)
            weighIns = try await w
            workouts = try await wo
        } catch {
            errorMessage = "Impossible de charger la progression."
        }
    }
}
