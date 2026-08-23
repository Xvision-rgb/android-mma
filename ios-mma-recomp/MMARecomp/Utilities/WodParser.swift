import Foundation

struct ParsedWodMovement: Identifiable, Hashable {
    var id = UUID()
    var nom: String
    var quantite: Int?
}

/// Parsing best-effort du texte du WOD collé depuis WhatsApp. Ne bloque
/// jamais la saisie libre : si rien n'est reconnu, le texte brut reste la
/// donnée de référence (wod_content), le parsing n'est qu'un résumé visuel.
enum WodParser {
    private static let knownMovements: [String: String] = [
        "burpee": "Burpees", "burpees": "Burpees",
        "squat": "Squats", "squats": "Squats",
        "pompe": "Pompes", "pompes": "Pompes", "push-up": "Pompes", "push up": "Pompes",
        "fente": "Fentes", "fentes": "Fentes", "lunge": "Fentes",
        "corde a sauter": "Corde à sauter", "double under": "Corde à sauter (double-unders)",
        "sprint": "Sprints",
        "shadow boxing": "Shadow boxing", "shadow": "Shadow boxing",
        "clinch": "Clinch",
        "sac de frappe": "Sac de frappe", "sac": "Sac de frappe",
        "medecine ball": "Med-ball", "med-ball": "Med-ball", "med ball": "Med-ball",
        "kettlebell": "Kettlebell", "kb swing": "Kettlebell swings",
        "abdo": "Abdos", "abs": "Abdos", "gainage": "Gainage", "plank": "Gainage",
        "traction": "Tractions", "tractions": "Tractions", "pull-up": "Tractions",
        "box jump": "Box jump", "jump squat": "Jump squat",
        "roulade": "Roulades", "sprawl": "Sprawls", "sprawls": "Sprawls",
    ]

    static func parse(_ text: String) -> [ParsedWodMovement] {
        guard !text.isEmpty else { return [] }
        let normalized = text.lowercased()
        var found: [ParsedWodMovement] = []
        var seen = Set<String>()

        let pattern = #"(\d+)\s*(x|reps?|répétitions?)?\s*([a-zàâäéèêëïîôöùûüç\- ]{3,25})"#
        if let regex = try? NSRegularExpression(pattern: pattern) {
            let nsrange = NSRange(normalized.startIndex..., in: normalized)
            regex.enumerateMatches(in: normalized, range: nsrange) { match, _, _ in
                guard let match,
                      let numberRange = Range(match.range(at: 1), in: normalized),
                      let wordsRange = Range(match.range(at: 3), in: normalized) else { return }
                let quantite = Int(normalized[numberRange])
                let words = normalized[wordsRange]

                for (keyword, label) in knownMovements where words.contains(keyword) && !seen.contains(label) {
                    seen.insert(label)
                    found.append(ParsedWodMovement(nom: label, quantite: quantite))
                }
            }
        }

        // Fallback : mot-clé présent n'importe où dans le texte, sans quantité détectée.
        for (keyword, label) in knownMovements where normalized.contains(keyword) && !seen.contains(label) {
            seen.insert(label)
            found.append(ParsedWodMovement(nom: label, quantite: nil))
        }

        return found
    }
}
