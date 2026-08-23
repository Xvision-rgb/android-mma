import Foundation

/// Flags contextuels qui faussent le %BF / le poids brut — affichés en note
/// à côté du point plutôt que cachés, pour désamorcer les fausses alertes.
struct WeighInContext: Codable, Hashable {
    var creatineRecente: Bool = false
    var alcoolRecent: Bool = false
    var postTraining: Bool = false

    enum CodingKeys: String, CodingKey {
        case creatineRecente = "creatine_recente"
        case alcoolRecent = "alcool_recent"
        case postTraining = "post_training"
    }

    var hasAnyFlag: Bool { creatineRecente || alcoolRecent || postTraining }

    var flagLabels: [String] {
        var labels: [String] = []
        if creatineRecente { labels.append("créatine récente") }
        if alcoolRecent { labels.append("alcool récent") }
        if postTraining { labels.append("post-training") }
        return labels
    }
}

struct WeighIn: Codable, Identifiable {
    var id: UUID
    var userId: UUID
    var date: String
    var heure: String // "HH:mm:ss"
    var type: WeighInType
    var poidsKg: Double
    var bfPct: Double?
    var contexte: WeighInContext
    var createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case date, heure, type
        case poidsKg = "poids_kg"
        case bfPct = "bf_pct"
        case contexte
        case createdAt = "created_at"
    }
}

struct NewWeighIn: Codable {
    var date: String
    var heure: String
    var type: WeighInType
    var poidsKg: Double
    var bfPct: Double?
    var contexte: WeighInContext

    enum CodingKeys: String, CodingKey {
        case date, heure, type
        case poidsKg = "poids_kg"
        case bfPct = "bf_pct"
        case contexte
    }
}
