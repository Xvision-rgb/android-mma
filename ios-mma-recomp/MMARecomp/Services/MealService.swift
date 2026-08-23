import Foundation
import Supabase

struct MealService {
    private let client = SupabaseClientProvider.shared

    func fetch(forDate dateString: String) async throws -> [Meal] {
        try await client
            .from("meals")
            .select()
            .eq("date", value: dateString)
            .order("repas", ascending: true)
            .execute()
            .value
    }

    func fetch(since dateString: String) async throws -> [Meal] {
        try await client
            .from("meals")
            .select()
            .gte("date", value: dateString)
            .order("date", ascending: true)
            .execute()
            .value
    }

    /// Upsert sur (user_id, date, repas) : re-loguer un créneau remplace
    /// l'entrée existante plutôt que d'en créer une deuxième.
    @discardableResult
    func log(_ meal: NewMeal) async throws -> Meal {
        try await client
            .from("meals")
            .upsert(meal, onConflict: "user_id,date,repas")
            .select()
            .single()
            .execute()
            .value
    }

    func delete(id: UUID) async throws {
        try await client
            .from("meals")
            .delete()
            .eq("id", value: id)
            .execute()
    }
}
