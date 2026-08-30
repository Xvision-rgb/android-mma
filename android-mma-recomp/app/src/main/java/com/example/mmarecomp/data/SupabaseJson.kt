package com.example.mmarecomp.data

import kotlinx.serialization.json.Json

/** Configuration JSON du client Supabase, isolée du client réseau pour rester
 *  testable sans initialiser Ktor/Android.
 *
 *  Le défaut de supabase-kt est `Json.Default` (`ignoreUnknownKeys = false`) :
 *  la moindre colonne renvoyée par PostgREST mais absente du modèle Kotlin
 *  (ex. `created_at` / `updated_at`, présents et NOT NULL sur `foods` et
 *  `training_plan`) faisait alors échouer TOUT le `decodeList()`/`decodeSingle()`
 *  de la requête — la bibliothèque d'aliments et le plan d'entraînement ne se
 *  chargeaient tout simplement pas.
 *
 *  - `ignoreUnknownKeys` : le schéma Postgres peut évoluer (nouvelles colonnes,
 *    timestamps) sans casser la lecture côté app.
 *  - `coerceInputValues` : un `null` inattendu sur un champ à valeur par défaut
 *    retombe sur le défaut au lieu de faire crasher la désérialisation de toute
 *    la ligne. */
val supabaseJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
