import Foundation

enum NutritionTargetCalculator {
    /// Cible du jour selon calorie cycling : plus haut les jours training,
    /// plus bas les jours off, protéines maintenues hautes dans les deux cas.
    static func target(for typeJour: TypeJour) -> (calories: Int, proteinesG: Double) {
        switch typeJour {
        case .training:
            return (2050, 135) // milieu de la fourchette 2000-2100 kcal / 130-140g
        case .repos:
            return (1800, 130)
        }
    }

    /// Répartition indicative (non bloquante) de la cible du jour sur les
    /// créneaux repas — un repas qui déborde n'est jamais signalé tant que
    /// le total du jour reste dans la cible.
    static func indicativeSplit(
        calories: Int,
        proteinesG: Double,
        slots: [RepasSlot]
    ) -> [RepasSlot: (calories: Int, proteinesG: Double)] {
        var result: [RepasSlot: (calories: Int, proteinesG: Double)] = [:]
        for slot in slots {
            result[slot] = (
                Int(Double(calories) * slot.shareIndicatif),
                (proteinesG * slot.shareIndicatif).rounded()
            )
        }
        return result
    }
}
