package com.example.mmarecomp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mmarecomp.util.ReadinessAction

/** Langage couleur de l'état, volontairement étroit : trois tons, toujours
 *  les mêmes, réutilisés partout où l'app signifie « ça va / attention /
 *  charge haute ».
 *
 *  Le principe vient des tableaux de bord de récupération (WHOOP, Oura) :
 *  un vocabulaire de trois couleurs répété à l'identique sur tous les écrans
 *  s'apprend une fois et vaut ensuite partout. Ici il n'introduit AUCUNE
 *  teinte nouvelle — les trois tons sont les rôles Material déjà en place
 *  (tertiary/Moss, secondary/Clay, primary/Steel), simplement nommés par ce
 *  qu'ils veulent dire plutôt que par leur position dans la palette.
 *
 *  Ce mapping vivait en local dans `RecoveryReadinessCard` ; il remonte ici
 *  pour que les autres usages d'état (ACWR, écarts aux objectifs, bannières)
 *  parlent la même langue. */
enum class SemanticTone {
    /** Nominal : rien à signaler, on suit le plan. */
    NOMINAL,

    /** Vigilance : un ajustement est conseillé, sans gravité. */
    VIGILANCE,

    /** Charge haute : la séance est allégée ou en déload. */
    CHARGE,
}

@Composable
fun semanticColor(tone: SemanticTone): Color = when (tone) {
    SemanticTone.NOMINAL -> MaterialTheme.colorScheme.tertiary
    SemanticTone.VIGILANCE -> MaterialTheme.colorScheme.secondary
    SemanticTone.CHARGE -> MaterialTheme.colorScheme.primary
}

/** Ton associé à la modulation du jour. L'ordre suit la sévérité, mais
 *  aucun de ces états n'est un échec : la règle produit reste qu'une
 *  mauvaise journée allège la séance, elle ne la supprime jamais. */
fun readinessTone(action: ReadinessAction): SemanticTone = when (action) {
    ReadinessAction.NOMINALE -> SemanticTone.NOMINAL
    ReadinessAction.VOLUME_REDUIT -> SemanticTone.VIGILANCE
    ReadinessAction.ALLEGEE, ReadinessAction.DELOAD -> SemanticTone.CHARGE
}

@Composable
fun readinessColor(action: ReadinessAction): Color = semanticColor(readinessTone(action))
