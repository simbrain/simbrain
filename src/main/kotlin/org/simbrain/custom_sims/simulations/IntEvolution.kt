package org.simbrain.custom_sims.simulations

import org.simbrain.util.geneticalgorithm.Optimize
import org.simbrain.util.geneticalgorithm.best
import org.simbrain.util.geneticalgorithm.IntegerEnvironment
import org.simbrain.util.geneticalgorithm.IntegerGeneticConfig
import org.simbrain.util.geneticalgorithm.untilFitnessScore
import org.simbrain.util.geneticalgorithm.upToGeneration
import kotlin.math.abs

/**
 * Try to get the ints to sum to exactly 125
 */
fun sumTo25() {
    val config = IntegerGeneticConfig(
            min = 0,
            max = 20,
            initialLength = 10,
            populationSize = 100
    )
    val env = IntegerEnvironment(config) {
        abs(chromosome.genes.map { it.value }.sum().toDouble() - 25)
    }
    var counter = 0
    val result = env.newEvolution(optimizeFor = Optimize.small)
            .upToGeneration(500)
            .untilFitnessScore { it < 1 }
            .onEach { println("|${++counter}|${it.best()}") }
            .last()
            .best()

    println("FINAL RESULT -> $result")
}

/**
 * Make a binary vector with 3 bits on.
 */
fun bitCount() {
    val config = IntegerGeneticConfig(
            min = 0,
            max = 1,
            initialLength = 5,
            populationSize = 100
    )

    val env = IntegerEnvironment(config) {
        // TODO: Make SSE
        abs(chromosome.genes.map { it.value }.sum().toDouble() - 3)
    }

    val result = env.newEvolution(optimizeFor = Optimize.small)
            .upToGeneration(500)
            .untilFitnessScore { it < 0.1 }
            .onEach { println(it.best()) }
            .last()
            .best()

    print(result)
}

fun main() {
    //sumTo25()
    bitCount();
}