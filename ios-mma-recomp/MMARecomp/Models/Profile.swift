import Foundation

struct Profile: Codable, Identifiable {
    var id: UUID
    var poidsObjectifKg: Double
    var bfObjectifPct: Double
    var phase: Phase
    var coachNotes: String?
    var createdAt: String?
    var updatedAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case poidsObjectifKg = "poids_objectif_kg"
        case bfObjectifPct = "bf_objectif_pct"
        case phase
        case coachNotes = "coach_notes"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

struct ProfileUpdate: Codable {
    var poidsObjectifKg: Double?
    var bfObjectifPct: Double?
    var phase: Phase?
    var coachNotes: String?

    enum CodingKeys: String, CodingKey {
        case poidsObjectifKg = "poids_objectif_kg"
        case bfObjectifPct = "bf_objectif_pct"
        case phase
        case coachNotes = "coach_notes"
    }
}
