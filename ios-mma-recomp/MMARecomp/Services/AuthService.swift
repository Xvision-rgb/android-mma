import Foundation
import Supabase

@MainActor
final class AuthService: ObservableObject {
    @Published var currentUserId: UUID?
    @Published var isLoading = true

    private let client = SupabaseClientProvider.shared

    init() {
        Task { await bootstrap() }
    }

    private func bootstrap() async {
        if let session = try? await client.auth.session {
            currentUserId = session.user.id
        }
        isLoading = false

        for await state in client.auth.authStateChanges {
            switch state.event {
            case .signedIn, .tokenRefreshed, .userUpdated:
                currentUserId = state.session?.user.id
            case .signedOut:
                currentUserId = nil
            default:
                break
            }
        }
    }

    func signIn(email: String, password: String) async throws {
        let session = try await client.auth.signIn(email: email, password: password)
        currentUserId = session.user.id
    }

    func signUp(email: String, password: String) async throws {
        let response = try await client.auth.signUp(email: email, password: password)
        currentUserId = response.user.id
    }

    func signOut() async throws {
        try await client.auth.signOut()
        currentUserId = nil
    }
}
