import SwiftUI

struct MMASessionLogView: View {
    @StateObject var viewModel: MMASessionViewModel
    @State private var didSave = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Séance MMA") {
                    DatePicker("Date", selection: $viewModel.date, displayedComponents: .date)
                }

                Section("WOD du coach (collé depuis WhatsApp)") {
                    TextField("Colle le texte du WOD ici…", text: $viewModel.wodContent, axis: .vertical)
                        .lineLimit(4...10)

                    if !viewModel.parsedMovements.isEmpty {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Mouvements détectés")
                                .font(.caption.bold())
                                .foregroundStyle(.secondary)
                            ForEach(viewModel.parsedMovements) { movement in
                                HStack {
                                    Text(movement.nom)
                                    Spacer()
                                    if let quantite = movement.quantite {
                                        Text("×\(quantite)").foregroundStyle(.secondary)
                                    }
                                }
                                .font(.footnote)
                            }
                        }
                    }
                }

                Section("Rounds / Sets") {
                    TextField("Ex: 5 rounds x 3min", text: $viewModel.roundsSets)
                }

                Section("Ressenti") {
                    Picker("Ressenti (1-5)", selection: $viewModel.ressenti) {
                        ForEach(1...5, id: \.self) { Text("\($0)").tag($0) }
                    }
                    .pickerStyle(.segmented)
                }

                Section("Notes technique") {
                    TextField("Points techniques travaillés…", text: $viewModel.notesTechnique, axis: .vertical)
                        .lineLimit(3...6)
                }

                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }

                Section {
                    Button {
                        Task {
                            if await viewModel.save() { didSave = true }
                        }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("Enregistrer la séance MMA").bold()
                        }
                    }
                    .disabled(viewModel.isSaving)
                }
            }
            .navigationTitle("Log MMA")
            .alert("Séance MMA enregistrée 🥋", isPresented: $didSave) {
                Button("OK", role: .cancel) {}
            }
        }
    }
}
