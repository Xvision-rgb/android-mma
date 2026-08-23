import Foundation

enum WorkoutType: String, Codable, CaseIterable, Identifiable {
    case jambesForce = "jambes_force"
    case torseForce = "torse_force"
    case jambesHypertrophie = "jambes_hypertrophie"
    case torseHypertrophie = "torse_hypertrophie"
    case hiit
    case mmaWod = "mma_wod"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .jambesForce: return "Jambes force"
        case .torseForce: return "Torse force"
        case .jambesHypertrophie: return "Jambes hypertrophie"
        case .torseHypertrophie: return "Torse hypertrophie"
        case .hiit: return "HIIT"
        case .mmaWod: return "MMA (WOD)"
        }
    }
}

/// Type de jour dans le split hebdo programmé — inclut "repos", contrairement
/// à WorkoutType qui ne couvre que les séances effectivement logguées.
enum PlanDayType: String, Codable, CaseIterable, Identifiable {
    case jambesForce = "jambes_force"
    case torseForce = "torse_force"
    case jambesHypertrophie = "jambes_hypertrophie"
    case torseHypertrophie = "torse_hypertrophie"
    case hiit
    case mmaWod = "mma_wod"
    case repos

    var id: String { rawValue }

    var label: String {
        switch self {
        case .jambesForce: return "Jambes force"
        case .torseForce: return "Torse force"
        case .jambesHypertrophie: return "Jambes hypertrophie"
        case .torseHypertrophie: return "Torse hypertrophie"
        case .hiit: return "HIIT"
        case .mmaWod: return "MMA (WOD)"
        case .repos: return "Repos"
        }
    }
}

enum WeighInType: String, Codable, CaseIterable, Identifiable {
    case matinJeun = "matin_jeun"
    case soir

    var id: String { rawValue }
    var label: String { self == .matinJeun ? "Matin à jeun" : "Soir" }
}

enum RepasSlot: Int, Codable, CaseIterable, Identifiable {
    case matin = 1
    case postTraining = 2
    case apresMidi = 3
    case soir = 4

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .matin: return "Matin"
        case .postTraining: return "Post-training"
        case .apresMidi: return "Après-midi"
        case .soir: return "Soir"
        }
    }

    /// Répartition indicative des kcal/protéines sur la journée.
    /// Purement informatif : rien n'est bloqué si un repas déborde,
    /// tant que le total du jour respecte la cible.
    var shareIndicatif: Double {
        switch self {
        case .matin: return 0.25
        case .postTraining: return 0.30
        case .apresMidi: return 0.20
        case .soir: return 0.25
        }
    }
}

enum TypeJour: String, Codable, CaseIterable, Identifiable {
    case training
    case repos

    var id: String { rawValue }
    var label: String { self == .training ? "Jour training" : "Jour repos" }
}

/// Prépare l'extension post-septembre (métriques MMA spécifiques :
/// explosivité, cardio) sans refondre le schéma ni les modèles.
enum Phase: String, Codable, CaseIterable, Identifiable {
    case ete
    case curriculumMma = "curriculum_mma"

    var id: String { rawValue }
    var label: String { self == .ete ? "Été" : "Curriculum MMA" }
}
