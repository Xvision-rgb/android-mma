package com.example.mmarecomp.util

import java.io.IOException

/**
 * Erreurs **réseau / transport** qui justifient cache local ou file outbox.
 * Les erreurs API métier / schéma (table manquante, RLS, validation) ne doivent
 * PAS être enqueuées — elles doivent remonter à l'opérateur.
 */
fun Throwable.isOfflineEnqueueable(): Boolean {
    val chain = generateSequence(this) { it.cause }.toList()
    if (chain.any { it is IOException }) return true
    if (chain.any {
            val name = it.javaClass.name
            name.contains("HttpRequestTimeoutException", ignoreCase = true) ||
                name.contains("ConnectTimeoutException", ignoreCase = true) ||
                name.contains("SocketTimeoutException", ignoreCase = true)
        }
    ) {
        return true
    }
    val msg = chain.mapNotNull { it.message }.joinToString(" ")
    return msg.contains("timeout", ignoreCase = true) &&
        !msg.contains("PGRST", ignoreCase = true) &&
        !isMissingDailyCheckInTable()
}

/** Table `daily_checkins` absente ou schéma Supabase pas à jour (migration 006/009). */
fun Throwable.isMissingDailyCheckInTable(): Boolean {
    val msg = generateSequence(this) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
    return msg.contains("daily_checkins", ignoreCase = true) ||
        (msg.contains("42P01") && msg.contains("checkin", ignoreCase = true)) ||
        (msg.contains("Could not find the table", ignoreCase = true) &&
            msg.contains("checkin", ignoreCase = true)) ||
        (msg.contains("Could not find the table", ignoreCase = true) &&
            msg.contains("daily_check", ignoreCase = true))
}

enum class CheckInSaveError {
    NETWORK,
    SCHEMA,
    OTHER,
}

fun Throwable.toCheckInSaveError(): CheckInSaveError = when {
    isMissingDailyCheckInTable() -> CheckInSaveError.SCHEMA
    isOfflineEnqueueable() || isNetworkError() -> CheckInSaveError.NETWORK
    else -> CheckInSaveError.OTHER
}
