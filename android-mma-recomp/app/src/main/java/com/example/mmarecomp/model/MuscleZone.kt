package com.example.mmarecomp.model

/** Zones de travail et part cible du volume hebdomadaire.
 *
 *  La répartition n'est pas neutre : elle encode un objectif de force relative
 *  et de densité (archétype lutteur) plutôt que d'hypertrophie équilibrée.
 *  D'où un ratio tirage:poussée d'environ 2:1, et le cou/poigne traité comme
 *  une zone à part entière — pas comme une finition de séance. */
enum class MuscleZone(val label: String, val partCible: Double) {
    TIRAGE("Tirage / dos", 0.35),
    CHAINE_POSTERIEURE("Chaîne postérieure", 0.25),
    COU_POIGNE("Cou / poigne", 0.15),
    POUSSEE("Poussée", 0.15),
    QUADS_BRAS("Quadriceps / bras", 0.10),
    ;

    companion object {
        /** Ratio tirage:poussée visé — l'écart le plus structurant entre un
         *  physique de salle et un physique de lutteur. */
        const val RATIO_TIRAGE_POUSSEE_CIBLE = 2.0
    }
}
