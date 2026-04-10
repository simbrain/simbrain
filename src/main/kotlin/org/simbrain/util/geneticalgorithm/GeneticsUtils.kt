package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KMutableProperty0

/**
 * Return one random gene from this chromosome.
 *
 * Sampling from a chromosome just means selecting one of the genes in that group at random.
 * This is useful when a mutation or construction step should act on one existing gene chosen
 * from a set of related genes.
 */
context(Genotype)
fun <P, G : Gene<P>> Chromosome<P, G>.sample() = this[random.nextInt(size)]

/**
 * Return one random gene from the provided chromosomes, weighted by chromosome size.
 *
 * This treats the input as one combined pool of genes. A larger chromosome contributes more
 * possible choices because it contains more genes. Empty chromosomes are ignored. Throws if
 * all chromosomes are empty.
 */
context(Genotype)
fun <P, G : Gene<P>> sampleFrom(vararg chromosomes: Chromosome<P, G>): G {
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


/**
 * Build a chromosome by repeatedly applying [block] to an initially empty chromosome.
 *
 * The [repeat] argument controls how many times the block runs. For example,
 * `chromosome(2) { add(nodeGene()) }` runs the block twice and adds whatever the
 * block adds each time. It does not mean "make a chromosome with 2 genes" unless
 * the block itself adds exactly one gene per iteration.
 *
 */
context(Genotype)
fun <P, G : Gene<P>> chromosome(repeat: Int = 0, block: Chromosome<P, G>.(index: Int) -> Unit = { }) =
    Chromosome<P, G>(
        listOf()
    ).apply { repeat(repeat) { block(it) } }


/**
 * Express each network gene in this chromosome into the target network.
 */
context(Genotype)
suspend fun <P : NetworkModel, G : NetworkGene<P>> Network.express(chromosome: Chromosome<P, G>): List<P> {
    return chromosome.map { it.express(this@express) }
}

/**
 * Express each top-level gene in this chromosome.
 */
context(Genotype)
fun <P, G : TopLevelGene<P>> express(chromosome: Chromosome<P, G>): List<P> {
    return chromosome.map { it.express() }
}

data class SimMetadata(
    val id: Int,
    val parentId: Int?,
    val generation: Int,
    val fitness: Double
)

data class GenerationState(
    val generation: Int,
    val population: List<Pair<EvoSim, SimMetadata>>
) {

    val fitnessScores get() = population.map { it.second.fitness }

    val best get() = population.first().first

    val bestMetadata get() = population.first().second

    /**
     * Example: give it 5 and it returns the 5th percentile. 0 for the best.
     * Call with a list to get a distribution.
     */
    fun nthPercentileFitness(nth: Int) = nthPercentileFitness(nth.toDouble())

    fun nthPercentileFitness(nth: Double) = fitnessScores[(fitnessScores.lastIndex * nth / 100).roundToInt()]

}

/**
 * Given a list of <number,block> pairs, choose one of the blocks to execute. Treat the numbers as weights.
 *
 * Example: Random.runOne(1 to { print("A") }, 1 to { print("B") }) will print A or B with equal probability
 *
 */
fun Random.runOne(vararg functions: Pair<Number, () -> Unit>) {
    val total = functions.sumOf { it.first.toDouble() }
    val random = nextDouble() * total
    var sum = 0.0
    for ((weight, function) in functions) {
        sum += weight.toDouble()
        if (random < sum) {
            function()
            return
        }
    }
}

/**
 * Mutate a property by a random delta.  If min and max values are not specified, [UserParameter.minimumValue] and [UserParameter.maximumValue] are used, if available.
 */
fun <T: Number> Random.mutateProperty(property: KMutableProperty0<T>, delta: T, min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY) {
    val userParameter = property.annotations.filterIsInstance<UserParameter>().firstOrNull()
    val guiEditable = property.getDelegate().let { it as? GuiEditable<*, *> }

    val lowerBound = if (min.isFinite()) {
        min
    } else {
        userParameter?.minimumValue ?: (guiEditable?.min as? Number?)?.toDouble() ?: Double.NEGATIVE_INFINITY
    }

    val upperBound = if (max.isFinite()) {
        max
    } else {
        userParameter?.maximumValue ?: (guiEditable?.max as? Number?)?.toDouble() ?: Double.POSITIVE_INFINITY
    }

    fun mutateData() = when (property.get()) {
        is Int -> property.set((property.get().toInt() + nextInt(-delta.toInt(), delta.toInt())).coerceIn(lowerBound.toInt(), upperBound.toInt()) as T)
        is Long -> property.set((property.get().toLong() + nextLong(-delta.toLong(), delta.toLong())).coerceIn(lowerBound.toLong(), upperBound.toLong()) as T)
        is Double -> property.set((property.get().toDouble() + nextDouble(-delta.toDouble(), delta.toDouble())).coerceIn(lowerBound, upperBound) as T)
        else -> throw IllegalArgumentException("Unsupported type ${property.get()::class.simpleName}")
    }

    mutateData()
}

/**
 * Execute [block] with the given probability and return its result, otherwise return null.
 */
context(Genotype)
fun <T> withProbability(probability: Double, block: () -> T): T? {
    return if (random.nextDouble() < probability) {
        block()
    } else {
        null
    }
}


/**
 * Convert a number that is a probability into a ratio where the right side is 1.
 * Used in conjunction with [runOne] to produce weights from probs. This is a bit overkill
 * but we already wrote the docs and tests so...
 *
 * Examples:
 *  .5 goes with the ratio 1:1 and so this function maps .5 to 1.
 *  .75 goes with the ratio 3:1 and so this function maps .75 to 3.
 *  .25 goes with the ratio 1:3 or 1/3:1  so this function maps .25 to 1/3
 *  1.0 goes basically goes with infinity to 1 so 1.0 maps to POSITIVE_INFINITY
 *
 */
fun Double.toProbabilityWeight(): Double {
    require(this in 0.0..1.0) { "Input must be a valid probability between 0 and 1, inclusive." }

    if (this == 1.0) {
        return Double.POSITIVE_INFINITY  // or some large number to represent infinity
    }
    val complement = 1 - this
    val ratio = this / complement
    return ratio
}
