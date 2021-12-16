package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.LinkedHashSet
import kotlin.random.Random
import kotlin.streams.toList

/**
 * Describes how to make a gene product, or "express a phenotype". Extend this class with your own gene type, and
 * provide the subclass with
 *  - A template object that can be used for copying and mutating the gene
 *  - A mutate function
 *  - A build function for expressing the gene. This will either be a [TopLevelBuilderContext] or a custom context
 *  that provides model objects used in expressing the gene.
 *
 *  For usage examples see [IntGene] and [NodeGene].
 *
 * @param P the phenotype of the gene product. For example, the phenotype of [NodeGene] is [Neuron]
 */
abstract class Gene<P, G: Gene<P, G>> {

    protected abstract val chromosome: Chromosome<P, G>

    /**
     * The phenotype expressed by the gene. Not expressed until onBuild is called.
     */
    abstract val product: CompletableFuture<P>

    /**
     * Helper to make it easy to complete the build.
     */
    protected inline fun completeWith(block: () -> P): P {
        return block().also { product.complete(it) }
    }

    abstract fun copy(chromosome: Chromosome<P, G>): G

    open fun delete() {
        chromosome.genes.remove(this)
    }

}

/**
 * Use this to designate that a [Gene] can be directly added in an onBuild function. If a gene must be added within
 * some other context, it is not top-level.
 *
 * Examples: [LayoutGene] (top level) vs [NodeGene] (not top-level).
 *
 * @param T the type of the phenotype expressed by the gene.
 */
interface TopLevelGene<T> {

    fun TopLevelBuilderContext.build(): T

}

/**
 * A list of genes that is memoized during evolution.
 */
class Chromosome<T, G : Gene<T, G>>(val genes: LinkedHashSet<G> = LinkedHashSet()) {
    @Suppress("UNCHECKED_CAST")
    fun copy(): Chromosome<T, G> {
        val chromosome = Chromosome<T, G>(LinkedHashSet())
        genes.forEach { chromosome.genes.add(it.copy(chromosome)) }
        return chromosome
    }

    fun forEach(block: (G) -> Unit) = genes.forEach(block)

    operator fun get(index: Int) = genes.asSequence().drop(index).first()

    operator fun plus(other: Chromosome<T, G>): Chromosome<T, G> {
        val thing = genes.toMutableList()
        thing.addAll(other.genes)
        return Chromosome(LinkedHashSet(thing))
    }

    val size get() = genes.size

    val products get() = genes.map { it.product.get() }

    fun add(block: Chromosome<T, G>.() -> G): G {
        val gene = block()
        genes.add(gene)
        return gene
    }

}

/**
 * Provides a context for [AgentBuilder.onEval]. "This" in onEval will refer to an instance of this class.
 */
class EvaluationContext(val evalRand: Random)

/**
 * Provides a context for [AgentBuilder.onMutate]. "This" in onMutate will refer to this object.
 */
object MutationContext

/**
 * The agent produced by [AgentBuilder.build]. Provides a context for interacting with an evolutionary
 * simulation after an agent has been built, so its products are available.
 *
 */
class Agent(
    private val evaluationContext: EvaluationContext,
    private val evalFunction: EvaluationContext.() -> Double,
    private val peekFunction: (EvaluationContext.() -> Unit)?
) {

    /**
     * A function that returns a double indicating fitness. Used by the [Evaluator] during evolution.
     */
    fun eval() = evaluationContext.evalFunction()

    /**
     * A function that can be called after an environment has been built. Useful for getting the "winning" genotype.
     */
    fun peek() = peekFunction?.let { evaluationContext.it() }

}

/**
 * Default context provided by the onBuild block for [TopLevelGene]s.  Allows code like
 * `+intChromosome`
 */
class TopLevelBuilderContext {

    /**
     * @param T the phenotype expressed, e.g. [Layout]
     * @param G the gene type, e.g. [LayoutGene]
     * @param C the inferred chromosome type, e.g. <Layout, LayoutGene>
     */
    operator fun <T, G, C> C.unaryPlus(): List<T>
            where
            G : Gene<T, G>,
            G : TopLevelGene<T>,
            C : Chromosome<T, G> = genes.map { with(it) { build() } }

}

/**
 * The main provider for the genetic algorithm DSL. The agent we are evolving, which will often be an agent in a
 * virtual environment.
 *
 * @param chromosomeList set of genes describing an agent, e.g. input, hidden, and output node genes
 * @param block the block opened up in the DSL, where you build the agent, by creating chromosomes and setting
 *                  up DSL functions like onMutate.
 * @param seed optional random seed
 * @param random private field used by copy function
 */
class AgentBuilder private constructor(
    private val chromosomeList: LinkedList<Chromosome<*, *>>,
    private val block: AgentBuilder.() -> Unit,
    val seed: Int = Random.nextInt(),
    val random: Random = Random(seed)
) {

    /**
     * Main public constructor.
     */
    constructor(builder: AgentBuilder.() -> Unit) : this(LinkedList(), builder)

    /**
     * Public constructor with a seed.
     */
    constructor(seed: Int, builder: AgentBuilder.() -> Unit) : this(LinkedList(), builder, seed)

    /**
     * List of mutation tasks executed at each generation.
     */
    private val mutationTasks = mutableListOf<MutationContext.() -> Unit>()

    /**
     * A fitness function.
     */
    private lateinit var evalFunction: EvaluationContext.() -> Double

    private var peekFunction: (EvaluationContext.() -> Unit)? = null

    private lateinit var builderBlock: TopLevelBuilderContext.(pretty: Boolean) -> Unit

    /**
     * Indicates we are on the first generation of the evolutionary algorithm.  Important to distinguish
     * this case so that future generations are "memoized".
     */
    private val isInitial = chromosomeList.isEmpty()

    private val chromosomeIterator = chromosomeList.iterator()

    fun copy(): AgentBuilder {
        val newSeed = random.nextInt()
        return AgentBuilder(LinkedList(chromosomeList.map { it.copy() }), block, newSeed).apply(block)
    }

    /**
     * Use this to describe what happens with each mutation. Can be called multiple times to add more mutation tasks.
     */
    fun onMutate(task: MutationContext.() -> Unit) {
        mutationTasks.add(task)
    }

    /**
     * Execute all the mutation tasks
     */
    fun mutate() {
        mutationTasks.forEach { MutationContext.it() }
    }

    /**
     * Use this to describe what happens when the builder expresses its products.
     *
     * You should only use once in a script. If a multiple onBuild blocks, only the last one will be called.
     *
     * The build operation is called once for each genome at each generation, via
     * [build]
     */
    fun onBuild(block: TopLevelBuilderContext.(visible: Boolean) -> Unit) {
        builderBlock = block
    }

    private fun createAgent(): Agent {
        val newSeed = random.nextInt()
        return Agent(EvaluationContext(Random(newSeed)), evalFunction, peekFunction)
    }

    /**
     * Build an agent by running the builderBlock defined in [onBuild].
     */
    fun build(): Agent {
        TopLevelBuilderContext().builderBlock(false)
        return createAgent()
    }

    /**
     * [build] for the case where the agent should be visible in the desktop/
     */
    fun visibleBuild(): Agent {
        TopLevelBuilderContext().builderBlock(true)
        return createAgent()
    }

    /**
     * Use this to define your evaluation / fitness function.
     */
    fun onEval(eval: EvaluationContext.() -> Double) {
        evalFunction = eval
    }

    fun onPeek(peek: EvaluationContext.() -> Unit) {
        peekFunction = peek
    }

    private inline fun <T, G : Gene<T, G>> createChromosome(crossinline initializeValue: () -> Chromosome<T, G>): Chromosome<T, G> {
        return if (isInitial) {
            initializeValue().also { chromosomeList.add(it) }
        } else {
            @Suppress("UNCHECKED_CAST")
            chromosomeIterator.next() as Chromosome<T, G>
        }
    }

    /**
     * Use this to create a chromosome with a set number of genes.
     */
    fun <T, G : Gene<T, G>> chromosome(initialCount: Int, @BuilderInference genes: Chromosome<T, G>.(index: Int) -> G): Chromosome<T, G> {
        return createChromosome {
            val chromosome = Chromosome<T, G>()
            repeat(initialCount) {
                chromosome.genes.add(chromosome.genes(it))
            }
            chromosome
        }
    }

    /**
     * Use this to create a chromosome from existing genes
     */
    fun <T, G : Gene<T, G>> chromosome(vararg genes: G): Chromosome<T, G> {
        return createChromosome { Chromosome(LinkedHashSet(listOf(*genes))) }
    }

    /**
     * Use this to create a chromosome from a collection of genes
     */
    fun <T, G : Gene<T, G>> chromosome(genes: Iterable<G>): Chromosome<T, G> {
        return createChromosome { Chromosome() }
    }

    /**
     * Use this when more complex logic is needed...
     */
    fun <T, G : Gene<T, G>> chromosome(@BuilderInference listBuilder: Chromosome<T, G>.() -> Unit): Chromosome<T, G> {
        return createChromosome { Chromosome(LinkedHashSet<G>()).apply(listBuilder) }
    }

}

/**
 * Holds an environment builder and fitness value. Used to hold results at each generation.
 */
data class BuilderFitnessPair(val agentBuilder: AgentBuilder, val fitness: Double)

/**
 * Entry point for building an evolutionary simulation
 */
fun evolutionarySimulation(builder: AgentBuilder.() -> Unit) = AgentBuilder(builder).apply(builder)

/**
 * Use this to create an environment builder with an initial seed.
 */
fun evolutionarySimulation(seed: Int, builder: AgentBuilder.() -> Unit) =
    AgentBuilder(seed, builder).apply(builder)

/**
 * Runs the evolutionary algorithm.
 */
class Evaluator(agentBuilder: AgentBuilder) {

    /**
     * Number of agents in a population. An agent exists in an environment, and so the objects created for each agent
     * will often be environments.
     */
    var populationSize: Int = 100

    /**
     * How many agents to eliminate each generation.
     */
    var eliminationRatio: Double = 0.5

    /**
     * What optimization method the evolutionary algorithm uses. Usuaally maximize fitness but
     * sometimes minimize errors.
     */
    var optimizationMethod: OptimizationMethod = OptimizationMethod.MAXIMIZE_FITNESS

    enum class OptimizationMethod {
        MAXIMIZE_FITNESS,
        MINIMIZE_FITNESS
    }

    /**
     * The initial, immutable list of agents.
     */
    private val initialPopulation = generateSequence(agentBuilder.copy()) { it.copy() }.take(populationSize).toList()

    /**
     * Condition in which to stop the evolution.
     */
    private lateinit var stoppingCondition: RunUntilContext.() -> Boolean

    /**
     * Set the stopping condition
     */
    fun runUntil(stoppingCondition: RunUntilContext.() -> Boolean) {
        this.stoppingCondition = stoppingCondition
    }

    /**
     * Allows you to set stopping condition in a convenient way.
     * Allows you to use a curly braced argument with several components.
     * E.g. runUntil { generation == 200 || fitness > 50 }
     * Note the instance is an implicit `this`, so you don't need to write
     * { data -> data.generation == 500 || data.fitness < 0.2 }
     */
    class RunUntilContext(val generation: Int, val fitness: Double)

    /**
     * Packages the result of a run of [Evaluator].
     */
    inner class Result {

        private var generation = 0

        /**
         * Begins yielding values when evolution.start() is called.
         */
        private var generations = sequence {
            var population = initialPopulation
            do {
                val builderFitnessPairs = population.parallelStream().map {
                    val build = it.build()
                    val score = build.eval()
                    BuilderFitnessPair(it, score)
                }.toList()
                    .sortedBy { if (optimizationMethod == OptimizationMethod.MAXIMIZE_FITNESS) -it.fitness else it.fitness }

                val currentFitness = builderFitnessPairs[0].fitness

                val survivors = builderFitnessPairs.take((eliminationRatio * builderFitnessPairs.size).toInt())

                // Concatenate (1) the most-fit survivors and (2) a random sample of mutated offspring of
                // those survivors to replenish the population
                population = survivors.map { it.agentBuilder } + (survivors.uniformSample()
                    .take(populationSize - survivors.size)
                    .map { it.agentBuilder.copy().apply { mutate() } }
                    .toList())

                yield(builderFitnessPairs)

                generation++

            } while (!stoppingCondition(RunUntilContext(generation, currentFitness)))
        }

        /**
         * Returns the wining agent builder and its fitness.
         */
        val best: BuilderFitnessPair
            get() = generations.last().first().let { (builder, fitness) ->
                BuilderFitnessPair(builder.copy(), fitness)
            }

        /**
         * Returns the generation number.
         */
        val finalGenerationNumber: Int get() = generation

        /**
         * Run the provided block at each generation. Context provides the whole population of builders.
         */
        fun onEachGeneration(block: (agents: List<BuilderFitnessPair>, generationNumber: Int) -> Unit): Result = this
            .apply {
                generations = generations.onEachIndexed { index, list -> block(list, index) }
            }

        /**
         * Like [onEachGeneration] but context only provides the (builder for) the fittest agent at each generation.
         */
        fun onEachGenerationBest(block: (agent: BuilderFitnessPair, generationNumber: Int) -> Unit): Result = this
            .apply {
                generations = generations.onEachIndexed { index, list -> block(list.first(), index) }
            }

    }

    /**
     * "Runs" the evolutionary algorithm.  Sequence is a lazy list. When you call .last()
     * the whole sequence is created, by sequentially creating successive generations, so that at any time only the
     * last generation and the current one need to be in memory.
     */
    fun start() = Result()
}

/**
 * Create the evaluator.
 */
fun evaluator(agentBuilder: AgentBuilder, block: Evaluator.() -> Unit) =
    Evaluator(agentBuilder).apply(block)

/**
 * Helper function to uniformly sample builder fitness papers. Used to choose survivors
 * for replenishing a population.
 */
fun List<BuilderFitnessPair>.uniformSample() = sequence {
    while (true) {
        // TODO: use evaluator random
        val index = (Math.random() * size).toInt()
        yield(this@uniformSample[index])
    }
}