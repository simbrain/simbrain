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


abstract class Gene<T> protected constructor(val template: T) : CopyableObject

class Chromosome<T, G : Gene<T>>(val genes: MutableList<G>): CopyableObject {
    @Suppress("UNCHECKED_CAST")
    override fun copy(): Chromosome<T, G> {
        return Chromosome(genes.map { it.copy() as G }.toMutableList())
    }
}

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

object MutationContext {

    inline fun <T, G: Gene<T>> Chromosome<T, G>.eachMutate(mutationTask: T.() -> Unit) {
        genes.forEach { it.template.mutationTask() }
    }

    inline fun <T> Gene<T>.mutate(mutationTask: T.() -> Unit) {
        template.mutationTask()
    }

}

class Environment(val evaluationContext: EvaluationContext, private val evalFunction: EvaluationContext.() -> Double) {

    fun eval() = evaluationContext.evalFunction()

}

class EnvironmentBuilder private constructor(
        private val chromosomeList: LinkedList<Chromosome<*, *>>,
        private val template: EnvironmentBuilder.() -> Unit,
        val seed: Int = Random.nextInt(),
        val random: Random = Random(seed)
) {

    constructor(builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder)

    constructor(seed: Int, builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder, seed)

    private val mutationTasks = mutableListOf<MutationContext.() -> Unit>()

    private lateinit var evalFunction: EvaluationContext.() -> Double

    private lateinit var builder: WorkspaceBuilder

    private lateinit var prettyBuilder: WorkspaceBuilder

    private val isInitial = chromosomeList.isEmpty()

    private val chromosomeIterator = chromosomeList.iterator()


    fun copy(): EnvironmentBuilder {
        val newSeed = random.nextInt()
        return EnvironmentBuilder(LinkedList(chromosomeList.map { it.copy() }), template, newSeed).apply(template)
    }

    fun onMutate(task: MutationContext.() -> Unit) {
        mutationTasks.add(task)
    }

    fun mutate() {
        mutationTasks.forEach { MutationContext.it() }
    }

    fun onBuild(template: WorkspaceBuilder.() -> Unit) {
        builder = WorkspaceBuilder().apply(template)
    }

    fun onPrettyBuild(template: WorkspaceBuilder.() -> Unit) {
        prettyBuilder = WorkspaceBuilder().apply(template)
    }

    private fun buildWith(builder: WorkspaceBuilder): Environment {
        val workspace = Workspace()
        builder.builders.forEach { it(workspace) }
        builder.couplings.forEach { workspace.couplingManager.it(builder.productMapping) }
        val newSeed = random.nextInt()
        return Environment(EvaluationContext(workspace, builder.productMapping, Random(newSeed)), evalFunction)
    }

    fun build() = buildWith(builder)

    fun prettyBuild() = buildWith(prettyBuilder)

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

    fun <T, G : Gene<T>> chromosome(count: Int, genes: (index: Int) -> G): Chromosome<T, G> {
        return createChromosome { Chromosome(List(count) { genes(it) }.toMutableList()) }
    }

    fun <T, G : Gene<T>> chromosome(vararg genes: G): Chromosome<T, G> {
        return createChromosome { Chromosome(mutableListOf(*genes)) }
    }

    fun <T, G : Gene<T>> chromosome(genes: Iterable<G>): Chromosome<T, G> {
        return createChromosome { Chromosome(genes.toMutableList()) }
    }

    fun <T, G : Gene<T>> chromosome(listBuilder: MutableList<G>.() -> Unit): Chromosome<T, G> {
        return createChromosome { Chromosome(mutableListOf<G>().apply(listBuilder)) }
    }

}

fun environmentBuilder(builder: EnvironmentBuilder.() -> Unit) = EnvironmentBuilder(builder).apply(builder)

fun environmentBuilder(seed: Int, builder: EnvironmentBuilder.() -> Unit)
        = EnvironmentBuilder(seed, builder).apply(builder)

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