package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.readinessColor
import com.example.mmarecomp.util.ModulationApplier
import com.example.mmarecomp.util.ModulationSeance
import com.example.mmarecomp.util.ReadinessAction

@Composable
fun SessionModulationBanner(
    modulation: ModulationSeance,
    scoreReadiness: Int?,
    aCheckInAujourdhui: Boolean,
    modulationApplied: Boolean,
    resumeModulation: List<String> = emptyList(),
    peutAppliquer: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val couleur = readinessColor(modulation.action)
    val actions = ModulationApplier.actionsConcretes(modulation)

    AppCard(modifier = modifier) {
        Text(
            "Modulation du jour",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modulation.action.label,
            style = MaterialTheme.typography.titleMedium,
            color = couleur,
        )
        Text(
            modulation.explication,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!aCheckInAujourdhui) {
            Text(
                "Pas encore de point aujourd'hui — la modulation se base surtout sur ta charge récente. " +
                    "Fais le point depuis l'Accueil pour affiner.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            scoreReadiness?.let {
                Text(
                    "Score de forme : $it/25",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            Text("Actions prévues :", style = MaterialTheme.typography.labelSmall)
            actions.forEach { action ->
                Text(
                    "• $action",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            modulationApplied -> {
                Text(
                    "Modulation appliquée à cette séance ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                resumeModulation.forEach { ligne ->
                    Text(
                        "• $ligne",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            modulation.action == ReadinessAction.NOMINALE -> {
                Text(
                    "Rien à modifier — tu peux t'entraîner comme prévu.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            !peutAppliquer -> {
                Text(
                    "Charge d'abord ton plan ou ajoute des exercices, puis applique la modulation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                    Text("Appliquer à ma séance")
                }
            }
        }
    }
}
