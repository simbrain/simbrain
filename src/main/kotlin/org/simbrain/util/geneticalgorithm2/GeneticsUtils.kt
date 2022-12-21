package org.simbrain.util.geneticalgorithm2

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.util.sampleWithReplacement
import org.simbrain.util.sampleWithoutReplacement
import kotlin.math.roundToInt

context(Genotype2)
fun <P, G : Gene2<P>> Chromosome2<P, G>.sample() = this[random.nextInt(size)]

context(Genotype2)
fun <P, G : Gene2<P>> sampleFrom(vararg chromosomes: Chromosome2<P, G>): G {
    val nonEmptyChromosomes = chromosomes.filter { it.isNotEmpty() }
    if (nonEmptyChromosomes.isEmpty()) {
        throw NoSuchElementException()
    }
    val cumulativeIndices = nonEmptyChromosomes.map { it.size }.runningReduce { acc, size -> acc + size }
    val indexChromosomeMap = cumulativeIndices zip nonEmptyChromosomes
    val index = random.nextInt(cumulativeIndices.last())
    return indexChromosomeMap.first { (ci) -> index < ci }.let { (ci, chromosome) ->
        val chromosomeIndex = index - (ci - chromosome.size)
        chromosome[chromosomeIndex]
    }
}

context(Genotype2)
fun <P, G : Gene2<P>> Chromosome2<P, G>.sampleWithReplacement() = sampleWithReplacement(random)

context(Genotype2)
fun <P, G : Gene2<P>> Chromosome2<P, G>.sampleWithoutReplacement(restartIfExhausted: Boolean = true) =
    sampleWithoutReplacement(random, restartIfExhausted)

context(Genotype2)
fun <P : NetworkModel, G : NetworkGene2<P>> chromosome2(repeat: Int = 1, block: Chromosome2<P, G>.() -> Unit) =
    Chromosome2<P, G>(
        listOf()
    ).apply { repeat(repeat) { block() } }

/**
 * Utility to create network models in a network, given their description as NetworkGenes.
 */
context(Genotype2)
suspend fun <P : NetworkModel, G : NetworkGene2<P>>
        Network.express(Chromosome2: Chromosome2<P, G>): List<P> {
    return Chromosome2.map { it.express(this@express) }
}

data class GenerationFitnessPair(val generation: Int, val fitnessScores: List<Double>) {

    /**
     * Example: give it 5 and it returns the 5th percentile. 0 for the best.
     * Call with a list to get a distribution.
     */
    fun nthPercentileFitness(nth: Int) = nthPercentileFitness(nth.toDouble())

    fun nthPercentileFitness(nth: Double) = fitnessScores[(fitnessScores.lastIndex * nth / 100).roundToInt()]

}
