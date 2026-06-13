package org.simbrain.util.geneticalgorithm

/**
 * Core abstractions for Simbrain's evolutionary framework.
 *
 * This file defines the generic building blocks used across the genetics package:
 * [Genotype], [Gene], [Chromosome], [EvoSim], [EvaluatorParams],
 * and the [evaluator] loop that runs selection and reproduction across generations.
 *
 * The basic mental model is:
 * - A [Gene] stores a mutable template for some object.
 * - A [Chromosome] is a typed list of related genes, such as all input nodes or all connections.
 * - A [Genotype] groups chromosomes together and provides a source of randomness.
 * - `express(...)` functions turn genes or chromosomes into runtime objects.
 * - An [EvoSim] wraps build, mutate, copy, visualization, and evaluation for one candidate.
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
 * 3. Wrap the genotype in an [EvoSim].
 * 4. Configure [EvaluatorParams] and run [evaluator].
 *
 * See also:
 * - [chromosome], sampling helpers, and `express(...)` helpers in [GeneticsUtils.kt][org.simbrain.util.geneticalgorithm.chromosome]
 * - network-specific genes such as `nodeGene` and `connectionGene` in `NetworkGenetics.kt`
 * - `EvolveXor.kt` for a compact end-to-end example
 */
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.util.FlowEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.sampleWithReplacement
import org.simbrain.workspace.Workspace
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A gene stores the information needed to build one evolvable object.
 *
 * [P] is the phenotype type: the kind of object this gene ultimately produces when it is expressed.
 * In practice, [template] is usually a mutable "prototype" or template phenotype that gets copied and then placed
 * into some larger structure. This is similar to gene expression in biology: the gene does not directly *become* the
 * final object in the simulation, it helps produce one.
 *
 * [C] is the context needed for expression. Expression often means writing the phenotype into that context:
 * - [Unit] for self-contained genes that do not need any outside object
 * - [Network][org.simbrain.network.core.Network] for genes that create or attach things to a network
 * - [OdorWorldEntity][org.simbrain.world.odorworld.entities.OdorWorldEntity] for genes that add sensors or effectors
 *
 * Example:
 * a neuron gene might keep a template neuron as [template], then [express] by copying that neuron into a target
 * [Network][org.simbrain.network.core.Network].
 *
 * Subclasses define the domain-specific details of how templates are copied, mutated, and expressed.
 */
abstract class Gene<C, P> {
    /** The mutable template or prototype used to produce phenotype objects during expression. */
    abstract val template: P

    /** Return an independent copy of this gene, including an appropriate copy of its template state. */
    abstract fun copy(): Gene<C, P>

    /**
     * Express this gene in the provided [context] and return the resulting phenotype object.
     *
     * Depending on the subtype, this may create a new object, copy [template], attach the result to [context],
     * or otherwise modify [context] as part of expression.
     */
    abstract suspend fun express(context: C): P

    /** Mutate the [template] in place by applying the provided edit block. */
    fun mutate(block: P.() -> Unit) {
        template.apply(block)
    }
}

/**
 * Base class for evolution simulations backed by a [Genotype].
 *
 * An [EvoSim] wraps one candidate solution in the evolutionary process.
 * The [genotype] stores the inherited structure and parameters, while the [workspace] holds the concrete components
 * built from that genotype for visualization or evaluation.
 *
 * Subclasses provide three main pieces:
 * - [onBuild] to express the genotype into the workspace
 * - [create] factory method to create a new evosim
 * - [eval] to evaluate the built sim and return a score
 *
 * This base class handles common behaviors such as:
 * - building only once even if [build] is called repeatedly
 * - delegating mutation to the genotype
 * - creating copied sims for evolution or visualization
 */
abstract class EvoSim<G : Genotype>(
    val genotype: G,

    val workspace: Workspace = Workspace(),

    /**
     * Optional metadata describing this candidate in the evolutionary run.
     *
     * This is typically set by [createDisplayCopy] so a sim can use information such as generation, fitness, or id
     * when naming components. It is usually `null` for internal clones created by the evolution loop.
     */
    val metadata: SimMetadata? = null
) {

    private var built = false

    /**
     * Build this sim if it has not already been built.
     *
     * Repeated calls are safe: after the first successful build, later calls do nothing.
     */
    suspend fun build() {
        if (!built) {
            onBuild()
            built = true
        }
    }

    /**
     * Express the genotype into the [workspace].
     *
     * This is where subclasses typically create components, add them to the workspace, and connect them together.
     * It is invoked by [build] and should assume the sim has not been built yet.
     */
    protected abstract suspend fun onBuild()

    /**
     * Factory method for producing another sim of the same concrete type.
     */
    protected abstract fun create(genotype: G, workspace: Workspace, metadata: SimMetadata?): EvoSim<G>

    /**
     * Evaluate this candidate and return its fitness or error metric.
     */
    abstract suspend fun eval(): Double

    /** Mutate this candidate by delegating to its [genotype]. */
    fun mutate() { genotype.mutate() }

    /** Return a copy of this sim with a copied genotype, a fresh workspace, and no [metadata]. */
    @Suppress("UNCHECKED_CAST")
    fun copy(): EvoSim<*> = create(genotype.copy() as G, Workspace(), null)

    /** Copy this sim into a provided [workspace], optionally annotated with [metadata]. This is done when the evosim is displayed in the "main" workspace. */
    @Suppress("UNCHECKED_CAST")
    fun createDisplayCopy(workspace: Workspace, metadata: SimMetadata? = null): EvoSim<*> =
        create(genotype.copy() as G, workspace, metadata)
}

/**
 * A typed list of related genes with convenience functions for copying and concatenation.
 *
 * In practice, chromosomes are used to keep one part of a genotype together, for example a set of node genes,
 * connection genes, or rule genes. Chromosomes may be empty, and they may grow or shrink during evolution if the
 * genotype's mutation logic adds or removes genes.
 */
class Chromosome<P, G : Gene<*, P>>(genes: List<G>) : MutableList<G> by ArrayList(genes) {

    /**
     * Provides a copy of the chromosome.
     */
    fun copy() = Chromosome(map { @Suppress("UNCHECKED_CAST") (it.copy() as G) })

    /**
     * Provides the ability to concatenate chromsomes. See usages.
     */
    operator fun plus(other: Chromosome<P, G>) = Chromosome(buildList { addAll(this@Chromosome); addAll(other); })
}

class EvolutionEvents : FlowEvents() {
    val beginEvolution = NoArgAwaitableEvent()
    val endEvolution = NoArgAwaitableEvent()
    /** Fires after [endEvolution] when the stopping condition was met (not when the user paused). */
    val targetReached = NoArgEvent()
    val generationUpdated = AwaitableEvent<GenerationState>()
}

/**
 * Runs an evolutionary loop with pause/resume/step support.
 *
 * Uses a channel-based task queue (same pattern as [SupervisedTrainer][org.simbrain.network.trainers.SupervisedTrainer])
 * to serialize control actions. Each generation completes fully before the next task is processed,
 * so pause/stop never interrupts a mid-generation evaluation.
 *
 * Usage:
 * ```
 * val runner = EvolutionRunner(evaluatorParams) { seed -> MySim(seed = seed) }
 * runner.events.generationUpdated.on { state -> updateDisplay(state) }
 * runner.startEvolving()   // continuous
 * runner.stopEvolving()    // pause
 * runner.evolveOnce()      // single step
 * runner.startEvolving()   // resume
 * ```
 */
class EvolutionRunner(
    val populatingFunction: (seed: Long) -> EvoSim<*>,
    val populationSize: Int,
    val eliminationRatio: Double,
    val stoppingFunction: GenerationState.() -> Boolean,
    val sortDescending: Boolean = true,
    val seed: Long = Random.nextLong(),
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Default + job

    constructor(
        evaluatorParams: EvaluatorParams,
        populatingFunction: (seed: Long) -> EvoSim<*>
    ) : this(
        populatingFunction = populatingFunction,
        populationSize = evaluatorParams.populationSize,
        eliminationRatio = evaluatorParams.eliminationRatio,
        stoppingFunction = {
            evaluatorParams.stoppingCondition.shouldStop(
                nthPercentileFitness(evaluatorParams.evaluationPercentile),
                evaluatorParams.targetMetric
            ) || generation > evaluatorParams.maxGenerations
        },
        sortDescending = evaluatorParams.stoppingCondition == EvaluatorParams.StoppingCondition.Fitness,
        seed = evaluatorParams.seed.toLong()
    )

    val events = EvolutionEvents()

    var generation = 0
        private set

    var isRunning = false
        private set

    var generationState: GenerationState? = null
        private set

    private var population: List<EvoSim<*>> = emptyList()
    private var metadata: List<SimMetadata> = emptyList()
    private var nextId = 0
    private var random = Random(seed)
    private var initialized = false
    private var stoppedByTarget = false

    private val processorChannel = Channel<Pair<EvolutionTask, CompletableDeferred<Unit>>>(capacity = Channel.UNLIMITED)

    sealed class EvolutionTask {
        object Start : EvolutionTask()
        object Evolve : EvolutionTask()
        object Stop : EvolutionTask()
    }

    init {
        launch {
            for ((task, signal) in processorChannel) {
                when (task) {
                    EvolutionTask.Start -> startHandler()
                    EvolutionTask.Evolve -> evolveOnceHandler()
                    EvolutionTask.Stop -> stopHandler()
                }
                signal.complete(Unit)
            }
        }
    }

    private suspend fun submitTask(task: EvolutionTask): CompletableDeferred<Unit> {
        val signal = CompletableDeferred<Unit>()
        processorChannel.send(task to signal)
        return signal
    }

    suspend fun startEvolving() {
        submitTask(EvolutionTask.Start).await()
    }

    suspend fun stopEvolving() {
        submitTask(EvolutionTask.Stop).await()
    }

    suspend fun evolveOnce() {
        submitTask(EvolutionTask.Evolve).await()
    }

    private fun initPopulation() {
        if (!initialized) {
            random = Random(seed)
            generation = 0
            nextId = 0
            population = List(populationSize) { populatingFunction(random.nextLong()) }
            metadata = population.map { SimMetadata(id = nextId++, parentId = null, generation = 0, fitness = 0.0) }
            initialized = true
        }
    }

    private suspend fun startHandler() {
        isRunning = true
        events.beginEvolution.fire()
        submitTask(EvolutionTask.Evolve)
    }

    private suspend fun evolveOnceHandler() {
        initPopulation()
        generation++

        val fitnessScores = coroutineScope {
            population.map { async { it.eval() } }.awaitAll()
        }
        val scored = (population zip metadata zip fitnessScores).map { (simMeta, fitness) ->
            simMeta.first to simMeta.second.copy(fitness = fitness)
        }
        val sorted = scored.shuffled(random).let {
            if (sortDescending) it.sortedByDescending { it.second.fitness }
            else it.sortedBy { it.second.fitness }
        }
        val state = GenerationState(generation, sorted)
        generationState = state
        events.generationUpdated.fire(state)

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

        if (isRunning) {
            if (stoppingFunction(state)) {
                stoppedByTarget = true
                submitTask(EvolutionTask.Stop)
            } else {
                submitTask(EvolutionTask.Evolve)
            }
        } else {
            submitTask(EvolutionTask.Stop)
        }
    }

    private suspend fun stopHandler() {
        isRunning = false
        events.endEvolution.fire()
        if (stoppedByTarget) {
            stoppedByTarget = false
            events.targetReached.fire()
        }
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
        min = 1,
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

    var evaluationPercentile by GuiEditable(
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
