import Foundation

@MainActor
final class WorkoutLogViewModel: ObservableObject {
    @Published var date: Date = Date()
    @Published var type: WorkoutType = .jambesForce
    @Published var exercices: [LoggedExercise] = []
    @Published var dureeMin: String = ""
    @Published var notes: String = ""
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var lastSaved: Workout?

    private let workoutService = WorkoutService()
    private let trainingPlanService: TrainingPlanService

    init(trainingPlanService: TrainingPlanService = TrainingPlanService()) {
        self.trainingPlanService = trainingPlanService
    }

    /// Pré-remplit la séance depuis le split programmé pour le jour choisi.
    func loadPlan(phase: Phase) async {
        let jour = DateUtils.weekdayISO(from: DateUtils.string(from: date))
        guard let plan = try? await trainingPlanService.fetchWeek(phase: phase)
            .first(where: { $0.jourSemaine == jour }) else { return }
        type = WorkoutType(rawValue: plan.type.rawValue) ?? type
        exercices = plan.exercices.map {
            LoggedExercise(nom: $0.nom, series: $0.series, reps: $0.reps, chargeCibleKg: $0.chargeCibleKg)
        }
    }

    func addExercise() {
        exercices.append(LoggedExercise(nom: "", series: 3, reps: 10, chargeCibleKg: nil))
    }

    func removeExercise(at offsets: IndexSet) {
        exercices.remove(atOffsets: offsets)
    }

    func save() async -> Bool {
        errorMessage = nil
        isSaving = true
        defer { isSaving = false }

        let newWorkout = NewWorkout(
            date: DateUtils.string(from: date),
            type: type,
            exercices: exercices,
            dureeMin: Int(dureeMin),
            notes: notes.isEmpty ? nil : notes
        )

        do {
            lastSaved = try await workoutService.log(newWorkout)
            return true
        } catch {
            errorMessage = "Impossible d'enregistrer la séance."
            return false
        }
    }
}
