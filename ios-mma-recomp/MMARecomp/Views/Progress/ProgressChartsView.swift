import SwiftUI
import Charts

/// Nommée "ProgressChartsView" pour ne pas entrer en collision avec
/// SwiftUI.ProgressView (l'indicateur de chargement).
struct ProgressChartsView: View {
    @StateObject var viewModel: ProgressViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Picker("Fenêtre", selection: $viewModel.windowWeeks) {
                        Text("4 semaines").tag(4)
                        Text("8 semaines").tag(8)
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: viewModel.windowWeeks) { Task { await viewModel.load() } }

                    VStack(alignment: .leading) {
                        Text("Poids (moy. 7j)").font(.headline)
                        WeightTrendChart(points: viewModel.weightTrend)
                            .frame(height: 160)
                    }

                    if !viewModel.bfTrend.isEmpty {
                        VStack(alignment: .leading) {
                            Text("% Masse grasse (moy. 7j)").font(.headline)
                            Chart(viewModel.bfTrend, id: \.date) { point in
                                LineMark(
                                    x: .value("Date", point.date),
                                    y: .value("BF %", point.value)
                                )
                                .foregroundStyle(.purple)
                            }
                            .frame(height: 140)
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Charges — progression par exercice").font(.headline)
                        if viewModel.chargeProgressionByExercise.isEmpty {
                            Text("Log des séances avec charge réelle pour voir apparaître ta progression ici.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        ForEach(
                            viewModel.chargeProgressionByExercise.sorted(by: { $0.key < $1.key }),
                            id: \.key
                        ) { name, series in
                            VStack(alignment: .leading) {
                                Text(name).font(.subheadline.bold())
                                Chart(series, id: \.date) { point in
                                    LineMark(
                                        x: .value("Date", point.date),
                                        y: .value("Charge (kg)", point.chargeKg)
                                    )
                                    .foregroundStyle(.green)
                                }
                                .frame(height: 100)
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Progression")
            .task { await viewModel.load() }
        }
    }
}
