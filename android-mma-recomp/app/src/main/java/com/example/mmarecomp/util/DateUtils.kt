package com.example.mmarecomp.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun string(date: LocalDate): String = date.format(isoFormatter)

    fun date(string: String): LocalDate? = runCatching { LocalDate.parse(string, isoFormatter) }.getOrNull()

    fun today(): String = string(LocalDate.now())

    fun daysAgo(n: Long): String = string(LocalDate.now().minusDays(n))

    /** 1 = lundi ... 7 = dimanche, pour matcher `training_plan.jour_semaine`. */
    fun weekdayIso(dateString: String): Int =
        date(dateString)?.dayOfWeek?.value ?: DayOfWeek.MONDAY.value

    fun startOfWeek(from: LocalDate = LocalDate.now()): String =
        string(from.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
}
