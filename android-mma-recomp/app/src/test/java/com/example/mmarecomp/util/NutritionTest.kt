package com.example.mmarecomp.util

import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyAvailabilityTest {

    // 75 kg à 12 % de masse grasse -> 66 kg de masse maigre.
    private val masseMaigre = 66.0

    @Test
    fun `ea correcte au dessus de 30 kcal par kg de masse maigre`() {
        val ea = EnergyAvailability.calculer(
            apportKcal = 2800,
            depenseExerciceKcal = 500,
            masseMaigreKg = masseMaigre,
        )!!
        assertEquals(EaStatut.CORRECTE, ea.statut)
        assertNull(ea.apportPourSeuilKcal)
    }

    @Test
    fun `le cas que le plancher calorique ne voyait pas`() {
        // 1875 kcal = plancher de 25 kcal/kg de POIDS DE CORPS pour 75 kg.
        // Avec 800 kcal d'entraînement, l'EA réelle tombe sous 25.
        val ea = EnergyAvailability.calculer(
            apportKcal = 1875,
            depenseExerciceKcal = 800,
            masseMaigreKg = masseMaigre,
        )!!
        assertTrue(ea.kcalParKgMasseMaigre < EnergyAvailability.SEUIL_BAS)
        assertEquals(EaStatut.BASSE, ea.statut)
    }

    @Test
    fun `une grosse depense fait basculer une journee autrement correcte`() {
        val repos = EnergyAvailability.calculer(2400, 0, masseMaigre)!!
        val charge = EnergyAvailability.calculer(2400, 900, masseMaigre)!!
        assertEquals(EaStatut.CORRECTE, repos.statut)
        assertTrue(charge.kcalParKgMasseMaigre < repos.kcalParKgMasseMaigre)
        assertEquals(EaStatut.BASSE, charge.statut)
    }

    @Test
    fun `l apport cible remet exactement au seuil`() {
        val ea = EnergyAvailability.calculer(1500, 600, masseMaigre)!!
        val cible = ea.apportPourSeuilKcal!!
        val corrigee = EnergyAvailability.calculer(cible, 600, masseMaigre)!!
        assertEquals(EaStatut.CORRECTE, corrigee.statut)
    }

    @Test
    fun `masse maigre inconnue ne produit aucun calcul`() {
        assertNull(EnergyAvailability.calculer(2500, 400, 0.0))
    }

    @Test
    fun `le message d alerte propose une action et ne juge pas`() {
        val message = EnergyAvailability.calculer(1500, 800, masseMaigre)!!.message
        assertTrue(message.contains("glucides"))
        listOf("trop", "mauvais", "erreur", "faute").forEach {
            assertFalse("registre culpabilisant : $it", message.lowercase().contains(it))
        }
    }
}

class MacroFloorsTest {

    private val poids = 75.0

    @Test
    fun `les planchers suivent le poids de corps`() {
        val p = MacroFloors.planchers(poids)
        assertEquals(225, p.glucidesG) // 3,0 g/kg
        assertEquals(90, p.proteinesG) // 1,2 g/kg
        assertEquals(38, p.lipidesG) // 0,5 g/kg (arrondi)
    }

    @Test
    fun `une repartition correcte n est pas modifiee`() {
        val r = MacroFloors.appliquer(poids, glucidesG = 350, proteinesG = 150, lipidesG = 70)
        assertEquals(350, r.glucidesG)
        assertEquals(150, r.proteinesG)
        assertTrue(r.corrections.isEmpty())
    }

    @Test
    fun `un deficit tenu en coupant les glucides est corrige`() {
        val r = MacroFloors.appliquer(poids, glucidesG = 120, proteinesG = 150, lipidesG = 70)
        assertEquals(225, r.glucidesG)
        assertEquals(1, r.corrections.size)
        assertTrue(r.corrections.first().contains("Glucides"))
    }

    @Test
    fun `les calories suivent la correction plutot que d etre redistribuees`() {
        val avant = 120 * 4 + 150 * 4 + 70 * 9
        val r = MacroFloors.appliquer(poids, glucidesG = 120, proteinesG = 150, lipidesG = 70)
        assertTrue(r.caloriesTotales > avant)
        assertEquals(225 * 4 + 150 * 4 + 70 * 9, r.caloriesTotales)
    }

    @Test
    fun `plusieurs planchers peuvent etre releves ensemble`() {
        val r = MacroFloors.appliquer(poids, glucidesG = 50, proteinesG = 40, lipidesG = 10)
        assertEquals(3, r.corrections.size)
    }

    @Test
    fun `une descente trop rapide est signalee`() {
        // 1,5 kg perdus en 7 jours.
        assertNotNull(MacroFloors.alertePerteTropRapide(variationKg = -1.5, jours = 7))
        // 0,7 kg en 7 jours reste dans la cible.
        assertNull(MacroFloors.alertePerteTropRapide(variationKg = -0.7, jours = 7))
    }

    @Test
    fun `une prise de poids ne declenche pas l alerte de descente`() {
        assertNull(MacroFloors.alertePerteTropRapide(variationKg = 0.5, jours = 14))
    }

    @Test
    fun `pas d alerte sur une fenetre trop courte`() {
        assertNull(MacroFloors.alertePerteTropRapide(variationKg = -2.0, jours = 3))
    }

    @Test
    fun `la pause diete se propose apres huit semaines de deficit`() {
        assertNull(DietBreak.proposerPause(30))
        assertNotNull(DietBreak.proposerPause(60))
    }
}

class NutritionTargetCalculatorTest {

    private val slots = RepasSlot.entries.toList()

    @Test
    fun `les proteines sont reparties a plat et non au prorata des calories`() {
        val split = NutritionTargetCalculator.indicativeSplit(2800, 160.0, slots)
        val valeurs = split.values.map { it.proteinesG }.distinct()
        // Le bug corrigé : l'après-midi recevait 20 % et le post-training 30 %.
        assertEquals("les protéines doivent être uniformes", 1, valeurs.size)
        assertEquals(40.0, valeurs.first(), 0.05)
    }

    @Test
    fun `les calories suivent bien la part indicative du creneau`() {
        val split = NutritionTargetCalculator.indicativeSplit(2000, 160.0, slots)
        assertEquals(600, split[RepasSlot.PostTraining]!!.calories) // 30 %
        assertEquals(400, split[RepasSlot.ApresMidi]!!.calories) // 20 %
    }

    @Test
    fun `une liste de creneaux vide ne divise pas par zero`() {
        assertTrue(NutritionTargetCalculator.indicativeSplit(2000, 160.0, emptyList()).isEmpty())
    }

    @Test
    fun `un jour de repos vise le bas de la fourchette glucidique`() {
        val g = NutritionTargetCalculator.glucidesParKgPour(TypeJour.Repos, chargeInterne = 900.0)
        assertEquals(MacroFloors.GLUCIDES_CIBLE_BASSE_G_PAR_KG, g, 0.001)
    }

    @Test
    fun `une grosse charge pousse les glucides vers le haut de la fourchette`() {
        val leger = NutritionTargetCalculator.glucidesParKgPour(TypeJour.Training, 200.0)
        val lourd = NutritionTargetCalculator.glucidesParKgPour(TypeJour.Training, 900.0)
        assertTrue(lourd > leger)
        assertEquals(MacroFloors.GLUCIDES_CIBLE_HAUTE_G_PAR_KG, lourd, 0.001)
    }

    @Test
    fun `le swing glucidique depasse largement les anciens 150 kcal`() {
        val repos = NutritionTargetCalculator.targetFor(
            TypeJour.Repos, baseCalories = 2800, proteinesG = 150, lipidesG = 75,
            poidsKg = 75.0, chargeInterne = 0.0,
        )
        val charge = NutritionTargetCalculator.targetFor(
            TypeJour.Training, baseCalories = 2800, proteinesG = 150, lipidesG = 75,
            poidsKg = 75.0, chargeInterne = 900.0,
        )
        assertEquals(150.0, repos.proteinesG, 0.001)
        assertEquals(150.0, charge.proteinesG, 0.001)
        assertEquals(75.0, charge.lipidesG, 0.001)
        // 4 g/kg contre 7 g/kg à 75 kg = 225 g d'écart.
        assertEquals(225.0, charge.glucidesG - repos.glucidesG, 1.0)
    }

    @Test
    fun `sans poids connu on garde l ancien comportement calorique`() {
        val t = NutritionTargetCalculator.targetFor(TypeJour.Training, 2800, 150)
        assertEquals(2950, t.calories)
        assertEquals(0.0, t.glucidesG, 0.001)
    }

    @Test
    fun `une prise trop legere en proteines est signalee sans juger`() {
        assertNotNull(NutritionTargetCalculator.notePriseProteique(12.0))
        assertNull(NutritionTargetCalculator.notePriseProteique(25.0))
        assertNotNull(NutritionTargetCalculator.notePriseProteique(90.0))
    }

    @Test
    fun `les modes caloriques respectent l ordre bulk recomp coupe`() {
        val poids = 75.0
        val bf = 12.0
        val bulk = CalorieCalculator.goal(poids, bf, CalorieMode.Bulk)
        val recomp = CalorieCalculator.goal(poids, bf, CalorieMode.Recomposition)
        val cut = CalorieCalculator.goal(poids, bf, CalorieMode.Coupe)
        assertTrue(bulk.targetCalories > recomp.targetCalories)
        assertTrue(recomp.targetCalories > cut.targetCalories)
    }

    @Test
    fun `targetFor preserve l energie de baseCalories a 75 kg`() {
        val baseCalories = 2800
        val result = NutritionTargetCalculator.targetFor(
            TypeJour.Repos,
            baseCalories = baseCalories,
            proteinesG = 150,
            lipidesG = 75,
            poidsKg = 75.0,
            chargeInterne = 0.0,
        )
        assertEquals(baseCalories.toDouble(), result.calories.toDouble(), 20.0)
    }
}

class LoggingConfidenceTest {

    private fun repas(date: String, slot: Int) = Meal(
        id = "$date-$slot",
        userId = "u",
        date = date,
        repas = slot,
        calories = 600,
        proteinesG = 40.0,
        glucidesG = 60.0,
        lipidesG = 20.0,
    )

    @Test
    fun `un suivi complet autorise le recalibrage`() {
        val repas = (1..14).flatMap { j -> (1..4).map { repas("2026-08-%02d".format(j), it) } }
        val c = LoggingConfidence.evaluer(repas, jours = 14)
        assertEquals(NiveauConfiance.ELEVEE, c.niveau)
        assertTrue(c.autoriseRecalibrage)
    }

    @Test
    fun `un suivi trop partiel bloque le recalibrage`() {
        val repas = (1..14).map { j -> repas("2026-08-%02d".format(j), 1) }
        val c = LoggingConfidence.evaluer(repas, jours = 14)
        assertEquals(NiveauConfiance.INSUFFISANTE, c.niveau)
        assertFalse(c.autoriseRecalibrage)
    }

    @Test
    fun `loguer deux fois le meme creneau ne compte pas double`() {
        val doublons = List(50) { repas("2026-08-01", 1) }
        val c = LoggingConfidence.evaluer(doublons, jours = 14)
        assertEquals(1, c.prisesLoguees)
    }

    @Test
    fun `le recalibrage adaptatif refuse une fenetre mal loguee`() {
        val complet = CalorieCalculator.adaptiveRecalibration(
            weightChangeKg = -0.5, periodDays = 28,
            avgLoggedCalories = 2600.0, staticMaintenanceCalories = 2800,
            completudeSuivi = 0.9,
        )
        val partiel = CalorieCalculator.adaptiveRecalibration(
            weightChangeKg = -0.5, periodDays = 28,
            avgLoggedCalories = 2600.0, staticMaintenanceCalories = 2800,
            completudeSuivi = 0.4,
        )
        assertNotNull(complet)
        assertNull(partiel)
    }

    @Test
    fun `14 jours de fenetre avec moins de 67 pourcent de prises bloque le recalibrage`() {
        val repas = (1..14).map { j -> repas("2026-08-%02d".format(j), 1) }
        val confiance = LoggingConfidence.evaluer(repas, jours = 14)
        assertFalse(confiance.autoriseRecalibrage)
        assertNull(
            CalorieCalculator.adaptiveRecalibration(
                weightChangeKg = -0.3,
                periodDays = 14,
                avgLoggedCalories = 2500.0,
                staticMaintenanceCalories = 2700,
                completudeSuivi = confiance.completude,
            ),
        )
    }

    @Test
    fun `le message de completude ne culpabilise jamais`() {
        val message = LoggingConfidence.evaluer(emptyList(), jours = 14).message
        listOf("devrais", "oublié", "mauvais", "faute", "discipline").forEach {
            assertFalse("registre culpabilisant : $it", message.lowercase().contains(it))
        }
    }
}
