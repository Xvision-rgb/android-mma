package com.example.mmarecomp.util

import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType

/**
 * Choix de la pesée de référence pour tout calcul qui dépend du poids
 * (cibles caloriques, maintenance, %BF).
 *
 * Une pesée du soir (repas, eau, entraînement) est typiquement 1 à 2 kg
 * au-dessus du matin à jeun : l'utiliser comme base gonfle la maintenance
 * et transforme une recomposition en surplus involontaire.
 */
object WeighInSelector {

    /** Dernière pesée matin à jeun avec un poids réel. */
    fun latestMorning(weighIns: List<WeighIn>): WeighIn? =
        weighIns
            .filter { it.type == WeighInType.MatinJeun && it.poidsKg > 0.0 }
            .maxByOrNull { it.date }

    /**
     * Pesée de référence pour une cible calorique : matin à jeun d'abord,
     * dernière pesée quelconque seulement s'il n'existe encore aucune
     * mesure du matin — mieux qu'une formule générique figée, sans
     * préférer un soir plus récent à un matin déjà connu.
     */
    fun latestReference(weighIns: List<WeighIn>): WeighIn? =
        latestMorning(weighIns)
            ?: weighIns.filter { it.poidsKg > 0.0 }.maxByOrNull { it.date }
}
