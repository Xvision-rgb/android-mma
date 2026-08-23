import SwiftUI

struct WorkoutLogView: View {
    @StateObject var viewModel: WorkoutLogViewModel
    @AppStorage("currentPhase") private var phaseRaw: String = Phase.ete.rawValue
    @State private var didSave = false
    @State private var showingMMASheet = false

    private var phase: Phase { Phase(rawValue: phaseRaw) ?? .ete }

    var body: some View {
        NavigationStack {
            Form {
                Section("Séance") {
                    DatePicker("Date", selection: $viewModel.date, displayedComponents: .date)
                    Picker("Type", selection: $viewModel.type) {
                        ForEach(WorkoutType.allCases) { type in
                            Text(type.label).tag(type)
                        }
                    }
                    TextField("Durée (min)", text: $viewModel.dureeMin)
                        .keyboardType(.numberPad)
                }

                Section("Exercices") {
                    ForEach($viewModel.exercices) { $exercice in
                        ExerciseRow(exercice: $exercice)
                    }
                    .onDelete { viewModel.removeExercise(at: $0) }

                    Button("Ajouter un exercice", systemImage: "plus") {
                        viewModel.addExercise()
                    }
                }

                Section("Notes") {
                    TextField("Ressenti, notes libres…", text: $viewModel.notes, axis: .vertical)
                        .lineLimit(3...6)
                }

                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }

                Section {
                    Button {
                        Task {
                            if await viewModel.save() {
                                didSave = true
                                viewModel.exercices = []
                            }
                        }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("Enregistrer la séance").bold()
                        }
                    }
                    .disabled(viewModel.isSaving)
                }
            }
            .navigationTitle("Log séance")
            .toolbar {
                if viewModel.type == .mmaWod {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("WOD MMA") { showingMMASheet = true }
                    }
                }
            }
            .sheet(isPresented: $showingMMASheet) {
                MMASessionLogView(viewModel: MMASessionViewModel())
            }
            .task { await viewModel.loadPlan(phase: phase) }
            .onChange(of: viewModel.date) {
                Task { await viewModel.loadPlan(phase: phase) }
            }
            .alert("Séance enregistrée 💪", isPresented: $didSave) {
                Button("OK", role: .cancel) {}
            }
        }
    }
}
