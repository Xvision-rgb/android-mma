package com.example.mmarecomp.util

import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.RepasSlot

/** Libellé affiché pour une phase — le nom personnalisé de la préférence
 *  "Renommer les phases" s'il existe, sinon le libellé par défaut. Ne
 *  change jamais la valeur envoyée à Supabase (Phase.value), seulement ce
 *  que l'utilisateur voit. */
fun Phase.displayLabel(overrides: Map<String, String>): String = overrides[name]?.takeIf { it.isNotBlank() } ?: label

/** Même principe pour les créneaux repas — RepasSlot.value (l'entier stocké
 *  en base) ne change jamais, seul le texte affiché est personnalisable. */
fun RepasSlot.displayLabel(overrides: Map<String, String>): String = overrides[name]?.takeIf { it.isNotBlank() } ?: label
