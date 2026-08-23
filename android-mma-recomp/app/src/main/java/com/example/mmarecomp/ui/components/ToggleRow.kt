package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Ligne label + interrupteur où toute la ligne est la cible tactile — pas
 * seulement le petit Switch — pour un ciblage plus facile et une meilleure
 * cible d'accessibilité. Le clic est porté par `Modifier.toggleable` sur la
 * Row (qui expose déjà l'état et le rôle Switch aux services
 * d'accessibilité) ; le Switch lui-même n'a donc pas son propre callback de
 * clic, pour éviter de doubler le geste.
 */
@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}
