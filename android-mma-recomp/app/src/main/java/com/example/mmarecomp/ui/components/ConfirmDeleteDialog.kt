package com.example.mmarecomp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Confirmation explicite avant suppression — alternative à l'undo-snackbar
 *  pour qui préfère être arrêté avant plutôt que pouvoir annuler après
 *  (préférence "Confirmer avant suppression"). `item` nul = dialogue fermé. */
@Composable
fun <T> ConfirmDeleteDialog(item: T?, title: String, onConfirm: (T) -> Unit, onDismiss: () -> Unit) {
    if (item != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            confirmButton = { TextButton(onClick = { onConfirm(item) }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        )
    }
}
