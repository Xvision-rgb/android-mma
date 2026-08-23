import SwiftUI

struct ExerciseRow: View {
    @Binding var exercice: LoggedExercise

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            TextField("Nom de l'exercice", text: $exercice.nom)
                .font(.subheadline.bold())

            HStack {
                Stepper("Séries: \(exercice.series)", value: $exercice.series, in: 1...10)
                Stepper("Reps: \(exercice.reps)", value: $exercice.reps, in: 1...30)
            }
            .font(.caption)

            HStack {
                labeledField("Charge cible (kg)", value: Binding(
                    get: { exercice.chargeCibleKg.map { String($0) } ?? "" },
                    set: { exercice.chargeCibleKg = Double($0.replacingOccurrences(of: ",", with: ".")) }
                ))
                labeledField("Charge réelle (kg)", value: Binding(
                    get: { exercice.chargeReelleKg.map { String($0) } ?? "" },
                    set: { exercice.chargeReelleKg = Double($0.replacingOccurrences(of: ",", with: ".")) }
                ))
            }

            Toggle("Toutes les reps faites proprement", isOn: $exercice.propre)
                .font(.caption)

            if let suggestion = exercice.suggestionProgression {
                SoftAlertBanner(
                    icon: "arrow.up.forward",
                    message: "Séance propre — essaie +2.5kg la prochaine fois (\(String(format: "%.1f", suggestion))kg)",
                    tone: .positive
                )
            }
        }
        .padding(.vertical, 4)
    }

    private func labeledField(_ label: String, value: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption2).foregroundStyle(.secondary)
            TextField(label, text: value)
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
        }
    }
}
