package com.example.mmarecomp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `weekdayIso LocalDate suit ISO lundi=1`() {
        // 2026-08-31 est un lundi
        assertEquals(1, DateUtils.weekdayIso(LocalDate.of(2026, 8, 31)))
        assertEquals(7, DateUtils.weekdayIso(LocalDate.of(2026, 8, 30)))
    }

    @Test
    fun `weekdayIsoOrNull refuse une date invalide au lieu de retomber sur lundi`() {
        assertNull(DateUtils.weekdayIsoOrNull("pas-une-date"))
        assertEquals(1, DateUtils.weekdayIsoOrNull("2026-08-31"))
    }
}
