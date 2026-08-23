import Foundation
import Supabase

struct TrainingPlanService {
    private let client = SupabaseClientProvider.shared

    func fetchWeek(phase: Phase) async throws -> [TrainingPlanDay] {
        try await client
            .from("training_plan")
            .select()
            .eq("phase", value: phase.rawValue)
            .eq("actif", value: true)
            .order("jour_semaine", ascending: true)
            .execute()
            .value
    }

    func upsert(_ day: NewTrainingPlanDay) async throws {
        try await client
            .from("training_plan")
            .upsert(day, onConflict: "user_id,jour_semaine,phase")
            .execute()
    }
}
