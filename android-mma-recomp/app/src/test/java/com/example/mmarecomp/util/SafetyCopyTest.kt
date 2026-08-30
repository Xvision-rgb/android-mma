package com.example.mmarecomp.util

import org.junit.Assert.assertFalse
import org.junit.Test

class SafetyCopyTest {

    private val interdits = listOf(
        "risque minimal",
        "jamais un repos complet",
        "ce n'est plus la poigne qui limite",
    )

    @Test
    fun `les textes publics n utilisent plus d affirmations absolues interdites`() {
        val sources = listOf(
            TrainingLoad::class.java,
            EnergyAvailability::class.java,
            CalorieCalculator::class.java,
            RelativeStrength::class.java,
            GripBenchmarks::class.java,
        ).flatMap { klass ->
            klass.declaredFields.mapNotNull { field ->
                field.isAccessible = true
                (field.get(null) as? String)
            } + klass.declaredMethods.flatMap { method ->
                method.isAccessible = true
                runCatching {
                    when (method.parameterCount) {
                        0 -> listOfNotNull(method.invoke(null) as? String)
                        1 -> listOfNotNull(method.invoke(null, sampleArg(method.parameterTypes[0])) as? String)
                        2 -> listOfNotNull(
                            method.invoke(null, sampleArg(method.parameterTypes[0]), sampleArg(method.parameterTypes[1])) as? String,
                        )
                        else -> emptyList()
                    }
                }.getOrDefault(emptyList())
            }
        }

        val textes = buildList {
            addAll(sources)
            add(GripBenchmarks.lecture(65))
            add(GripBenchmarks.lecture(25))
            add(EnergyAvailability.calculer(2800, 500, 66.0)!!.message)
            add(TrainingLoad.moduler(12, 1.8).explication)
        }.joinToString(" ").lowercase()

        interdits.forEach { phrase ->
            assertFalse("formulation interdite trouvée : $phrase", textes.contains(phrase))
        }
    }

    private fun sampleArg(type: Class<*>): Any? = when (type) {
        Int::class.java, java.lang.Integer.TYPE -> 30
        Double::class.java, java.lang.Double.TYPE -> 30.0
        else -> null
    }
}
