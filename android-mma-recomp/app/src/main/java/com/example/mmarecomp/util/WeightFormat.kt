package com.example.mmarecomp.util

import com.example.mmarecomp.data.WeightUnit

private const val KG_TO_LB = 2.20462

/** Formatage d'affichage uniquement — les données restent toujours stockées
 *  et saisies en kg (schéma Supabase, champs de saisie) ; seule la
 *  représentation textuelle change selon la préférence "Unités de poids",
 *  pour éviter tout risque de confusion sur ce qui est réellement envoyé
 *  au serveur. */
fun formatWeight(kg: Double, unit: WeightUnit): String = when (unit) {
    WeightUnit.KG -> "%.1f kg".format(kg)
    WeightUnit.LB -> "%.1f lb".format(kg * KG_TO_LB)
}
