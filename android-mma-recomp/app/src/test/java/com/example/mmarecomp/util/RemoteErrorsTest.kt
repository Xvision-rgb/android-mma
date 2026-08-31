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
    }

    @Test
    fun `table daily_checkins manquante est detectee`() {
        val error = RuntimeException(
            "Could not find the table 'public.daily_checkins' in the schema cache",
        )
        assertTrue(error.isOfflineEnqueueable())
        assertTrue(error.isMissingDailyCheckInTable())
        assertEquals(CheckInSaveError.SCHEMA, error.toCheckInSaveError())
    }

    @Test
    fun `erreur metier non reseau n est pas enqueueable`() {
        assertFalse(IllegalStateException("bug local").isOfflineEnqueueable())
        assertEquals(CheckInSaveError.OTHER, IllegalStateException().toCheckInSaveError())
    }
}
