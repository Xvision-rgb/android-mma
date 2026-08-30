package com.example.mmarecomp.data

import com.example.mmarecomp.model.Food
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.Workout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Régression du bug de désérialisation : PostgREST renvoie systématiquement les
 * colonnes techniques (`created_at`, `updated_at`) que les modèles Kotlin ne
 * mappent pas. Avec le `Json.Default` de supabase-kt (`ignoreUnknownKeys = false`),
 * une seule colonne inconnue faisait échouer TOUT le `decodeList()` : la
 * bibliothèque d'aliments et le plan d'entraînement ne se chargeaient jamais.
 *
 * [supabaseJson] est la configuration réellement injectée dans le client
 * (cf. [SupabaseProvider]) — on la teste ici directement, sans initialiser Ktor.
 */
class SupabaseJsonTest {

    private val foodAvecCreatedAt = """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "nom": "Blanc de poulet cru",
          "categorie": "proteine",
          "kcal_100g": 165.0,
          "proteines_100g": 31.0,
          "glucides_100g": 0.0,
          "lipides_100g": 3.6,
          "created_at": "2026-08-30T10:00:00+00:00"
        }
    """.trimIndent()

    @Test
    fun `foods se decode malgre la colonne created_at non mappee`() {
        val food = supabaseJson.decodeFromString<Food>(foodAvecCreatedAt)

        assertEquals("Blanc de poulet cru", food.nom)
        assertEquals(31.0, food.proteines100g, 0.001)
    }

    @Test
    fun `le defaut supabase-kt aurait crashe sur created_at — regression documentee`() {
        // Reproduit le bug d'origine : le client utilisait Json.Default
        // (ignoreUnknownKeys = false), donc la colonne created_at renvoyée par
        // PostgREST faisait échouer toute la lecture de la table foods.
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<Food>(foodAvecCreatedAt)
        }
    }

    @Test
    fun `une liste foods entiere se decode sans crash sur les timestamps`() {
        val json = """
            [
              {"id":"a","nom":"Œuf entier","categorie":"proteine","kcal_100g":155.0,
               "proteines_100g":13.0,"glucides_100g":1.1,"lipides_100g":11.0,
               "created_at":"2026-08-30T10:00:00Z"},
              {"id":"b","nom":"Riz basmati cuit","categorie":"glucide","kcal_100g":130.0,
               "proteines_100g":2.7,"glucides_100g":28.0,"lipides_100g":0.3,
               "created_at":"2026-08-30T10:00:00Z"}
            ]
        """.trimIndent()

        val foods = supabaseJson.decodeFromString<List<Food>>(json)

        assertEquals(2, foods.size)
    }

    @Test
    fun `training_plan se decode malgre created_at et updated_at non mappes`() {
        val json = """
            {
              "id": "22222222-2222-2222-2222-222222222222",
              "user_id": "u1",
              "jour_semaine": 1,
              "type": "jambes_force",
              "exercices": [],
              "phase": "ete",
              "notes": null,
              "actif": true,
              "created_at": "2026-08-30T10:00:00Z",
              "updated_at": "2026-08-30T10:00:00Z"
            }
        """.trimIndent()

        val day = supabaseJson.decodeFromString<TrainingPlanDay>(json)

        assertEquals(1, day.jourSemaine)
        assertTrue(day.exercices.isEmpty())
    }

    @Test
    fun `un workout historique JSONB se decode malgre une cle legacy inconnue`() {
        // La colonne `exercices` est du JSONB : de l'historique peut contenir
        // des clés d'anciens formats. Elles ne doivent pas casser la lecture.
        val json = """
            {
              "id": "33333333-3333-3333-3333-333333333333",
              "user_id": "u1",
              "date": "2026-08-30",
              "type": "jambes_force",
              "exercices": [
                {"nom":"Squat","series":3,"reps":5,"charge_reelle_kg":100.0,"legacy_flag":true}
              ],
              "duree_min": 60,
              "rpe": 7,
              "notes": null,
              "created_at": "2026-08-30T10:00:00Z"
            }
        """.trimIndent()

        val workout = supabaseJson.decodeFromString<Workout>(json)

        assertEquals(1, workout.exercices.size)
        // La dérivation rétrocompatible reste opérationnelle (série unique -> sets).
        assertEquals(100.0, workout.exercices.first().chargeMaxKg!!, 0.001)
    }
}
