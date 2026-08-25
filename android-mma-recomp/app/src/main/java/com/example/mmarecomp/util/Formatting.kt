package com.example.mmarecomp.util

import java.util.Locale

/** Formatage numérique cohérent (point décimal) — indépendant de la locale
 *  système, pour matcher le parsing (`replace(",", ".")`) utilisé partout
 *  dans les formulaires de saisie. */
object Formatting {
    fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
}
