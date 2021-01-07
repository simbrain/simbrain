package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.random.Random
import kotlin.streams.toList

/**
 * Something with a template that can be used to produce multiple copies of itself. The smallest "atom" of a
 * evolutionary simulation. Describes how to make a product, or "express a phenotype". Evolutionary simulations
 * should extend this class.
 *
 * Support for mutation is not provided. Genes are mutated by directly changing the template in an
 * [EnvironmentBuilder.onMutate] function.
 *
 * By convention most Genes should provide a build function that returns a product.
 *
 * @param template A skeleton object holding the param of this gene. While we use builder patterns, we still need
 */
abstract class Gene<P> : CopyableObject {

    abstract val promise: CompletableFuture<P>

    protected inline fun completeWith(block: () -> P): P {
        return block().also { promise.complete(it) }
    }

}

interface TopLevelGene<T> {

    fun TopLevelBuilderContext.build(): T

}

/**
 * A list of genes. They are meant to be used inside of builders. Note that most of the machinery associated with
 * chromosomes is context specific and in extension functions.  For exmaple, some extensions to this function ensure
 * that copies of chromosomes are not changed between generations.
 */
class Chromosome<T, G : Gene<T>>(val genes: MutableList<G>): CopyableObject {
    @Suppress("UNCHECKED_CAST")
    override fun copy(): Chromosome<T, G> {
        return Chromosome(genes.map { it.copy() as G }.toMutableList())
    }

    inline fun forEach(block: (G) -> Unit) = genes.forEach(block)

    operator fun get(index: Int) = genes[index]

    val products get() = genes.map { it.promise.get() }
}

/**
 * Provides a context for [EnvironmentBuilder.onEval]. "This" in onEval will refer to an instance of this class.
 */
class EvaluationContext(val evalRand: Random)

/**
 * Provides a context for [EnvironmentBuilder.onMutate]. "This" in onMutate will refer to this object.
 */
object MutationContext {

}

/**
 * The environment produced by [EnvironmentBuilder.build]. Provides a context for interacting with an evolutionary
 * simulation after an environment has been built, so that products are availalble.
 *
 * @param evalFunction A function that returns a double indicating fitness. Used by the [Evaluator] during evolution.
 * @param peekFunction A function that can be called after an environment has been built.
 */
class Environment(
        val evaluationContext: EvaluationContext,
        private val evalFunction: EvaluationContext.() -> Double,
        private val peekFunction: EvaluationContext.() -> Unit
) {

    fun eval() = evaluationContext.evalFunction()

    fun peek() = evaluationContext.peekFunction()

}

class TopLevelBuilderContext {

    operator fun <T, G, C> C.unaryPlus() : List<T>
            where
            G: Gene<T>,
            G: TopLevelGene<T>,
            C: Chromosome<T, G>
    = genes.map { with(it) { build() } }

}

/**
 * The main provider for the genetic algorithm DSL. An environment is basically the thing that we are evolving, which
 * will often be an agent in a virtual enviroment.  A single agent or entity with everything it needs to be evaluated.
 *
 * @param chromosomeList set of genes describing an agent, e.g. input, hidden, and output node genes
 * @param template allow for the DSL to open a configuration block, as in " = environmentBuilder {..}
 */
class EnvironmentBuilder private constructor(
        private val chromosomeList: LinkedList<Chromosome<*, *>>,
        private val template: EnvironmentBuilder.() -> Unit,
        val seed: Int = Random.nextInt(),
        val random: Random = Random(seed)
) {

    /**
     * Main public constructor.
     */
    constructor(builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder)

    /**
     * Construct with a seed.
     */
    constructor(seed: Int, builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder, seed)

    /**
     * List of mutation tasks executed at each generation.
     */
    private val mutationTasks = mutableListOf<MutationContext.() -> Unit>()

    /**
     * A fitness function.
     */
    private lateinit var evalFunction: EvaluationContext.() -> Double

    private lateinit var peekFunction: EvaluationContext.() -> Unit

    private lateinit var builderTemplate: TopLevelBuilderContext.(pretty: Boolean) -> Unit

    /**
     * Indicates we are on the first generation of the evolutionary algorithm.  Important to distinguish
     * this case so that future generations are "memoized".
     */
    private val isInitial = chromosomeList.isEmpty()

    private val chromosomeIterator = chromosomeList.iterator()

    fun copy(): EnvironmentBuilder {
        val newSeed = random.nextInt()
        return EnvironmentBuilder(LinkedList(chromosomeList.map { it.copy() }), template, newSeed).apply(template)
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
     * Use this to describe what happens when the builder builds products.
     *
     * Only use once in a script.
     *
     * The build operation is called once for each genome at each generation, via
     * [build]
     */
    fun onBuild(template: TopLevelBuilderContext.(pretty: Boolean) -> Unit) {
        builderTemplate = template
    }

    /**
     * Builds the products.
     */
    private fun buildWith(builder: TopLevelBuilderContext): Environment {
        val newSeed = random.nextInt()
        return Environment(EvaluationContext(Random(newSeed)), evalFunction, peekFunction)
    }

    fun build() = buildWith(TopLevelBuilderContext().apply { builderTemplate(false) })

    fun prettyBuild() = buildWith(TopLevelBuilderContext().apply { builderTemplate(true) })

    /**
     * Use this to define your evaluation / fitness function.
     */
    fun onEval(eval: EvaluationContext.() -> Double) {
        evalFunction = eval
    }

    fun onPeek(peek: EvaluationContext.() -> Unit) {
        peekFunction = peek
    }

    private inline fun <T, G: Gene<T>> createChromosome(initializeValue: () -> Chromosome<T, G>): Chromosome<T, G> {
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
    fun <T, G : Gene<T>> chromosome(initialCount: Int, genes: (index: Int) -> G): Chromosome<T, G> {
        return createChromosome { Chromosome(List(initialCount) { genes(it) }.toMutableList()) }
    }

    /**
     * Use this to create a chromosome from existing genes
     */
    fun <T, G : Gene<T>> chromosome(vararg genes: G): Chromosome<T, G> {
        return createChromosome { Chromosome(mutableListOf(*genes)) }
    }

    /**
     * Use this to create a chromosome from a collection of genes
     */
    fun <T, G : Gene<T>> chromosome(genes: Iterable<G>): Chromosome<T, G> {
        return createChromosome { Chromosome(genes.toMutableList()) }
    }

    /**
     * Use this when more complex logic is needed...
     */
    fun <T, G : Gene<T>> chromosome(listBuilder: MutableList<G>.() -> Unit): Chromosome<T, G> {
        return createChromosome { Chromosome(mutableListOf<G>().apply(listBuilder)) }
    }

}

/**
 * Holds an environment builder and fitness value. Used to hold results at each generation.
 */
data class BuilderFitnessPair(val environmentBuilder: EnvironmentBuilder, val fitness: Double)

/**
 * Use this to create an environment builder.
 */
fun environmentBuilder(builder: EnvironmentBuilder.() -> Unit) = EnvironmentBuilder(builder).apply(builder)

/**
 * Use tihs to create an environment builder with an initial seed.
 */
fun environmentBuilder(seed: Int, builder: EnvironmentBuilder.() -> Unit)
        = EnvironmentBuilder(seed, builder).apply(builder)

/**
 * Runs the evolutionary algorithm.
 */
class Evaluator(environmentBuilder: EnvironmentBuilder) {

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
     * A lazy list of agents, or more specifically environments containing agents.
     */
    private val population = generateSequence(environmentBuilder.copy()) { it.copy() }.take(populationSize).toList()

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
     * "Runs" the evolutionary algorithm.  Sequence is a lazy list. When you call .last
     * the whole sequence is created, by sequentially creating successive generations, so that at any time only the
     * last generation and the current one need to be in memory.
     */
    fun start(): Sequence<List<BuilderFitnessPair>> {
        return sequence {
            var generation = 0
            var next = population
            do {
                val current = next.parallelStream().map {
                    val build = it.build()
                    val score = build.eval()
                    BuilderFitnessPair(it, score)
                }.toList().sortedBy { if (optimizationMethod == OptimizationMethod.MAXIMIZE_FITNESS) -it.fitness else it.fitness }

                val currentFitness = if (optimizationMethod == OptimizationMethod.MAXIMIZE_FITNESS) {
                    current.maxOf { it.fitness }
                } else {
                    current.minOf { it.fitness }
                }

                val survivors = current.take((eliminationRatio * current.size).toInt())

                yield(current)

                next = survivors.map { it.environmentBuilder } + survivors.uniformSample()
                        .take(populationSize - survivors.size)
                        .map { it.environmentBuilder.copy().apply { mutate() } }
                        .toList()

                generation++
            } while (!stoppingCondition(RunUntilContext(generation, currentFitness)))
        }
    }
}

/**
 * Create the evaluator.
 */
fun evaluator(environmentBuilder: EnvironmentBuilder, template: Evaluator.() -> Unit) =
        Evaluator(environmentBuilder).apply(template)

/**
 * Helper function to uniformly sample builder fitness papers.  Used to choose survivors
 * for replenishing a population.
 */
fun List<BuilderFitnessPair>.uniformSample() = sequence {
    while(true) {
        val index = (Math.random() * size).toInt()
        yield(this@uniformSample[index])
    }
}