package com.example.mmarecomp.data

import com.example.mmarecomp.SupabaseConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlin.time.Duration.Companion.seconds

/**
 * Toutes les dates/timestamps du modèle sont transportées en String
 * (formats "yyyy-MM-dd" / "HH:mm:ss") pour matcher directement les colonnes
 * Postgres sans piège de désérialisation de date. Les @SerialName de chaque
 * modèle mappent déjà explicitement le snake_case Postgres.
 *
 * Le SDK supabase-kt évoluant vite, si un appel (`.from().select()...`) ne
 * compile pas tel quel avec ta version résolue, ajuste la signature : la
 * logique métier (repositories/viewmodels/écrans) n'a pas besoin de changer.
 */
object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY,
    ) {
        // Sérialiseur explicite tolérant aux colonnes inconnues (created_at,
        // updated_at, futures colonnes) — cf. supabaseJson. Sans ça, le défaut
        // supabase-kt faisait crasher toute lecture d'une table à timestamps.
        defaultSerializer = KotlinXSerializer(supabaseJson)
        // Timeout explicite : le défaut supabase-kt (10s) est court sur réseau
        // mobile instable ; 30s évite des échecs prématurés sans bloquer l'UI
        // indéfiniment (les ViewModels annulent à la navigation).
        requestTimeout = 30.seconds
        install(Auth)
        install(Postgrest)
    }
}
