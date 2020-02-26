package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.simbrain.util.SimpleId
import org.simbrain.util.clip
import kotlin.random.Random

/**
 * The top level object for evolutionary simulations.
 *
 * An environment contains a list or "population" of agents (see [initialPopulation])
 *
 * Each [Agent2] has a [Genome2] and a [Agent2.fitness]. A genome is used to generate an Agent.
 *
 * The environment contains an [eval] function.  At each generation, each agent is evaluated using that [eval] function.
 * At each iteration of [runEvolution] the population is replenished using the most fit agents, by crossing over their
 * genomes.
 *
 * Examples
 *  - A Simbrain Network is the Agent, and the environment is just a training dataset.
 *  - A Simbrain Network and OdorWorldEntity make up the Agent, and an OdorWorld is the environment.
 *  - A list of integers is the agent, and all that's used in the environment is the eval function
 *
 * Notice that currently agents are always evaluated within a single environment.
 * Possible future use case: an environment can be shared across agents during evolution.
 *
 */
abstract class Environment<A : Agent2<G>, G : Genome2>(seed: Long, name: String, val eval: G.() -> Double) {

    protected abstract val initialPopulation: List<A>

    protected val geneticsRandom = Random(seed)

    val simpleId = SimpleId(name, 0)

    abstract val config: EnvironmentConfig

    protected fun random() = geneticsRandom

    protected fun IntRange.random() = this.random(geneticsRandom)

    protected fun Double.rand(range: Double) = geneticsRandom.nextDouble(-range, range) + this

    protected fun <T> T.applyRandomly(probability: Double, block: (T) -> Unit): T {
        return if (geneticsRandom.nextDouble() > probability) {
            apply(block)
        } else {
            this
        }
    }

    /**
     * See https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)#Single-point_crossover
     */
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
    fun runEvolution(optimizeFor: Optimize = Optimize.small) = generateSequence(initialPopulation) {
        it.eval(optimizeFor).eliminateLeastFit(0.5).replenish()
    }

    /**
     * Cross one genome over with another.
     *
     * https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm))
     */
    protected abstract infix fun G.crossOver(other: G): G

    /**
     * Mutate the genome.
     */
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
    protected fun List<A>.eliminateLeastFit(eliminateRate: Double) = this.take(((config.populationSize * (1 - (eliminateRate.clip(0.0, 1.0)))).toInt()))

    /**
     * Convert a genome into an agent.
     */
    protected abstract fun G.express(): A

    /**
     * Convert genomes into agents
     */
    protected fun Sequence<G>.init() = map { it.express() }.toList()

    /**
     * Evaluates an agent's fitness.
     */
    protected abstract fun A.eval(): A

    /**
     * Evaluates fitness of a list of agents.
     */
    protected fun List<A>.eval(optimizeFor: Optimize) = runBlocking {
        val result = map { async { it.eval() } }.toList().awaitAll()
        when (optimizeFor) {
            Optimize.small -> result.sorted()
            Optimize.big -> result.sortedDescending()
        }
    }
}

/**
 * Used for stopping condition.
 */
enum class Optimize {
    // Optimizing for a larger fitness score
    big,
    // Optimizing for a smaller fitness score
    small
}

/**
 * Util to evolve up to the indicated generation
 */
fun <A : Agent2<G>, G : Genome2> Sequence<List<A>>.upToGeneration(n: Int) = take(n)

/**
 * Util to get the best in a list.
 */
fun <A : Agent2<G>, G : Genome2> List<A>.best() = first()

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

/**
 * Configuration information for an environment.
 */
interface EnvironmentConfig {
    val populationSize: Int
}

/**
 * A genome together with a fitness value.  Implementing classes can contain additional structures like sensors and
 * effectors.
 */
abstract class Agent2<G : Genome2> : Comparable<Agent2<G>> {
    abstract val genome: G
    abstract val fitness: Double

    /**
     * To compare fitness scores
     */
    override fun compareTo(other: Agent2<G>) = fitness.compareTo(other.fitness)
}

/**
 * It is left up to the implementing class to use chromosomes and / or genes to encode a genome.
 */
abstract class Genome2 {
    abstract val id: String
}

/**
 * A list of genes.
 */
abstract class Chromosome2<G : Gene2> {
    abstract val genes: List<G>
}

/**
 * Implementing class encodes features of the phenotype in question. E.g. a neuron gene could contain information about a
 * neuron like it's bias value, threshold, update rule, etc.
 */
abstract class Gene2
