import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject var viewModel: ProfileViewModel
    @AppStorage("currentPhase") private var phaseRaw: String = Phase.ete.rawValue

    var body: some View {
        NavigationStack {
            Form {
                Section("Objectifs") {
                    TextField("Poids objectif (kg)", text: $viewModel.poidsObjectifKg)
                        .keyboardType(.decimalPad)
                    TextField("% BF objectif", text: $viewModel.bfObjectifPct)
                        .keyboardType(.decimalPad)
                    Picker("Phase", selection: $viewModel.phase) {
                        ForEach(Phase.allCases) { Text($0.label).tag($0) }
                    }
                }

                Section("Notes du coach") {
                    TextField("Notes libres…", text: $viewModel.coachNotes, axis: .vertical)
                        .lineLimit(3...8)
                }

                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }

                Section {
                    Button("Enregistrer") {
                        Task {
                            await viewModel.save()
                            phaseRaw = viewModel.phase.rawValue
                        }
                    }
                    .disabled(viewModel.isSaving)
                }

                Section {
                    Button("Se déconnecter", role: .destructive) {
                        Task { try? await authService.signOut() }
                    }
                }
            }
            .navigationTitle("Réglages")
            .task { await viewModel.load() }
        }
    }
}
