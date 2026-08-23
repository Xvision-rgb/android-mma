import SwiftUI

struct DailyNutritionSummaryView: View {
    let caloriesCible: Int
    let caloriesConsommees: Int
    let proteinesCible: Double
    let proteinesConsommees: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            TargetVsActualBar(
                label: "Calories",
                actual: Double(caloriesConsommees),
                target: Double(caloriesCible),
                unit: "kcal"
            )
            TargetVsActualBar(
                label: "Protéines",
                actual: proteinesConsommees,
                target: proteinesCible,
                unit: "g"
            )

            if caloriesConsommees < caloriesCible {
                Text("Il reste \(caloriesCible - caloriesConsommees) kcal aujourd'hui — pas de panique, juste une indication.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}
