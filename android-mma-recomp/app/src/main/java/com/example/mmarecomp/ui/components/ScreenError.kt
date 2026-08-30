package com.example.mmarecomp.ui.components

/** Type d'opération qui a échoué — pilote le libellé de retry dans
 *  [ErrorBanner] (recharger ≠ réenregistrer ≠ resupprimer). */
enum class ErrorOperation {
    LOAD,
    SAVE,
    DELETE,
    UPDATE,
}

data class ScreenError(
    val message: String,
    val operation: ErrorOperation,
)
