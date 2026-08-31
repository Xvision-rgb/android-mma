package com.example.mmarecomp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RemoteErrorsTest {

    @Test
    fun `IOException est enqueueable hors-ligne`() {
        assertTrue(IOException("timeout").isOfflineEnqueueable())
        assertEquals(CheckInSaveError.NETWORK, IOException("timeout").toCheckInSaveError())
    }

    @Test
    fun `table daily_checkins manquante n est PAS enqueueable`() {
        val error = RuntimeException(
            "Could not find the table 'public.daily_checkins' in the schema cache",
        )
        assertFalse(error.isOfflineEnqueueable())
        assertTrue(error.isMissingDailyCheckInTable())
        assertEquals(CheckInSaveError.SCHEMA, error.toCheckInSaveError())
    }

    @Test
    fun `RestException generique n est pas enqueueable`() {
        val error = object : RuntimeException("PGRST116") {
            // Simule une erreur API métier (pas réseau).
        }
        // Nom de classe sans RestException → OTHER, non enqueueable via message PGRST seul.
        assertFalse(error.isOfflineEnqueueable())
        assertEquals(CheckInSaveError.OTHER, error.toCheckInSaveError())
    }

    @Test
    fun `erreur metier non reseau n est pas enqueueable`() {
        assertFalse(IllegalStateException("bug local").isOfflineEnqueueable())
        assertEquals(CheckInSaveError.OTHER, IllegalStateException().toCheckInSaveError())
    }
}
