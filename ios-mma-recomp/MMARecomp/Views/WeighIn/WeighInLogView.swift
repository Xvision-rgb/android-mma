import SwiftUI

struct WeighInLogView: View {
    @StateObject var viewModel: WeighInViewModel
    @State private var didSave = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Tendance (moyenne 7 jours, matin à jeun)") {
                    WeightTrendChart(points: viewModel.trend7Day)
                        .frame(height: 160)

                    if case .recompositionEnCours = viewModel.plateauStatus {
                        SoftAlertBanner(
                            icon: "sparkles",
                            message: "Poids stable mais tu progresses — recomposition en cours 💪",
                            tone: .positive
                        )
                    }
                }

                Section("Nouvelle pesée") {
                    DatePicker("Date", selection: $viewModel.date, displayedComponents: .date)
                    DatePicker("Heure", selection: $viewModel.heure, displayedComponents: .hourAndMinute)
                    Picker("Type", selection: $viewModel.type) {
                        ForEach(WeighInType.allCases) { Text($0.label).tag($0) }
                    }
                    TextField("Poids (kg)", text: $viewModel.poidsKg)
                        .keyboardType(.decimalPad)
                    TextField("% masse grasse (optionnel)", text: $viewModel.bfPct)
                        .keyboardType(.decimalPad)

                    Toggle("Créatine reprise récemment", isOn: $viewModel.creatineRecente)
                    Toggle("Alcool récent", isOn: $viewModel.alcoolRecent)
                    Toggle("Juste après une séance intense", isOn: $viewModel.postTraining)
                }

                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }

                Section {
                    Button {
                        Task {
                            if await viewModel.save() {
                                didSave = true
                                viewModel.poidsKg = ""
                                viewModel.bfPct = ""
                            }
                        }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("Enregistrer la pesée").bold()
                        }
                    }
                    .disabled(viewModel.isSaving)
                }
            }
            .navigationTitle("Log pesée")
            .task { await viewModel.loadHistory() }
            .alert("Pesée enregistrée", isPresented: $didSave) {
                Button("OK", role: .cancel) {}
            }
        }
    }
}
