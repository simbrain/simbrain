package org.simbrain.util.geneticalgorithm

/**
 * Core abstractions for Simbrain's evolutionary framework.
 *
 * This file defines the generic building blocks used across the genetics package:
 * [Genotype], [Gene], [TopLevelGene], [Chromosome], [SlotEvoSim], [EvaluatorParams],
 * and the [evaluator] loop that runs selection and reproduction across generations.
 *
 * The basic mental model is:
 * - A [Gene] stores a mutable template for some object.
 * - A [Chromosome] is a typed list of related genes, such as all input nodes or all connections.
 * - A [Genotype] groups chromosomes together and provides a source of randomness.
 * - `express(...)` functions turn genes or chromosomes into runtime objects.
 * - An [SlotEvoSim] wraps build, mutate, copy, visualization, and evaluation for one candidate.
 * - [evaluator] repeatedly evaluates a population, keeps the survivors, clones them, and mutates the new copies.
 *
 * Chromosomes are mainly an organizational abstraction. They let a genotype keep related genes together as a unit,
 * preserve type information, and support operations over whole groups of genes such as copying, concatenation,
 * expression, and random selection. A chromosome can be empty, and its size does not need to stay fixed across
 * generations. Mutation logic can add or remove genes over time.
 *
 * Typical usage is:
 * 1. Define a genotype class holding one or more chromosomes.
 * 2. Provide mutation, copy, and expression logic for that genotype.
 * 3. Wrap the genotype in an [SlotEvoSim].
 * 4. Configure [EvaluatorParams] and run [evaluator].
 *
 * See also:
 * - [chromosome], sampling helpers, and `express(...)` helpers in [GeneticsUtils.kt][org.simbrain.util.geneticalgorithm.chromosome]
 * - network-specific genes such as `nodeGene` and `connectionGene` in `NetworkGenetics.kt`
 * - [EvolveXor.kt][/Users/jyoshimi/gitstuff/simbrainmain/simbrain/src/main/kotlin/org/simbrain/custom_sims/simulations/evolution/EvolveXor.kt]
 *   for a compact end-to-end example
 */
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.sampleWithReplacement
import org.simbrain.workspace.Workspace
import kotlin.math.roundToInt
import kotlin.random.Random

interface Genotype {
    val random: Random
}

/**
 * A gene stores a mutable template for one evolvable object.
 *
 * Subclasses provide domain-specific copy and expression behavior.
 */
abstract class Gene<P> {
    abstract val template: P
    abstract fun copy(): Gene<P>

    fun mutate(block: P.() -> Unit) {
        template.apply(block)
    }
}

/**
 * A gene whose expression step does not need an external target object.
 */
abstract class TopLevelGene<P>: Gene<P>() {
    abstract fun express(): P
}

object TopLevelGeneticsContext

/**
 * Base class for evolution simulations backed by a [SlotGenotype].
 *
 * Subclasses implement [onBuild] (called at most once), [create] (factory for copies),
 * and [eval] (fitness evaluation).
 * Boilerplate for idempotent build, copy/visualize, and mutation delegation is handled here.
 */
abstract class SlotEvoSim<G : SlotGenotype>(
    val genotype: G,
    val workspace: Workspace = Workspace()
) {

    private var built = false

    suspend fun build() {
        if (!built) {
            onBuild()
            built = true
        }
    }

    /**
     * Express the genotype into the workspace. Called exactly once.
     */
    protected abstract suspend fun onBuild()

    /**
     * Factory method: create a new sim instance with the given genotype and workspace.
     */
    protected abstract fun create(genotype: G, workspace: Workspace): SlotEvoSim<G>

    /**
     * Evaluate this candidate and return its fitness or error metric.
     */
    abstract suspend fun eval(): Double

    fun mutate() { genotype.mutate() }

    @Suppress("UNCHECKED_CAST")
    fun copy(): SlotEvoSim<*> = create(genotype.copyGenotype() as G, Workspace())

    @Suppress("UNCHECKED_CAST")
    fun visualize(workspace: Workspace): SlotEvoSim<*> = create(genotype.copyGenotype() as G, workspace)
}

/**
 * A typed list of related genes with convenience functions for copying and concatenation.
 *
 * In practice, chromosomes are used to keep one part of a genotype together, for example a set of node genes,
 * connection genes, or rule genes. Chromosomes may be empty, and they may grow or shrink during evolution if the
 * genotype's mutation logic adds or removes genes.
 */
class Chromosome<P, G : Gene<P>>(genes: List<G>) : MutableList<G> by ArrayList(genes) {

    /**
     * Provides a copy of the chromosome.
     */
    fun copy() = Chromosome(map { it.copy() as G })

    /**
     * Provides the ability to concatenate chromsomes. See usages.
     */
    operator fun plus(other: Chromosome<P, G>) = Chromosome(buildList { addAll(this@Chromosome); addAll(other); })
}


data class PopulatingFunctionParams(val seed: Long)

/**
 * Runs an evolutionary loop, publishing [GenerationState] to a [StateFlow] each generation.
 *
 * Subscribers collect from [state] to observe progress (gene display, progress bars, logging).
 * Call [run] to start evolution; it suspends until the stopping condition is met and returns
 * the final [GenerationState].
 */
class EvolutionRunner(
    val populatingFunction: PopulatingFunctionParams.() -> SlotEvoSim<*>,
    val populationSize: Int,
    val eliminationRatio: Double,
    val stoppingFunction: GenerationState.() -> Boolean,
    val sortDescending: Boolean = true,
    val seed: Long = Random.nextLong(),
) {
    constructor(
        evaluatorParams: EvaluatorParams,
        populatingFunction: PopulatingFunctionParams.() -> SlotEvoSim<*>
    ) : this(
        populatingFunction = populatingFunction,
        populationSize = evaluatorParams.populationSize,
        eliminationRatio = evaluatorParams.eliminationRatio,
        stoppingFunction = {
            evaluatorParams.stoppingCondition.shouldStop(
                nthPercentileFitness(evaluatorParams.evalutationPercentile),
                evaluatorParams.targetMetric
            ) || generation > evaluatorParams.maxGenerations
        },
        sortDescending = evaluatorParams.stoppingCondition == EvaluatorParams.StoppingCondition.Fitness,
        seed = evaluatorParams.seed.toLong()
    )
    private val _state = MutableStateFlow<GenerationState?>(null)
    val state: StateFlow<GenerationState?> = _state.asStateFlow()

    private val subscribers = mutableListOf<suspend (GenerationState) -> Unit>()
    private val completionCallbacks = mutableListOf<() -> Unit>()

    /**
     * Register a callback that runs for each generation state.
     * Subscribers are launched as coroutines when [run] starts and cancelled when it completes.
     */
    fun onGeneration(block: suspend (GenerationState) -> Unit) {
        subscribers.add(block)
    }

    /**
     * Register a callback that runs once when the evolution run completes.
     */
    fun onComplete(block: () -> Unit) {
        completionCallbacks.add(block)
    }

    suspend fun run(): GenerationState = coroutineScope {
        val subscriberJob = Job()
        for (sub in subscribers) {
            launch(subscriberJob) {
                state.filterNotNull().collect { sub(it) }
            }
        }
        val random = Random(seed)
        var generation = 0
        var nextId = 0
        val populatingFunctionParams = PopulatingFunctionParams(seed)
        var population = List(populationSize) { populatingFunction(populatingFunctionParams) }
        var metadata = population.map { SimMetadata(id = nextId++, parentId = null, generation = 0, fitness = 0.0) }
        lateinit var generationState: GenerationState
        do {
            generation++
            val fitnessScores = population.map { async { it.eval() } }.awaitAll()
            val scored = (population zip metadata zip fitnessScores).map { (simMeta, fitness) ->
                simMeta.first to simMeta.second.copy(fitness = fitness)
            }
            val sorted = scored.shuffled(random).let {
                if (sortDescending) it.sortedByDescending { it.second.fitness }
                else it.sortedBy { it.second.fitness }
            }
            generationState = GenerationState(generation, sorted)
            _state.value = generationState

            val eliminationCount = (sorted.size * eliminationRatio).roundToInt()
            val survivors = sorted.take(populationSize - eliminationCount)
            val offspring = survivors.sampleWithReplacement(random).take(eliminationCount).toList().map { (sim, meta) ->
                val childId = nextId++
                sim.copy().apply { mutate() } to SimMetadata(id = childId, parentId = meta.id, generation = generation, fitness = 0.0)
            }
            val nextGen = survivors.map { (sim, meta) ->
                sim.copy() to meta
            } + offspring
            population = nextGen.map { it.first }
            metadata = nextGen.map { it.second }
        } while (!stoppingFunction(generationState))
        subscriberJob.cancel()
        completionCallbacks.forEach { it() }
        generationState
    }
}


/**
 * Parameters for configuring a standard evolutionary run and its UI helpers.
 */
class EvaluatorParams(
    populationSize: Int = 100,
    eliminationRatio: Double = 0.5,
    iterationsPerRun: Int = 100,
    maxGenerations: Int = 500,
    evaluationPercentile: Int = 5,
    var stoppingCondition: StoppingCondition = StoppingCondition.Fitness,
    targetMetric: Double,
    seed: Int = Random.nextInt()
): EditableObject {

    var populationSize by GuiEditable(
        initValue = populationSize,
        description = "Number of simulations spawned per generation",
        min = 0,
        order = 0
    )

    var eliminationRatio by GuiEditable(
        initValue = eliminationRatio,
        description = "Percentage of the population eliminated each generation",
        min = 0.0,
        max = 1.0,
        order = 10
    )

    var iterationsPerRun by GuiEditable(
        initValue = iterationsPerRun,
        description = "Each generation, the simulation is iterated this many times",
        min = 0,
        order = 20
    )

    var maxGenerations by GuiEditable(
        initValue = maxGenerations,
        description = "After this many generations stop, regardless of ${stoppingCondition.name.lowercase()}",
        min = 0,
        order = 30
    )

    var targetMetric by GuiEditable(
        label = "Target ${stoppingCondition.name.lowercase()}",
        description = if (stoppingCondition == StoppingCondition.Error) {
            "Once the error is below this amount, the simulation is stopped"
        } else {
            "Once the fitness is above this amount, the simulation is stopped"
        },
        initValue = targetMetric,
        min = 0.0,
        order = 50
    )

    var evalutationPercentile by GuiEditable(
        initValue = evaluationPercentile,
        label = "Evaluation percentile",
        description = "When deciding whether to stop the simulation, consider current ${stoppingCondition.name.lowercase()} in this percentile of the population",
        min = 0,
        max = 100,
        order = 60
    )

    var seed by GuiEditable(
        initValue = seed,
        description = "Random seed that can be used for replicability",
        order = 70
    )

    /**
     * Use error when the evolutionary algorithm is trying to minimize a value, and fitness when it is trying to maximize a value.
     */
    sealed class StoppingCondition {
        abstract fun shouldStop(actual: Double, target: Double): Boolean

        abstract val name: String

        data object Error : StoppingCondition() {
            override fun shouldStop(actual: Double, target: Double) = actual < target
            override val name = "Error"
        }

        data object Fitness : StoppingCondition() {
            override fun shouldStop(actual: Double, target: Double) = actual > target
            override val name = "Fitness"
        }
    }
}
