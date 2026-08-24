package com.example.mmarecomp.util

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMessagesTest {

    @Test
    fun `network failures get a connectivity message`() {
        val result = IOException("connect timed out").toFriendlyMessage("fallback")
        assertEquals("Pas de connexion internet — réessaie dans un instant.", result)
    }

    @Test
    fun `duplicate key errors get an explanatory message instead of raw SQL`() {
        val result = Exception("duplicate key value violates unique constraint \"weigh_ins_user_id_date_type_key\"")
            .toFriendlyMessage("fallback")
        assertEquals("Une entrée existe déjà pour cette date — vérifie qu'elle n'a pas déjà été enregistrée.", result)
    }

    @Test
    fun `check constraint violations get a validation message`() {
        val result = Exception("new row violates check constraint \"meals_calories_check\"").toFriendlyMessage("fallback")
        assertEquals("Une des valeurs saisies est invalide.", result)
    }

    @Test
    fun `expired session errors mention reconnecting`() {
        val result = Exception("JWT expired").toFriendlyMessage("fallback")
        assertEquals("Ta session a expiré — reconnecte-toi.", result)
    }

    @Test
    fun `unrecognized errors fall back to the caller-provided message`() {
        val result = Exception("something completely unexpected").toFriendlyMessage("Impossible d'enregistrer.")
        assertEquals("Impossible d'enregistrer.", result)
    }
}
