package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Contexte de pratique actuel.
 *
 *  L'app supposait partout une pratique de combat en parallèle : détection de
 *  conflit avec le sparring, multiplicateur d'activité calibré sur 6-7 séances
 *  dont du MMA, priorité donnée à la fraîcheur neuromusculaire. Quand le club
 *  ferme, ces hypothèses deviennent fausses — et surtout, elles font passer
 *  à côté de l'opportunité : sans combat, la récupération disponible pour la
 *  salle augmente nettement, et c'est la meilleure fenêtre pour construire une
 *  base de force qu'un fight camp ne permet jamais.
 *
 *  Le multiplicateur d'activité reflète la dépense hors salle, pas le nombre
 *  de séances de musculation : c'est le sparring et le cardio de combat qui
 *  font la différence, pas trois séries de squat de plus. */
@Serializable
enum class ContexteSportif(
    val value: String,
    val label: String,
    val multiplicateurActivite: Double,
    val description: String,
) {
    @SerialName("salle_uniquement")
    SalleUniquement(
        "salle_uniquement",
        "Salle et extérieur",
        1.4,
        "Musculation et course, sans sport de combat. Fenêtre idéale pour construire la force et la masse.",
    ),

    @SerialName("avec_combat")
    AvecCombat(
        "avec_combat",
        "Avec sport de combat",
        1.6,
        "Sparring ou grappling en parallèle. La récupération se partage, la fraîcheur neuromusculaire devient prioritaire.",
    ),
    ;

    val sansCombat: Boolean get() = this == SalleUniquement
}
