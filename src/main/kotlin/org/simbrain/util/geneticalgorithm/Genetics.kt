package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.simbrain.util.SimpleId
import org.simbrain.util.clip
import kotlin.random.Random

abstract class Environment<A: Agent2<G>, G : Genome2>(seed: Long, name: String, val eval: G.() -> Double) {

    protected abstract val initialPopulation: List<A>

    protected val geneticsRandom = Random(seed)

    val simpleId = SimpleId(name, 0)

    abstract val config: EnvironmentConfig

    protected fun random() = geneticsRandom

    protected fun IntRange.random() = this.random(geneticsRandom)

    protected infix fun <G : Gene2> List<G>.singlePointCrossOver(other: List<G>):
            List<G> {
        val maxSize = maxOf(this.size, other.size)
        val minSize = minOf(this.size, other.size)
        val index = minOf(geneticsRandom.nextInt(maxSize), minSize)
        val (firstGene, secondGenes) = if (geneticsRandom.nextBoolean()) Pair(this, other) else Pair(other, this)
        return firstGene.subList(0, index) + secondGenes.subList(index, secondGenes.size)
    }

    /**
     * Create a new evolution from the given configuration
     */
    fun newEvolution(optimizeFor: Optimize = Optimize.small) = generateSequence(initialPopulation) {
        it.eval(optimizeFor).eliminateLeastFit(0.5).replenish()
    }

    protected abstract infix fun G.crossOver(other: G): G

    protected abstract fun G.mutate(): G

    /**
     * Replenish the population to the configured size with the offspring from survivors.
     */
    protected fun List<A>.replenish(): List<A> {
        val survivors = this.toList()
        val offspring = generateSequence {
            val parent1 = survivors[geneticsRandom.nextInt(survivors.size)]
            val parent2 = survivors[geneticsRandom.nextInt(survivors.size)]
            (parent1.genome crossOver parent2.genome).mutate()
        }.take(config.populationSize - survivors.size).map { it.express() }.toList()
        return survivors + offspring
    }

    /**
     * Eliminate the worst performing agents
     *
     * @param eliminateRate the percentage of agents to eliminate. Ranges from 0 to 1.
     */
    protected fun List<A>.eliminateLeastFit(eliminateRate: Double)
            = this.take(((config.populationSize * (1 - (eliminateRate.clip(0.0, 1.0)))).toInt()))

    protected abstract fun G.express(): A

    /**
     * Convert genomes into agents
     */
    protected fun Sequence<G>.init() = map { it.express() }.toList()

    protected abstract fun A.eval() : A

    protected fun List<A>.eval(optimizeFor: Optimize) = runBlocking {
        val result = map { async { it.eval() } }.toList().awaitAll()
        when (optimizeFor) {
            Optimize.small -> result.sorted()
            Optimize.big -> result.sortedDescending()
        }
    }

}

enum class Optimize {
    big, small
}

interface EnvironmentConfig {
    val populationSize: Int
}

abstract class Agent2<G : Genome2> : Comparable<Agent2<G>> {
    abstract val genome: G
    abstract val fitness: Double

    override fun compareTo(other: Agent2<G>) = fitness.compareTo(other.fitness)
}

abstract class Genome2 {
    abstract val id: String
}

abstract class Chromosome2<G : Gene2> {
    abstract val genes: List<G>
}

abstract class Gene2

fun <A : Agent2<G>, G : Genome2> Sequence<List<A>>.upToGeneration(n: Int) = take(n)

/**
 * Take from sequence until the fitness it reaches the goal.
 * Reference: https://gist.github.com/matklad/54776705250e3b375618f59a8247a237
 *
 * @param predicate given a fitness score, returns whether it reaches the goal or not
 */
fun <A : Agent2<G>, G : Genome2> Sequence<List<A>>.untilFitnessScore(predicate: (Double) -> Boolean):
        Sequence<List<A>> {
    var takeMore = true
    return takeWhile {
        val result = takeMore
        takeMore = !predicate(it.first().fitness)
        result
    }
}

fun <A : Agent2<G>, G : Genome2> List<A>.best() = first()