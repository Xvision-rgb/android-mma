package com.example.mmarecomp.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNormalizeTest {

    @Test
    fun `accents are ignored`() {
        assertTrue(matchesSearch("Crème", "creme"))
        assertTrue(matchesSearch("Épinards cuits", "epinards"))
    }

    @Test
    fun `case is ignored`() {
        assertTrue(matchesSearch("Poulet (blanc, cuit)", "POULET"))
    }

    @Test
    fun `oe ligature matches the plain oe spelling`() {
        assertTrue(matchesSearch("Œuf entier", "oeuf"))
        assertTrue(matchesSearch("Œuf entier", "OEUF"))
    }

    @Test
    fun `partial matches still work`() {
        assertTrue(matchesSearch("Bœuf haché 5%", "boeuf"))
    }

    @Test
    fun `unrelated queries do not match`() {
        assertFalse(matchesSearch("Riz blanc cuit", "poulet"))
    }
}
