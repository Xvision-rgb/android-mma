import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var authService: AuthService

    var body: some View {
        TabView {
            DashboardView(viewModel: DashboardViewModel())
                .tabItem { Label("Dashboard", systemImage: "square.grid.2x2") }

            WorkoutLogView(viewModel: WorkoutLogViewModel())
                .tabItem { Label("Séance", systemImage: "figure.strengthtraining.traditional") }

            MealLogView(viewModel: MealLogViewModel())
                .tabItem { Label("Repas", systemImage: "fork.knife") }

            WeighInLogView(viewModel: WeighInViewModel())
                .tabItem { Label("Pesée", systemImage: "scalemass") }

            ProgressChartsView(viewModel: ProgressViewModel())
                .tabItem { Label("Progression", systemImage: "chart.line.uptrend.xyaxis") }

            if let userId = authService.currentUserId {
                SettingsView(viewModel: ProfileViewModel(userId: userId))
                    .tabItem { Label("Réglages", systemImage: "gearshape") }
            }
        }
    }
}
