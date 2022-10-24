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

class Chromosome2<P, G : Gene2<P>>(genes: List<G>) : MutableList<G> by ArrayList(genes) {
    fun copy() = Chromosome2(map { it.copy() as G })

    operator fun plus(other: Chromosome2<P, G>) = Chromosome2(buildList { addAll(this@Chromosome2); addAll(other); })
}

suspend fun evaluator2(
    populatingFunction: (index: Int) -> EvoSim,
    populationSize: Int,
    eliminationRatio: Double,
    peek: GenerationFitnessPair.() -> Unit,
    stoppingFunction: GenerationFitnessPair.() -> Boolean,
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