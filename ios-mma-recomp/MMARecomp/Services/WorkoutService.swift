import Foundation
import Supabase

struct WorkoutService {
    private let client = SupabaseClientProvider.shared

    func fetchRecent(limit: Int = 30) async throws -> [Workout] {
        try await client
            .from("workouts")
            .select()
            .order("date", ascending: false)
            .limit(limit)
            .execute()
            .value
    }

    func fetchWeek(from startDate: String) async throws -> [Workout] {
        try await client
            .from("workouts")
            .select()
            .gte("date", value: startDate)
            .order("date", ascending: true)
            .execute()
            .value
    }

    @discardableResult
    func log(_ workout: NewWorkout) async throws -> Workout {
        try await client
            .from("workouts")
            .insert(workout)
            .select()
            .single()
            .execute()
            .value
    }

    func delete(id: UUID) async throws {
        try await client
            .from("workouts")
            .delete()
            .eq("id", value: id)
            .execute()
    }
}
