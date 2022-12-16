package org.simbrain.util.geneticalgorithm2

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.simbrain.util.sampleWithReplacement
import org.simbrain.workspace.Workspace
import kotlin.math.roundToInt
import kotlin.random.Random

interface Genotype2 {
    val random: Random
}

abstract class Gene2<P> {
    abstract val template: P
    abstract fun copy(): Gene2<P>

    fun mutate(block: P.() -> Unit) {
        template.apply(block)
    }
}

interface EvoSim {
    fun mutate()
    suspend fun build()
    fun visualize(workspace: Workspace): EvoSim
    fun copy(): EvoSim
    suspend fun eval(): Double
}

/**
 * A typed list of Genes, with functions to copy and concatenate.
 */
class Chromosome2<P, G : Gene2<P>>(genes: List<G>) : MutableList<G> by ArrayList(genes) {

    /**
     * Provides a copy of the chromosome.
     */
    fun copy() = Chromosome2(map { it.copy() as G })

    /**
     * Provides the ability to concatenate chromsomes. See usages.
     */
    operator fun plus(other: Chromosome2<P, G>) = Chromosome2(buildList { addAll(this@Chromosome2); addAll(other); })
}

/**
 * The main evolutionary code.
 * Assumes fitness, i.e. bigger numbers are better. For "error", the eval function should return a negative number.
 * Returns all simulations from the last generation of the run.
 *
 * @param populatingFunction initial evolutionary sim
 * @param populationSize stays constant during the run
 * @param eliminationRatio how many sims to eliminate each generation.
 * @param stoppingFunction a function that determines when to stop running the sim. Generally check a generation
 * number and for fitness.
 * @param peek code to run each iteration, for example to update a progress bar
 */
suspend fun evaluator2(

    populatingFunction: (index: Int) -> EvoSim,
    populationSize: Int,
    eliminationRatio: Double,
    stoppingFunction: GenerationFitnessPair.() -> Boolean,
    peek: GenerationFitnessPair.() -> Unit = {},
    seed: Long = Random.nextLong(),
    random: Random = Random(seed)
): List<EvoSim> = coroutineScope {
    var generation = 0
    var population = List(populationSize) { populatingFunction(generation) }
    do {
        generation++
        val fitnessScores = population.map { async { it.eval() } }.awaitAll()
        val agentFitnessPair = (population zip fitnessScores).shuffled().sortedByDescending { it.second }
        val eliminationCount = (agentFitnessPair.size * eliminationRatio).roundToInt()
        val survivors = agentFitnessPair.take(populationSize - eliminationCount).map { (sim) -> sim }
        population = (survivors.map { async { it.copy() } } + survivors.sampleWithReplacement(random).take(eliminationCount)
            .toList().map {
                async {
                    it.copy().apply {
                        mutate()
                    }
                }
            }).awaitAll()
        val generationFitnessPair = GenerationFitnessPair(generation, agentFitnessPair.map { it.second })
        peek(generationFitnessPair)
    } while (!stoppingFunction(generationFitnessPair))
    population
}