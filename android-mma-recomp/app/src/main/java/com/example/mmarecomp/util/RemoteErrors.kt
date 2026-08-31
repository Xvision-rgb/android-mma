package com.example.mmarecomp.util

import java.io.IOException

/** Erreurs réseau ou API Supabase/PostgREST qui justifient une mise en file
 *  hors-ligne plutôt qu'un échec bloquant côté UI. */
fun Throwable.isOfflineEnqueueable(): Boolean {
    if (this is IOException) return true
    val name = javaClass.name
    if (name.contains("RestException", ignoreCase = true)) return true
    val msg = message.orEmpty()
    return msg.contains("PGRST", ignoreCase = true) ||
        msg.contains("42P01", ignoreCase = true) ||
        msg.contains("daily_checkins", ignoreCase = true) ||
        msg.contains("Could not find the table", ignoreCase = true)
}

/** Table `daily_checkins` absente ou schéma Supabase pas à jour (migration 006). */
fun Throwable.isMissingDailyCheckInTable(): Boolean {
    val msg = message.orEmpty()
    return msg.contains("daily_checkins", ignoreCase = true) ||
        msg.contains("42P01", ignoreCase = true) ||
        msg.contains("Could not find the table", ignoreCase = true)
}

enum class CheckInSaveError {
    NETWORK,
    SCHEMA,
    OTHER,
}

fun Throwable.toCheckInSaveError(): CheckInSaveError = when {
    isMissingDailyCheckInTable() -> CheckInSaveError.SCHEMA
    isNetworkError() -> CheckInSaveError.NETWORK
    else -> CheckInSaveError.OTHER
}
