#!/usr/bin/env bash
#
# Bootstrap idempotent du projet Android pour un Cloud Agent.
# Exécuté depuis android-mma-recomp/ (voir .cursor/environment.json -> install).
#
# Responsabilités :
#   1. Pointer Gradle vers l'Android SDK (local.properties).
#   2. Générer SupabaseConfig.kt (gitignored) depuis les variables d'env ou l'exemple.
#   3. Réchauffer le cache Gradle (dépendances AGP/AndroidX) + valider build & tests.
#
# Ne contient aucun secret en clair : les identifiants viennent de l'environnement.
set -euo pipefail

log() { printf '[cloud-agent-install] %s\n' "$*"; }

# --- 0. Contexte ------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_DIR}"
log "Projet : ${PROJECT_DIR}"

# --- 1. Android SDK ---------------------------------------------------------
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
if [[ ! -d "${SDK_DIR}/platforms" ]]; then
  log "ERREUR : Android SDK introuvable dans '${SDK_DIR}'." >&2
  log "        Vérifie que l'image Docker (.cursor/Dockerfile) l'a bien installé." >&2
  exit 1
fi
printf 'sdk.dir=%s\n' "${SDK_DIR}" > local.properties
log "local.properties -> sdk.dir=${SDK_DIR}"

# --- 2. Configuration Supabase (gitignored) ---------------------------------
CONFIG_PKG="app/src/main/java/com/example/mmarecomp"
CONFIG_FILE="${CONFIG_PKG}/SupabaseConfig.kt"
SUPABASE_URL="${SUPABASE_URL:-https://YOUR-PROJECT.supabase.co}"
SUPABASE_ANON_KEY="${SUPABASE_ANON_KEY:-YOUR-SUPABASE-ANON-KEY}"
cat > "${CONFIG_FILE}" <<EOF
package com.example.mmarecomp

// Généré par scripts/cloud-agent-install.sh — NE PAS COMMIT (gitignored).
// Valeurs issues des variables d'environnement SUPABASE_URL / SUPABASE_ANON_KEY,
// sinon placeholders permettant la compilation.
object SupabaseConfig {
    const val URL = "${SUPABASE_URL}"
    const val ANON_KEY = "${SUPABASE_ANON_KEY}"
}
EOF
if [[ "${SUPABASE_URL}" == https://YOUR-PROJECT.supabase.co ]]; then
  log "SupabaseConfig.kt : placeholders (définis SUPABASE_URL / SUPABASE_ANON_KEY pour un vrai backend)."
else
  log "SupabaseConfig.kt : identifiants injectés depuis l'environnement."
fi

# --- 3. Réchauffe Gradle + validation ---------------------------------------
log "Build + tests unitaires JVM (réchauffe le cache Gradle)…"
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest

log "Bootstrap terminé : APK debug construit et tests unitaires OK."
