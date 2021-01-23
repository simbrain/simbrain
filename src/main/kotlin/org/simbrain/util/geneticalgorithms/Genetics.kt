package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.concurrent.CompletableFuture
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
abstract class Gene<P> : CopyableObject {

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
class Chromosome<T, G : Gene<T>>(val genes: MutableList<G>): CopyableObject {
    @Suppress("UNCHECKED_CAST")
    override fun copy(): Chromosome<T, G> {
        return Chromosome(genes.map { it.copy() as G }.toMutableList())
    }

    inline fun forEach(block: (G) -> Unit) = genes.forEach(block)

    operator fun get(index: Int) = genes[index]

    val products get() = genes.map { it.product.get() }
}

/**
 * Provides a context for [EnvironmentBuilder.onEval]. "This" in onEval will refer to an instance of this class.
 */
class EvaluationContext(val evalRand: Random)

/**
 * Provides a context for [EnvironmentBuilder.onMutate]. "This" in onMutate will refer to this object.
 */
object MutationContext

/**
 * The environment produced by [EnvironmentBuilder.build]. Provides a context for interacting with an evolutionary
 * simulation after an environment has been built, so that products are available.
 *
 */
class Environment(
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
 * Default context provided by the onBuild block for [TopLevelGene]s.
 */
class TopLevelBuilderContext {

    /**
     * @param T the phenotype expressed, e.g. [Layout]
     * @param G the gene type, e.g. [LayoutGene]
     * @param C the inferred chromosome type, e.g. <Layout, LayoutGene>
     */
    operator fun <T, G, C> C.unaryPlus() : List<T>
            where
            G: Gene<T>,
            G: TopLevelGene<T>,
            C: Chromosome<T, G>
    = genes.map { with(it) { build() } }

}

/**
 * The main provider for the genetic algorithm DSL. An environment is basically the thing that we are evolving, which
 * will often be an agent in a virtual environment. A single agent or entity with everything it needs to be evaluated.
 *
 * @param chromosomeList set of genes describing an agent, e.g. input, hidden, and output node genes
 * @param template the block opened up in the DSL
 * @param seed optional random seed
 * @param random private field used by copy function
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
     * Public constructor with a seed.
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

    private var peekFunction: (EvaluationContext.() -> Unit)? = null

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
     * Use this to describe what happens when the builder expresses its products.
     *
     * You should only use once in a script. If a multiople onBuild blocks, only the last one will be called.
     *
     * The build operation is called once for each genome at each generation, via
     * [build]
     */
    fun onBuild(template: TopLevelBuilderContext.(pretty: Boolean) -> Unit) {
        builderTemplate = template
    }

    private fun buildWith(builder: TopLevelBuilderContext): Environment {
        val newSeed = random.nextInt()
        return Environment(EvaluationContext(Random(newSeed)), evalFunction, peekFunction)
    }

    /**
     * Called when building without graphics.
     */
    fun build() = buildWith(TopLevelBuilderContext().apply { builderTemplate(false) })

    /**
     * Called when building with graphics
     */
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
     * Packages the result of a run of [Evaluator].
     */
    inner class Result {
        private val generations = sequence {
            var generation = 0
            var next = population
            do {
                val current = next.parallelStream().map {
                    val build = it.build()
                    val score = build.eval()
                    BuilderFitnessPair(it, score)
                }.toList().sortedBy { if (optimizationMethod == OptimizationMethod.MAXIMIZE_FITNESS) -it.fitness else it.fitness }

                val currentFitness = current[0].fitness

                val survivors = current.take((eliminationRatio * current.size).toInt())

                yield(current)

                next = survivors.map { it.environmentBuilder } + survivors.uniformSample()
                    .take(populationSize - survivors.size)
                    .map { it.environmentBuilder.copy().apply { mutate() } }
                    .toList()

                generation++
            } while (!stoppingCondition(RunUntilContext(generation, currentFitness)))
        }

        /**
         * Returns the wining agent builder and its fitness.
         */
        val best: BuilderFitnessPair get() = generations.last().first().let { (builder, fitness) ->
            BuilderFitnessPair(builder.copy(), fitness)
        }

        /**
         * Returns the generation number.
         */
        val finalGenerationNumber: Int get() = generations.count()

        /**
         * Run the provided block at each generation. Context provides the whole population of builders.
         */
        fun onEachGeneration(block: List<BuilderFitnessPair>.(generationNumber: Int) -> Unit): Result = this.apply {
            generations.onEachIndexed { index, list -> list.block(index) }
        }


        /**
         * Like  [onEachGeneration] but context only provides the (builder for) the fittest agent at each generation.
         */
        fun onEachGenerationBest(block: BuilderFitnessPair.(generationNumber: Int) -> Unit): Result = this.apply {
            generations.map { it.first() }.onEachIndexed { index, pair -> pair.block(index) }
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
fun evaluator(environmentBuilder: EnvironmentBuilder, template: Evaluator.() -> Unit) =
        Evaluator(environmentBuilder).apply(template)

/**
 * Helper function to uniformly sample builder fitness papers. Used to choose survivors
 * for replenishing a population.
 */
fun List<BuilderFitnessPair>.uniformSample() = sequence {
    while(true) {
        // TODO: use evaluator random
        val index = (Math.random() * size).toInt()
        yield(this@uniformSample[index])
    }
}