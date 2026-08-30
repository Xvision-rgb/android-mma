package com.example.mmarecomp.util

import com.example.mmarecomp.model.MuscleZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le Bloc Densité est livré en asset texte et passe par le parser générique.
 *  Ces tests vérifient que le texte livré produit bien le programme voulu —
 *  une faute de frappe dans l'asset donnerait sinon un plan silencieusement
 *  amputé, le parser ignorant les lignes qu'il ne reconnaît pas. */
class BlocDensiteTest {

    // Copie du contenu de app/src/main/assets/bloc_densite.txt : un test JVM
    // pur n'a pas accès aux assets Android.
    private val programme = """
        Lundi
        - Soulevé de terre roumain 3x8 @100kg
        - Rowing Pendlay 3x6 @70kg
        - Hip thrust 2x10 @80kg
        - Farmer's walk 2x40

        Mardi
        - Tirage vertical prise neutre 3x10 @60kg
        - Développé militaire 3x8 @45kg
        - Rowing haltère unilatéral 2x12 @30kg
        - Isométrie cou 2x30

        Mercredi
        - Squat Zercher 3x6 @70kg
        - Fente marchée 2x10 @20kg
        - Leg curl 2x12 @40kg
        - Suspension lestée 2x30

        Jeudi
        - Développé couché 3x6 @80kg
        - Dips 2x10
        - Face pull 2x15 @25kg
        - Isométrie cou 2x30
        - Course à pied 1x30

        Vendredi
        - Rowing barre 3x10 @70kg
        - Soulevé de terre roumain léger 2x10 @70kg
        - Tirage horizontal prise épaisse 2x12 @50kg

        Samedi
        - Curl marteau prise épaisse 2x12 @14kg
        - Extension triceps 2x12 @25kg
        - Dead hang 2x30
        - Isométrie cou 2x30
        - Course à pied 1x25
    """.trimIndent()

    private val jours = TrainingPlanParser.parse(programme).days

    @Test
    fun `les six seances sont reconnues et le dimanche reste libre`() {
        assertEquals(6, jours.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), jours.map { it.jourSemaine }.sorted())
    }

    @Test
    fun `aucune ligne d exercice n est perdue par le parser`() {
        // 4 + 4 + 4 + 5 + 3 + 5 lignes, dont deux sorties de course.
        assertEquals(25, jours.sumOf { it.exercices.size })
    }

    @Test
    fun `le volume de tirage de la semaine 1 est celui du bloc`() {
        val series = jours
            .flatMap { it.exercices }
            .filter { MuscleZoneClassifier.classifier(it.nom) == MuscleZone.TIRAGE }
            .sumOf { it.series }
        // Pendlay 3 + tirage vertical 3 + rowing haltère 2 + face pull 2
        // + rowing barre 3 + tirage horizontal 2 = 15.
        assertEquals(15, series)
    }

    @Test
    fun `le ratio tirage poussee part deja au dessus de 1`() {
        fun seriesDe(zone: MuscleZone) = jours
            .flatMap { it.exercices }
            .filter { MuscleZoneClassifier.classifier(it.nom) == zone }
            .sumOf { it.series }

        val tirage = seriesDe(MuscleZone.TIRAGE)
        val poussee = seriesDe(MuscleZone.POUSSEE)
        assertTrue("tirage $tirage doit dépasser poussée $poussee", tirage > poussee)
    }

    @Test
    fun `le travail de cou et de poigne est reellement present`() {
        val series = jours
            .flatMap { it.exercices }
            .filter { MuscleZoneClassifier.classifier(it.nom) == MuscleZone.COU_POIGNE }
            .sumOf { it.series }
        assertTrue("cou/poigne trop faible : $series séries", series >= 10)
    }

    @Test
    fun `les deux sorties de course sont bien programmees`() {
        val sorties = jours.flatMap { it.exercices }.count { it.nom.contains("Course", ignoreCase = true) }
        assertEquals(EnduranceInterference.DOSE_ENTRETIEN_PAR_SEMAINE, sorties)
    }

    @Test
    fun `les sorties tombent les jours de haut du corps`() {
        val joursAvecCourse = jours
            .filter { j -> j.exercices.any { it.nom.contains("Course", ignoreCase = true) } }
            .map { it.jourSemaine }
            .sorted()
        // Jeudi et samedi. Le bas du corps lourd du bloc est lundi (chaîne
        // postérieure) et mercredi (Zercher) : aucune sortie ne partage un
        // jour avec eux, et samedi est à trois jours du plus proche.
        assertEquals(listOf(4, 6), joursAvecCourse)
        val joursBasDuCorps = setOf(1, 3)
        assertTrue(joursAvecCourse.none { it in joursBasDuCorps })
    }
}
