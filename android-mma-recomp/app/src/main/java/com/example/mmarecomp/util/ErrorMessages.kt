package com.example.mmarecomp.util

import java.io.IOException

/**
 * Traduit une exception technique en message compréhensible pour
 * l'utilisateur, sans jamais exposer de détails techniques bruts (stack
 * trace, code SQL, nom de contrainte Postgres...). `fallback` reste le
 * message par défaut quand rien de plus précis n'est identifiable.
 */
fun Throwable.toFriendlyMessage(fallback: String): String = when {
    this is IOException -> "Pas de connexion internet — réessaie dans un instant."
    // "Remplacée" serait faux ici : seule la pesée upserte réellement sur
    // conflit (donc ne lève jamais cette erreur en pratique). Séance et
    // séance MMA font un simple insert — un conflit de clé signifie que
    // rien n'a été enregistré, pas qu'une entrée a été remplacée.
    message?.contains("duplicate key", ignoreCase = true) == true ->
        "Une entrée existe déjà pour cette date — vérifie qu'elle n'a pas déjà été enregistrée."
    message?.contains("violates check constraint", ignoreCase = true) == true ||
        message?.contains("violates foreign key", ignoreCase = true) == true ->
        "Une des valeurs saisies est invalide."
    message?.contains("JWT", ignoreCase = true) == true ||
        message?.contains("401", ignoreCase = true) == true ->
        "Ta session a expiré — reconnecte-toi."
    message?.contains("timeout", ignoreCase = true) == true ->
        "Le serveur met trop de temps à répondre — réessaie."
    else -> fallback
}
