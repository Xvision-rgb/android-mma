package com.example.mmarecomp.util

import java.text.Normalizer

/** Normalisation des noms d'exercice.
 *
 *  Les noms sont saisis à la main, exercice par exercice, ou produits par le
 *  parser de programme. Rien ne garantit qu'un même mouvement s'écrive deux
 *  fois pareil : une majuscule d'un jour à l'autre, un espace de trop en fin
 *  de champ, ou un accent composé différemment (« é » en un seul caractère
 *  U+00E9, ou « e » suivi d'un accent combinant U+0301 — visuellement
 *  identiques, différents pour `equals`).
 *
 *  Sans clé commune, tout ce qui rapproche deux séances du même mouvement
 *  échoue en silence : la force relative affiche deux lignes pour un seul
 *  exercice avec deux 1RM contradictoires, le préremplissage de charge ne
 *  retrouve pas la dernière valeur connue, et le record personnel ne se
 *  déclenche pas.
 *
 *  [cle] sert aux comparaisons et aux regroupements, jamais à l'affichage.
 *  [propre] sert à ce qu'on enregistre et montre : on garde la casse de
 *  l'utilisateur, on enlève seulement ce qui est invisible. */
object ExerciseName {

    /** Clé de comparaison : accents unifiés, espaces des deux bouts retirés,
     *  espaces internes multiples réduits, casse ignorée. */
    fun cle(nom: String): String =
        Normalizer.normalize(nom, Normalizer.Form.NFC)
            .trim()
            .replace(WHITESPACE, " ")
            .lowercase()

    /** Forme à enregistrer et à afficher : mêmes nettoyages invisibles, mais
     *  la casse choisie par l'utilisateur est préservée. */
    fun propre(nom: String): String =
        Normalizer.normalize(nom, Normalizer.Form.NFC)
            .trim()
            .replace(WHITESPACE, " ")

    /** Deux noms désignent-ils le même mouvement ? */
    fun memeExercice(a: String, b: String): Boolean = cle(a) == cle(b)

    private val WHITESPACE = Regex("\\s+")
}
