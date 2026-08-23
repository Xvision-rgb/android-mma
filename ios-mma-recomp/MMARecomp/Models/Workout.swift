import Foundation

/// Log réel d'une séance (jambes/torse/HIIT). Le date est en "yyyy-MM-dd"
/// pour matcher directement la colonne Postgres `date` sans souci de fuseau
/// horaire côté décodage JSON.
struct Workout: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    var date: String
    var type: WorkoutType
    var exercices: [LoggedExercise]
    var dureeMin: Int?
    var notes: String?
    var createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case date, type, exercices
        case dureeMin = "duree_min"
        case notes
        case createdAt = "created_at"
    }
}

struct NewWorkout: Codable {
    var date: String
    var type: WorkoutType
    var exercices: [LoggedExercise]
    var dureeMin: Int?
    var notes: String?

    enum CodingKeys: String, CodingKey {
        case date, type, exercices
        case dureeMin = "duree_min"
        case notes
    }
}
