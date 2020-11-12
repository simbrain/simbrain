package org.simbrain.util.geneticalgorithms

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
 */
abstract class Gene<T> protected constructor(val template: T) : CopyableObject

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
}

/**
 * Maps genes to products or builder functions to products.  E.g. [NodeGene] to [Neuron] or supplier<OdorWorldEntity>
 * to [OdorWorldEntity].  Think of "gene products", not the gene itself but the thing it expresses.
 */
class ProductMap(private val map: HashMap<Any, Any> = HashMap()) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T, G: Gene<T>> get(gene: G) = map[gene] as T?

    @Suppress("UNCHECKED_CAST")
    operator fun <T, P, B: (P) -> T> get(builder: B) = map[builder] as T?

    operator fun <T, G: Gene<T>> set(gene: G, product: T) {
        map[gene] = product as Any
    }

    operator fun <T, P, B: (P) -> T> set(builder: B, product: T) {
        map[builder] = product as Any
    }

}

/**
 * Provides a context for [EnvironmentBuilder.onEval]. "This" in onEval will refer to an instance of this class.
 */
class EvaluationContext(val workspace: Workspace, val mapping: ProductMap, val evalRand: Random) {

    val <T, G: Gene<T>, C: Chromosome<T, G>> C.products: List<T> get() {
        return genes.map { mapping[it]!! }
    }

    val <T, P> ((P) -> T).product: T get() {
        return mapping[this]!!
    }

    fun coupling(couplingManager: CouplingManager.() -> Unit) {
        workspace.couplingManager.apply(couplingManager)
    }

}

/**
 * Provides a context for [EnvironmentBuilder.onMutate]. "This" in onMutate will refer to this object.
 */
object MutationContext {

    inline fun <T, G: Gene<T>> Chromosome<T, G>.eachMutate(mutationTask: T.() -> Unit) {
        genes.forEach { it.template.mutationTask() }
    }

    inline fun <T> Gene<T>.mutate(mutationTask: T.() -> Unit) {
        template.mutationTask()
    }

}

/**
 * The environment produced by [EnvironmentBuilder.build]. All it does is provide for evaluation of a fitness function.
 */
class Environment(val evaluationContext: EvaluationContext, private val evalFunction: EvaluationContext.() -> Double) {

    fun eval() = evaluationContext.evalFunction()

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

    // TODO: Replace this with something more generic
    /**
     * Builders which build the products ("agents") associated with the chromosomes
     */
    private lateinit var builder: WorkspaceBuilder

    /**
     * A separate builder that allows you to build "nicer" products.  Whatever is done here is not needed to evaluate
     * an agent, but can be done for nice layout on the final product that is viewed.
     */
    private lateinit var prettyBuilder: WorkspaceBuilder

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
     * Use this to describe what happens when the builder builds products.  Can only be called once.
     */
    fun onBuild(template: WorkspaceBuilder.() -> Unit) {
        builder = WorkspaceBuilder().apply(template)
    }

    /**
     * Use this to describe what happens o "pretty build". Usually called for the "best" evolved agent, which can
     * then be displayed.
     */
    fun onPrettyBuild(template: WorkspaceBuilder.() -> Unit) {
        prettyBuilder = WorkspaceBuilder().apply(template)
    }

    /**
     * Builds the products.
     */
    private fun buildWith(builder: WorkspaceBuilder): Environment {
        val workspace = Workspace()
        builder.builders.forEach { it(workspace) }
        builder.couplings.forEach { workspace.couplingManager.it(builder.productMapping) }
        val newSeed = random.nextInt()
        return Environment(EvaluationContext(workspace, builder.productMapping, Random(newSeed)), evalFunction)
    }

    fun build() = buildWith(builder)

    fun prettyBuild() = buildWith(prettyBuilder)

    /**
     * Use this to define your evaluation / fitness function.
     */
    fun onEval(eval: EvaluationContext.() -> Double) {
        evalFunction = eval
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
    fun <T, G : Gene<T>> chromosome(count: Int, genes: (index: Int) -> G): Chromosome<T, G> {
        return createChromosome { Chromosome(List(count) { genes(it) }.toMutableList()) }
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

// TODO: Decouple from here down and move to a separate file
class WorkspaceBuilder {

    val builders = ArrayList<(Workspace) -> Unit>()

    val couplings = LinkedList<CouplingManager.(ProductMap) -> Unit>()

    val productMapping = ProductMap()

    /**
     * Returns a NetworkAgentBuilder
     */
    fun network(builder: NetworkAgentBuilder.() -> Unit): NetworkAgentBuilder {
        return NetworkAgentBuilder().apply(builder)
    }

    fun odorworld(builder: OdorWorldAgentBuilder.() -> Unit): OdorWorldAgentBuilder {
        return OdorWorldAgentBuilder().apply(builder)
    }

    fun couplingManager(template: CouplingManagerContext.() -> Unit) {
        CouplingManagerContext().template()
    }

    inner class CouplingManagerContext {

        operator fun <T1, T2, G1 : Gene<T1>, G2 : Gene<T2>> Chromosome<T1, G1>.plus(
                other: Chromosome<T2, G2>
        ) = listOf(this, other)

        fun couple(producers: Collection<Gene<*>>, consumers: Collection<Gene<*>>) {
            couplings.add { mapping ->
                (producers zip consumers).map { (source, target) ->
                    mapping[source] to mapping[target]
                }.takeWhile { (source, target) ->
                    source != null && target != null
                }.forEach { (source, target) ->
                    val producer = when (source) {
                        is Neuron -> source.getProducerByMethodName("getActivation")
                        is ObjectSensor -> source.getProducerByMethodName("getCurrentValue")
                        else -> TODO("Not implemented: ${source?.javaClass?.simpleName}")
                    }
                    val consumer = when (target) {
                        is Neuron -> target.getConsumerByMethodName("setInputValue")
                        is Effector -> target.getConsumerByMethodName("setAmount")
                        else -> TODO("Not implemented: ${target?.javaClass?.simpleName}")
                    }
                    createCoupling(producer, consumer)
                }
            }
        }

        fun <T, G : Gene<T>> couple(
                producers: Chromosome<T, G>,
                consumers: List<Chromosome<out Any?, out Gene<out Any?>>>
        ) {
            couple(producers.genes, consumers.flatMap { it.genes })
        }

        fun <T, G : Gene<T>> couple(
                producers: List<Chromosome<out Any?, out Gene<out Any?>>>,
                consumers: Chromosome<T, G>
        ) {
            couple(producers.flatMap { it.genes }, consumers.genes)
        }

        fun couple(
                producers: List<Chromosome<out Any?, out Gene<out Any?>>>,
                consumers: List<Chromosome<out Any?, out Gene<out Any?>>>
        ) {
            couple(producers.flatMap { it.genes }, consumers.flatMap { it.genes })
        }

        fun <T1, T2, G1 : Gene<T1>, G2 : Gene<T2>> couple(
                producers: Chromosome<T1, G1>,
                consumers: Chromosome<T2, G2>
        ) {
            couple(producers.genes, consumers.genes)
        }
    }

    inner class OdorWorldAgentBuilder {

        private val tasks = LinkedList<(OdorWorld) -> Unit>()

        operator fun unaryPlus() {
            builders.add { workspace: Workspace ->
                workspace.addWorkspaceComponent(OdorWorldComponent("TempWorld", OdorWorld().also { world ->
                    tasks.forEach { task -> task(world) }
                }))
            }
        }

        operator fun ((OdorWorld) -> OdorWorldEntity).invoke(
                template: OdorWorldEntityAgentBuilder.() -> Unit = { }
        ): OdorWorldEntityAgentBuilder {
            return OdorWorldEntityAgentBuilder(this).apply(template)
        }

        operator fun ((OdorWorld) -> OdorWorldEntity).unaryPlus() {
            tasks.add { world ->
                this(world).also { entity ->
                    world.addEntity(entity)
                    productMapping[this] = entity
                }
            }
        }

        inner class OdorWorldEntityAgentBuilder(val template: (OdorWorld) -> OdorWorldEntity) {
            private val tasks2 = LinkedList<(OdorWorldEntity) -> Unit>()

            operator fun unaryPlus() {
                tasks.add { world ->
                    template(world).also { entity ->
                        world.addEntity(entity)
                        tasks2.forEach { task -> task(entity) }
                        productMapping[template] = entity
                    }
                }
            }

            @JvmName("unaryPlusSmellSensorSmellSensorGene")
            operator fun Chromosome<SmellSensor, SmellSensorGene>.unaryPlus() {
                tasks2.add { entity -> genes.forEach { gene ->
                    entity.addSensor(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusObjectSensorObjectSensorGene")
            operator fun Chromosome<ObjectSensor, ObjectSensorGene>.unaryPlus() {
                tasks2.add { entity -> genes.forEach { gene ->
                    entity.addSensor(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusStraightMovementStraightMovementGene")
            operator fun Chromosome<StraightMovement, StraightMovementGene>.unaryPlus() {
                tasks2.add { entity -> genes.forEach { gene ->
                    entity.addEffector(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusTurningTurningGene")
            operator fun Chromosome<Turning, TurningGene>.unaryPlus() {
                tasks2.add { entity -> genes.forEach { gene ->
                    entity.addEffector(gene.build(entity).also { productMapping[gene] = it })
                } }
            }
        }

    }

    inner class NetworkAgentBuilder {

        private val tasks = LinkedList<(Network) -> Unit>()

        private inline fun <C: Chromosome<T, G>, G: Gene<T>, T> C.addGene(
                crossinline adder: (gene: G, net: Network) -> T
        ) {
            tasks.add { net ->
                genes.forEach {
                    productMapping[it] = adder(it, net)
                }
            }
        }

        operator fun unaryPlus() {
            builders.add { workspace: Workspace ->
                workspace.addWorkspaceComponent(NetworkComponent("TempNet", Network().also { network ->
                    tasks.forEach { task -> task(network) }
                }))
            }
        }

        operator fun ((Network) -> Unit).unaryPlus() {
            tasks.add(this)
        }

        @JvmName("unaryPlusNeuron")
        operator fun <C: Chromosome<Neuron, NodeGene>> C.unaryPlus() {
            addGene { gene, net ->
                gene.build(net).also { neuron ->
                    net.addLooseNeuron(neuron)
                }
            }
        }

        private inline fun <T, G: Gene<T>, C: Chromosome<T, G>> C.option(
                crossinline options: List<T>.() -> Unit,
                crossinline adder: (gene: G, net: Network) -> T
        ): (Network) -> Unit {
            return { net ->
                genes.map { gene ->
                    adder(gene, net).also { productMapping[gene] = it }
                }.options()
            }
        }

        operator fun <C: Chromosome<Neuron, NodeGene>> C.invoke(options: List<Neuron>.() -> Unit) =
                option(options) { gene, net ->
                    gene.build(net).also { neuron ->
                        net.addLooseNeuron(neuron)
                    }
                }

        fun <C: Chromosome<Neuron, NodeGene>> C.asGroup(
                options: NeuronGroup.() -> Unit = {  }
        ): (Network) -> Unit {
            return { net ->
                genes.map { gene ->
                    gene.build(net).also { productMapping[gene] = it }
                }.let { net.addNeuronGroup(NeuronGroup(net, it).apply(options)) }
            }
        }

        @JvmName("unaryPlusSynapse")
        operator fun <C: Chromosome<Synapse, ConnectionGene>> C.unaryPlus() {
            addGene { gene, net ->
                gene.build(net, productMapping[gene.source]!!, productMapping[gene.target]!!)
                        .also { synapse -> net.addLooseSynapse(synapse) }
            }
        }
    }

}

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