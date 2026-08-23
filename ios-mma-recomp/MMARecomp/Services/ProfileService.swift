import Foundation
import Supabase

struct ProfileService {
    private let client = SupabaseClientProvider.shared

    func fetch(userId: UUID) async throws -> Profile {
        try await client
            .from("profiles")
            .select()
            .eq("id", value: userId)
            .single()
            .execute()
            .value
    }

    func update(userId: UUID, patch: ProfileUpdate) async throws {
        try await client
            .from("profiles")
            .update(patch)
            .eq("id", value: userId)
            .execute()
    }
}
