import Foundation
import Supabase

struct WeighInService {
    private let client = SupabaseClientProvider.shared

    func fetch(since dateString: String) async throws -> [WeighIn] {
        try await client
            .from("weigh_ins")
            .select()
            .gte("date", value: dateString)
            .order("date", ascending: true)
            .execute()
            .value
    }

    /// Upsert sur (user_id, date, type) : re-loguer la même pesée du jour
    /// remplace la précédente au lieu de dupliquer.
    @discardableResult
    func log(_ weighIn: NewWeighIn) async throws -> WeighIn {
        try await client
            .from("weigh_ins")
            .upsert(weighIn, onConflict: "user_id,date,type")
            .select()
            .single()
            .execute()
            .value
    }
}
