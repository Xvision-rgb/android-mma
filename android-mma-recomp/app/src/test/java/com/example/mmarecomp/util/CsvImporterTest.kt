package com.example.mmarecomp.util

import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInContext
import com.example.mmarecomp.model.WeighInType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    private fun weighIn(
        date: String = "2026-01-01",
        poidsKg: Double = 80.0,
        type: WeighInType = WeighInType.MatinJeun,
        bfPct: Double? = 15.0,
        contexte: WeighInContext = WeighInContext(),
    ) = WeighIn(
        id = "id",
        userId = "user",
        date = date,
        heure = "07:30:00",
        type = type,
        poidsKg = poidsKg,
        bfPct = bfPct,
        contexte = contexte,
    )

    @Test
    fun `round-trips through the exporter's own format`() {
        val csv = CsvExporter.weighInsToCsv(listOf(weighIn(date = "2026-03-01", poidsKg = 82.5)))
        val result = CsvImporter.parseWeighIns(csv)
        assertEquals(1, result.parsed.size)
        assertEquals(0, result.skipped)
        assertEquals("2026-03-01", result.parsed.first().date)
        assertEquals(82.5, result.parsed.first().poidsKg, 0.0001)
        assertEquals(WeighInType.MatinJeun, result.parsed.first().type)
    }

    @Test
    fun `columns can be in a different order than the exporter's canonical one`() {
        val csv = "poids_kg,date,type,heure\n80.0,2026-01-05,matin_jeun,07:00:00\n"
        val result = CsvImporter.parseWeighIns(csv)
        assertEquals(1, result.parsed.size)
        assertEquals("2026-01-05", result.parsed.first().date)
        assertEquals(80.0, result.parsed.first().poidsKg, 0.0001)
    }

    @Test
    fun `missing a required column skips every row instead of crashing`() {
        val csv = "date,heure,type\n2026-01-01,07:00:00,matin_jeun\n"
        val result = CsvImporter.parseWeighIns(csv)
        assertTrue(result.parsed.isEmpty())
        assertEquals(1, result.skipped)
    }

    @Test
    fun `a row with an unparseable weight is skipped, not the whole import`() {
        val csv = "date,heure,type,poids_kg\n2026-01-01,07:00:00,matin_jeun,quatre-vingts\n2026-01-02,07:00:00,matin_jeun,79.5\n"
        val result = CsvImporter.parseWeighIns(csv)
        assertEquals(1, result.parsed.size)
        assertEquals(1, result.skipped)
        assertEquals("2026-01-02", result.parsed.first().date)
    }

    @Test
    fun `a row with an unknown weigh-in type is skipped`() {
        val csv = "date,heure,type,poids_kg\n2026-01-01,07:00:00,midi,80.0\n"
        val result = CsvImporter.parseWeighIns(csv)
        assertTrue(result.parsed.isEmpty())
        assertEquals(1, result.skipped)
    }

    @Test
    fun `a quoted field containing a comma is parsed as a single value`() {
        val csv = "date,heure,type,poids_kg,bf_pct\n\"2026,01,01\",07:00:00,matin_jeun,80.0,15.0\n"
        val result = CsvImporter.parseWeighIns(csv)
        assertEquals(1, result.parsed.size)
        assertEquals("2026,01,01", result.parsed.first().date)
    }

    @Test
    fun `boolean context flags default to false when the column is missing or malformed`() {
        val csv = "date,heure,type,poids_kg\n2026-01-01,07:00:00,matin_jeun,80.0\n"
        val result = CsvImporter.parseWeighIns(csv)
        val contexte = result.parsed.first().contexte
        assertEquals(false, contexte.creatineRecente)
        assertEquals(false, contexte.alcoolRecent)
        assertEquals(false, contexte.postTraining)
    }

    @Test
    fun `an empty file produces no parsed rows and no skips`() {
        val result = CsvImporter.parseWeighIns("")
        assertTrue(result.parsed.isEmpty())
        assertEquals(0, result.skipped)
    }
}
