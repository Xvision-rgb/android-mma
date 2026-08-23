import SwiftUI

struct RootView: View {
    @StateObject private var authService = AuthService()

    var body: some View {
        Group {
            if authService.isLoading {
                ProgressView()
            } else if authService.currentUserId != nil {
                MainTabView()
                    .environmentObject(authService)
            } else {
                AuthView(viewModel: AuthViewModel(authService: authService))
            }
        }
    }
}
