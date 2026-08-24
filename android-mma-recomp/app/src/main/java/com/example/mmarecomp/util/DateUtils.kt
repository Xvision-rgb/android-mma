package com.example.mmarecomp.util

import com.example.mmarecomp.data.DateFormatStyle
import com.example.mmarecomp.data.WeekStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val frFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun string(date: LocalDate): String = date.format(isoFormatter)

    fun date(string: String): LocalDate? = runCatching { LocalDate.parse(string, isoFormatter) }.getOrNull()

    fun today(): String = string(LocalDate.now())

    fun daysAgo(n: Long): String = string(LocalDate.now().minusDays(n))

    /** Formate une date ISO stockée pour l'affichage selon la préférence
     *  "Format de date" — le stockage reste toujours ISO, seul le texte
     *  montré à l'utilisateur change. */
    fun forDisplay(isoDateString: String, format: DateFormatStyle): String {
        val parsed = date(isoDateString) ?: return isoDateString
        return if (format == DateFormatStyle.FR) parsed.format(frFormatter) else isoDateString
    }

    /** 1 = lundi ... 7 = dimanche, pour matcher `training_plan.jour_semaine` —
     *  toujours ISO/lundi, indépendant de la préférence d'affichage
     *  "premier jour de semaine" qui ne concerne que les calculs de fenêtre
     *  d'affichage (dashboard), jamais ce mapping de schéma. */
    fun weekdayIso(dateString: String): Int =
        date(dateString)?.dayOfWeek?.value ?: DayOfWeek.MONDAY.value

    /** Début de la semaine "affichée" (fenêtre du dashboard) — lundi par
     *  défaut, dimanche si l'utilisateur préfère cette convention. */
    fun startOfWeek(from: LocalDate = LocalDate.now(), weekStart: WeekStart = WeekStart.MONDAY): String {
        val firstDay = if (weekStart == WeekStart.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        return string(from.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay)))
    }
}
