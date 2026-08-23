import SwiftUI

struct MealLogView: View {
    @StateObject var viewModel: MealLogViewModel
    @State private var selectedSlot: RepasSlot = .matin
    @State private var calories = ""
    @State private var proteines = ""
    @State private var glucides = ""
    @State private var lipides = ""
    @State private var description = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Jour") {
                    DatePicker("Date", selection: $viewModel.date, displayedComponents: .date)
                        .onChange(of: viewModel.date) { Task { await viewModel.load() } }
                }

                if let target = viewModel.target {
                    Section("Cible du jour") {
                        DailyNutritionSummaryView(
                            caloriesCible: target.caloriesCible,
                            caloriesConsommees: viewModel.totalCalories,
                            proteinesCible: target.proteinesCibleG,
                            proteinesConsommees: viewModel.totalProteines
                        )
                    }
                } else {
                    Section("Cible du jour") {
                        Text("Pas encore de cible définie pour ce jour.")
                            .foregroundStyle(.secondary)
                        HStack {
                            Button("Jour training") { Task { await viewModel.setTarget(typeJour: .training) } }
                            Button("Jour repos") { Task { await viewModel.setTarget(typeJour: .repos) } }
                        }
                        .buttonStyle(.bordered)
                    }
                }

                Section("Repas déjà loggés") {
                    if viewModel.mealsForDay.isEmpty {
                        Text("Aucun repas pour l'instant.").foregroundStyle(.secondary)
                    }
                    ForEach(viewModel.mealsForDay) { meal in
                        if let slot = meal.repasSlot {
                            HStack {
                                Text(slot.label)
                                Spacer()
                                Text("\(meal.calories) kcal · \(Int(meal.proteinesG))g prot")
                                    .foregroundStyle(.secondary)
                            }
                            .font(.subheadline)
                        }
                    }
                }

                Section("Ajouter / modifier un repas") {
                    Picker("Créneau", selection: $selectedSlot) {
                        ForEach(RepasSlot.allCases) { slot in
                            Text(slot.label).tag(slot)
                        }
                    }
                    TextField("Calories", text: $calories).keyboardType(.numberPad)
                    TextField("Protéines (g)", text: $proteines).keyboardType(.decimalPad)
                    TextField("Glucides (g)", text: $glucides).keyboardType(.decimalPad)
                    TextField("Lipides (g)", text: $lipides).keyboardType(.decimalPad)
                    TextField("Description (optionnel)", text: $description)

                    Button("Enregistrer ce repas") {
                        Task {
                            let saved = await viewModel.logMeal(
                                slot: selectedSlot,
                                calories: Int(calories) ?? 0,
                                proteinesG: Double(proteines.replacingOccurrences(of: ",", with: ".")) ?? 0,
                                glucidesG: Double(glucides.replacingOccurrences(of: ",", with: ".")) ?? 0,
                                lipidesG: Double(lipides.replacingOccurrences(of: ",", with: ".")) ?? 0,
                                description: description
                            )
                            if saved {
                                calories = ""; proteines = ""; glucides = ""; lipides = ""; description = ""
                            }
                        }
                    }
                }

                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }
            }
            .navigationTitle("Log repas")
            .task { await viewModel.load() }
        }
    }
}
