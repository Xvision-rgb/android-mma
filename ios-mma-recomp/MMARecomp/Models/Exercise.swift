import Foundation

/// Exercice tel que programmé dans le split hebdo (training_plan.exercices).
struct PlannedExercise: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var nom: String
    var series: Int
    var reps: Int
    var chargeCibleKg: Double?

    enum CodingKeys: String, CodingKey {
        case nom, series, reps
        case chargeCibleKg = "charge_cible_kg"
    }
}

/// Exercice tel que loggué après une séance réelle (workouts.exercices).
struct LoggedExercise: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var nom: String
    var series: Int
    var reps: Int
    var chargeCibleKg: Double?
    var chargeReelleKg: Double?
    var repsReelles: Int?
    /// Toutes les reps faites proprement -> déclenche la suggestion "+2.5kg".
    var propre: Bool = false

    enum CodingKeys: String, CodingKey {
        case nom, series, reps, propre
        case chargeCibleKg = "charge_cible_kg"
        case chargeReelleKg = "charge_reelle_kg"
        case repsReelles = "reps_reelles"
    }

    var suggestionProgression: Double? {
        guard propre, let cible = chargeCibleKg else { return nil }
        return cible + 2.5
    }
}
