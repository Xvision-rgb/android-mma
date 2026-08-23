import Foundation

struct Meal: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    var date: String
    var repas: Int
    var calories: Int
    var proteinesG: Double
    var glucidesG: Double
    var lipidesG: Double
    var description: String?
    var createdAt: String?

    var repasSlot: RepasSlot? { RepasSlot(rawValue: repas) }

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case date, repas, calories
        case proteinesG = "proteines_g"
        case glucidesG = "glucides_g"
        case lipidesG = "lipides_g"
        case description
        case createdAt = "created_at"
    }
}

struct NewMeal: Codable {
    var date: String
    var repas: Int
    var calories: Int
    var proteinesG: Double
    var glucidesG: Double
    var lipidesG: Double
    var description: String?

    enum CodingKeys: String, CodingKey {
        case date, repas, calories
        case proteinesG = "proteines_g"
        case glucidesG = "glucides_g"
        case lipidesG = "lipides_g"
        case description
    }
}
