import Foundation

enum PlateauStatus {
    case none
    /// Poids stable mais performances en hausse -> message positif,
    /// jamais une alerte de stagnation.
    case recompositionEnCours
}

enum PlateauDetector {
    /// Poids stable ±0.5kg sur 2+ semaines (14 jours). Si en plus les
    /// performances progressent sur la même période, on affiche un message
    /// positif de recomposition plutôt qu'une alerte de stagnation.
    static func detect(
        morningWeighIns: [(date: Date, poidsKg: Double)],
        performanceTrendUp: Bool
    ) -> PlateauStatus {
        let cutoff = Calendar.current.date(byAdding: .day, value: -14, to: Date()) ?? Date()
        let recent = morningWeighIns
            .filter { $0.date >= cutoff }
            .sorted { $0.date < $1.date }

        guard recent.count >= 2,
              let minW = recent.map(\.poidsKg).min(),
              let maxW = recent.map(\.poidsKg).max() else {
            return .none
        }

        let isStable = (maxW - minW) <= 0.5
        return (isStable && performanceTrendUp) ? .recompositionEnCours : .none
    }
}
