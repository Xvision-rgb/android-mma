import Foundation
import Supabase

/// Toutes les dates/timestamps du modèle sont transportées en String
/// (formats "yyyy-MM-dd" / "HH:mm:ss" / ISO8601) pour éviter les pièges de
/// `dateDecodingStrategy` sur les colonnes Postgres `date` (sans heure).
/// Les CodingKeys de chaque modèle mappent déjà explicitement le snake_case
/// Postgres, donc aucune stratégie de conversion de clé n'est nécessaire ici.
enum SupabaseClientProvider {
    static let shared: SupabaseClient = SupabaseClient(
        supabaseURL: SupabaseConfig.url,
        supabaseKey: SupabaseConfig.anonKey
    )
}
