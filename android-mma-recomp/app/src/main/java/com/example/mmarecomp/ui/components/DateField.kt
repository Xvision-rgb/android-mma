package com.example.mmarecomp.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val displayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

private fun relativeLabel(date: LocalDate): String = when (ChronoUnit.DAYS.between(date, LocalDate.now())) {
    0L -> "Aujourd'hui"
    1L -> "Hier"
    else -> date.format(displayFormatter)
}

/** Champ texte en lecture seule qui ouvre le DatePicker natif Android au tap.
 *  `maxDate` empêche par défaut de sélectionner une date future (log rétroactif
 *  uniquement) — passer `null` pour un champ sans limite. */
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = LocalDate.now(),
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.clickable {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day -> onDateChange(LocalDate.of(year, month + 1, day)) },
                date.year, date.monthValue - 1, date.dayOfMonth,
            )
            maxDate?.let {
                dialog.datePicker.maxDate = it.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli() + 24L * 60 * 60 * 1000 - 1
            }
            dialog.show()
        },
    ) {
        OutlinedTextField(
            value = relativeLabel(date),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                if (date != LocalDate.now()) {
                    TextButton(onClick = { onDateChange(LocalDate.now()) }) { Text("Aujourd'hui") }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
