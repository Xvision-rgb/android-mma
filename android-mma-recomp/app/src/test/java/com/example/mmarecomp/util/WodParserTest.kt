package com.example.mmarecomp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WodParserTest {

    @Test
    fun `empty text yields no movements`() {
        assertTrue(WodParser.parse("").isEmpty())
    }

    @Test
    fun `text with no known movement yields nothing, never blocks free-form input`() {
        assertTrue(WodParser.parse("machin truc bidule").isEmpty())
    }

    @Test
    fun `numbered movements are detected with their quantity`() {
        val result = WodParser.parse("15 burpees, 20 squats")

        assertEquals(2, result.size)
        assertEquals(ParsedWodMovement("Burpees", 15), result[0])
        assertEquals(ParsedWodMovement("Squats", 20), result[1])
    }

    @Test
    fun `movements without a number are still detected via the fallback keyword pass`() {
        val result = WodParser.parse("Burpees et squats aujourd'hui, pas de compte précis")

        assertEquals(listOf("Burpees", "Squats"), result.map { it.nom })
        assertTrue(result.all { it.quantite == null })
    }

    @Test
    fun `each movement is only reported once even if mentioned twice`() {
        val result = WodParser.parse("10 burpees puis encore des burpees")
        assertEquals(1, result.count { it.nom == "Burpees" })
    }

    @Test
    fun `is case-insensitive`() {
        val result = WodParser.parse("10 BURPEES")
        assertEquals(1, result.size)
        assertEquals("Burpees", result[0].nom)
    }
}
