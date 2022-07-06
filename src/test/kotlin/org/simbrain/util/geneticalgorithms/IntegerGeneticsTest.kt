package org.simbrain.util.geneticalgorithms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class IntegerGeneticsTest {

    val targetSum = 10

    @Test
    fun `use integer evolution to evolve a set of integer that sum to 10`() {

        val evolutionarySimulation = evolutionarySimulation {

            val intChromosome = chromosome(5) { intGene() }

            onMutate {
                intChromosome.forEach {
                    it.mutate {
                        value += random.nextInt(-5,5)
                    }
                }
            }

            onBuild {
                +intChromosome
            }

            onEval {
                val total = intChromosome.map { it.product.get() }.sumOf { it.toDouble() }
                abs(total - targetSum)
            }

            onPeek {
                // print("Integer genes:")
                // println(intChromosome.map { it.product.get() }.joinToString(", "))
                val total = intChromosome.map { it.product.get() }.sumOf { it.toDouble() }
                val error = abs(total - targetSum)
                assertEquals(0.0, error, .001)
            }
        }

        evaluator(evolutionarySimulation) {
            populationSize = 100
            eliminationRatio = 0.5
            optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
            runUntil { generation == 10000 || fitness < .2 }
        }.start().best.agentBuilder.build().peek()

    }

}