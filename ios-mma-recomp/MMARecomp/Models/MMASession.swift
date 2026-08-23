import Foundation

struct MMASession: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    var date: String
    var wodContent: String
    var roundsSets: String?
    var ressenti: Int?
    var notesTechnique: String?
    var createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case date
        case wodContent = "wod_content"
        case roundsSets = "rounds_sets"
        case ressenti
        case notesTechnique = "notes_technique"
        case createdAt = "created_at"
    }
}

struct NewMMASession: Codable {
    var date: String
    var wodContent: String
    var roundsSets: String?
    var ressenti: Int?
    var notesTechnique: String?

    enum CodingKeys: String, CodingKey {
        case date
        case wodContent = "wod_content"
        case roundsSets = "rounds_sets"
        case ressenti
        case notesTechnique = "notes_technique"
    }
}
