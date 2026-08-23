import Foundation

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var errorMessage: String?
    @Published var isSubmitting = false

    private let authService: AuthService

    init(authService: AuthService) {
        self.authService = authService
    }

    func signIn() async {
        errorMessage = nil
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await authService.signIn(email: email, password: password)
        } catch {
            errorMessage = "Connexion impossible. Vérifie ton email et mot de passe."
        }
    }

    func signUp() async {
        errorMessage = nil
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await authService.signUp(email: email, password: password)
        } catch {
            errorMessage = "Inscription impossible. Réessaie."
        }
    }
}
