package com.example.mmarecomp.ui.theme

import androidx.compose.ui.unit.dp

/** Tokens d'espacement/formes partagés — évite les valeurs .dp éparpillées
 *  et garde une grille cohérente entre écrans.
 *
 *  La règle « pas de valeurs codées en dur » existait déjà, mais `Dimens` ne
 *  couvrait pas assez de cas pour être tenable : rayons, élévations, tailles
 *  de pastille et hauteurs de graphe s'écrivaient donc en dur (18 `16.dp`,
 *  12 `6.dp`, 10 `8.dp` recensés dans l'audit). Les manques sont comblés ici
 *  pour que la règle devienne applicable. */
object Dimens {
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp

    val cornerSm = 10.dp
    val cornerMd = 16.dp
    val cornerLg = 24.dp
    /** Forme « pilule » (chips, badges) — grand rayon volontairement borné. */
    val cornerPill = 999.dp

    /** Élévation des cartes. En sombre, `surface` (#1C1C1E) sur `background`
     *  (#14171C) sépare trop peu : l'élévation et la bordure fine ci-dessous
     *  font le travail que la seule différence de teinte ne faisait pas. */
    val cardElevation = 1.dp
    val cardElevationRaised = 3.dp
    val borderHairline = 1.dp

    /** Padding intérieur d'une carte, et interligne entre ses lignes. */
    val cardPadding = 16.dp
    val cardContentSpacing = 8.dp

    /** Pastille de couleur sémantique (type de séance, état du jour) —
     *  la même taille était recopiée en 8.dp et 10.dp selon les écrans. */
    val dotSm = 8.dp
    val dotMd = 10.dp

    val iconSm = 20.dp
    val iconMd = 32.dp

    val chartHeight = 140.dp
    val ringSize = 110.dp

    /** Marge basse des listes défilantes pour dégager le FAB et la barre. */
    val scrollBottomInset = 96.dp

    /** Taille minimale recommandée pour une cible tactile (Material / WCAG).
     *  En salle, doigts moites entre deux séries, c'est un plancher et non
     *  un objectif — les cibles de saisie de série visent plus large. */
    val minTouchTarget = 48.dp
}
