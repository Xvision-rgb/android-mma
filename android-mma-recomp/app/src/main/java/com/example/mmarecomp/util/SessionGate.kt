package com.example.mmarecomp.util

import io.github.jan.supabase.auth.status.SessionStatus

/** Évite de démonter l'UI principale pendant les états auth transitoires
 *  (Initializing, refresh token) au retour au premier plan. */
object SessionGate {
    fun shouldShowAuthenticatedShell(status: SessionStatus, cachedUserId: String?): Boolean =
        when (status) {
            is SessionStatus.Authenticated -> true
            is SessionStatus.NotAuthenticated -> false
            is SessionStatus.Initializing -> cachedUserId != null
            is SessionStatus.RefreshFailure -> cachedUserId != null
        }
}
