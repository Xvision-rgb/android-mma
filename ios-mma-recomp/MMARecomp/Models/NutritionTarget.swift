import Foundation

struct NutritionTarget: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    var date: String
    var typeJour: TypeJour
    var caloriesCible: Int
    var proteinesCibleG: Double
    var createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case date
        case typeJour = "type_jour"
        case caloriesCible = "calories_cible"
        case proteinesCibleG = "proteines_cible_g"
        case createdAt = "created_at"
    }
}

struct NewNutritionTarget: Codable {
    var date: String
    var typeJour: TypeJour
    var caloriesCible: Int
    var proteinesCibleG: Double

    enum CodingKeys: String, CodingKey {
        case date
        case typeJour = "type_jour"
        case caloriesCible = "calories_cible"
        case proteinesCibleG = "proteines_cible_g"
    }
}
