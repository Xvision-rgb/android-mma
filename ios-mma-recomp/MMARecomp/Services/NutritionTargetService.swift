import Foundation
import Supabase

struct NutritionTargetService {
    private let client = SupabaseClientProvider.shared

    func fetch(forDate dateString: String) async throws -> NutritionTarget? {
        let results: [NutritionTarget] = try await client
            .from("nutrition_targets")
            .select()
            .eq("date", value: dateString)
            .execute()
            .value
        return results.first
    }

    @discardableResult
    func set(_ target: NewNutritionTarget) async throws -> NutritionTarget {
        try await client
            .from("nutrition_targets")
            .upsert(target, onConflict: "user_id,date")
            .select()
            .single()
            .execute()
            .value
    }
}
