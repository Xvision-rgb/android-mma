package com.example.mmarecomp.util

data class ParsedWodMovement(val nom: String, val quantite: Int?)

/** Parsing best-effort du texte du WOD collé depuis WhatsApp. Ne bloque
 *  jamais la saisie libre : si rien n'est reconnu, le texte brut reste la
 *  donnée de référence (wodContent), le parsing n'est qu'un résumé visuel. */
object WodParser {
    private val knownMovements = linkedMapOf(
        "burpee" to "Burpees", "burpees" to "Burpees",
        "squat" to "Squats", "squats" to "Squats",
        "pompe" to "Pompes", "pompes" to "Pompes", "push-up" to "Pompes", "push up" to "Pompes",
        "fente" to "Fentes", "fentes" to "Fentes", "lunge" to "Fentes",
        "corde a sauter" to "Corde à sauter", "double under" to "Corde à sauter (double-unders)",
        "sprint" to "Sprints",
        "shadow boxing" to "Shadow boxing", "shadow" to "Shadow boxing",
        "clinch" to "Clinch",
        "sac de frappe" to "Sac de frappe", "sac" to "Sac de frappe",
        "medecine ball" to "Med-ball", "med-ball" to "Med-ball", "med ball" to "Med-ball",
        "kettlebell" to "Kettlebell", "kb swing" to "Kettlebell swings",
        "abdo" to "Abdos", "abs" to "Abdos", "gainage" to "Gainage", "plank" to "Gainage",
        "traction" to "Tractions", "tractions" to "Tractions", "pull-up" to "Tractions",
        "box jump" to "Box jump", "jump squat" to "Jump squat",
        "roulade" to "Roulades", "sprawl" to "Sprawls", "sprawls" to "Sprawls",
    )

    private val numberedMovementRegex =
        Regex("""(\d+)\s*(x|reps?|répétitions?)?\s*([a-zàâäéèêëïîôöùûüç\- ]{3,25})""")

    fun parse(text: String): List<ParsedWodMovement> {
        if (text.isEmpty()) return emptyList()
        val normalized = text.lowercase()
        val found = mutableListOf<ParsedWodMovement>()
        val seen = mutableSetOf<String>()

        for (match in numberedMovementRegex.findAll(normalized)) {
            val quantite = match.groupValues[1].toIntOrNull()
            val words = match.groupValues[3]
            for ((keyword, label) in knownMovements) {
                if (words.contains(keyword) && seen.add(label)) {
                    found.add(ParsedWodMovement(label, quantite))
                }
            }
        }

        // Fallback : mot-clé présent n'importe où dans le texte, sans quantité détectée.
        for ((keyword, label) in knownMovements) {
            if (normalized.contains(keyword) && seen.add(label)) {
                found.add(ParsedWodMovement(label, null))
            }
        }

        return found
    }
}
