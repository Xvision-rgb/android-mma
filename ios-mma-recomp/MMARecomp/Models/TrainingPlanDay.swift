import Foundation

/// Une journée du split hebdo programmé (training_plan).
struct TrainingPlanDay: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    /// 1 = lundi ... 7 = dimanche
    var jourSemaine: Int
    var type: PlanDayType
    var exercices: [PlannedExercise]
    var phase: Phase
    var notes: String?
    var actif: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case jourSemaine = "jour_semaine"
        case type, exercices, phase, notes, actif
    }
}

extension TrainingPlanDay {
    static let joursLabels: [Int: String] = [
        1: "Lundi", 2: "Mardi", 3: "Mercredi", 4: "Jeudi",
        5: "Vendredi", 6: "Samedi", 7: "Dimanche",
    ]
}

struct NewTrainingPlanDay: Codable {
    var jourSemaine: Int
    var type: PlanDayType
    var exercices: [PlannedExercise]
    var phase: Phase
    var notes: String?

    enum CodingKeys: String, CodingKey {
        case jourSemaine = "jour_semaine"
        case type, exercices, phase, notes
    }
}
