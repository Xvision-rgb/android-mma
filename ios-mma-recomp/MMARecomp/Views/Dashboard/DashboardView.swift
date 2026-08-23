import SwiftUI

struct DashboardView: View {
    @StateObject var viewModel: DashboardViewModel
    @AppStorage("currentPhase") private var phaseRaw: String = Phase.ete.rawValue

    private var phase: Phase { Phase(rawValue: phaseRaw) ?? .ete }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    weekSummaryCard
                    weightTrendCard
                    nutritionCard
                }
                .padding()
            }
            .navigationTitle("Cette semaine")
            .refreshable { await viewModel.load(phase: phase) }
            .task { await viewModel.load(phase: phase) }
        }
    }

    private var weekSummaryCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Séances")
                .font(.headline)
            Text("\(viewModel.seancesFaitesCount) faites / \(viewModel.seancesPlanifieesCount) prévues")
                .font(.title2.bold())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private var weightTrendCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Tendance poids (moyenne 7 jours)")
                .font(.headline)
            WeightTrendChart(points: viewModel.weightTrend7Day)
                .frame(height: 160)

            if case .recompositionEnCours = viewModel.plateauStatus {
                SoftAlertBanner(
                    icon: "sparkles",
                    message: "Poids stable mais tes séances progressent — recomposition en cours 💪",
                    tone: .positive
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private var nutritionCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Nutrition")
                .font(.headline)
            Text("Moyenne 7j : \(viewModel.avgCaloriesLast7Days) kcal/jour")
                .font(.title3.bold())
            if let target = viewModel.todayTarget {
                Text("Cible aujourd'hui : \(target.caloriesCible) kcal · \(Int(target.proteinesCibleG))g protéines")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}
