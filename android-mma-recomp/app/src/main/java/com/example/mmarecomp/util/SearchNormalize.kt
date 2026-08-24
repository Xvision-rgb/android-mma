package com.example.mmarecomp.util

import java.text.Normalizer

/** Normalise une chaîne pour une recherche insensible aux accents et à la
 *  casse — "creme" doit retrouver "Crème", "oeuf" doit retrouver "Œuf". */
fun normalizeForSearch(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutAccents = decomposed.replace(Regex("\\p{Mn}+"), "")
    return withoutAccents
        .replace("œ", "oe", ignoreCase = true)
        .replace("æ", "ae", ignoreCase = true)
        .lowercase()
}

fun matchesSearch(candidate: String, query: String): Boolean =
    normalizeForSearch(candidate).contains(normalizeForSearch(query))
