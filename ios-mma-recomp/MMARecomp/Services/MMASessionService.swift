import Foundation
import Supabase

struct MMASessionService {
    private let client = SupabaseClientProvider.shared

    func fetchRecent(limit: Int = 20) async throws -> [MMASession] {
        try await client
            .from("mma_sessions")
            .select()
            .order("date", ascending: false)
            .limit(limit)
            .execute()
            .value
    }

    @discardableResult
    func log(_ session: NewMMASession) async throws -> MMASession {
        try await client
            .from("mma_sessions")
            .insert(session)
            .select()
            .single()
            .execute()
            .value
    }
}
