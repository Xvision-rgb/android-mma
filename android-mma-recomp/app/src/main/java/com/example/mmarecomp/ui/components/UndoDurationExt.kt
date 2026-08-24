package com.example.mmarecomp.ui.components

import androidx.compose.material3.SnackbarDuration
import com.example.mmarecomp.data.UndoDuration

/** Traduit la préférence "Durée du snackbar Annuler" vers les trois seules
 *  durées que Material3 propose — pas de secondes arbitraires. */
fun UndoDuration.toSnackbarDuration(): SnackbarDuration = when (this) {
    UndoDuration.SHORT -> SnackbarDuration.Short
    UndoDuration.NORMAL -> SnackbarDuration.Long
    UndoDuration.LONG -> SnackbarDuration.Indefinite
}
