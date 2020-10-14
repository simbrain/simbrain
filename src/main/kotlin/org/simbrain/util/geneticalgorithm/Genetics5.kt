package org.simbrain.util.geneticalgorithm

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
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random


abstract class Gene5<T> protected constructor(val template: T) : CopyableObject

class NodeGene5 (template: Neuron) : Gene5<Neuron>(template) {

    private val copyListeners = LinkedList<(NodeGene5) -> Unit>()

    fun onCopy(task: (NodeGene5) -> Unit) {
        copyListeners.add(task)
    }

    private fun fireCopied(newGene: NodeGene5) {
        copyListeners.forEach { it(newGene) }
    }

    override fun copy(): NodeGene5 {
        val newGene = NodeGene5(template.deepCopy())
        fireCopied(newGene)
        return newGene
    }

    fun build(network: Network): Neuron {
        return Neuron(network, template)
    }

}

class ConnectionGene5 (template: Synapse, val source: NodeGene5, val target: NodeGene5) : Gene5<Synapse>(template) {

    lateinit var sourceCopy: NodeGene5
    lateinit var targetCopy: NodeGene5

    init {
        source.onCopy { sourceCopy = it }
        target.onCopy { targetCopy = it }
    }

    override fun copy(): ConnectionGene5 {
        return ConnectionGene5(Synapse(template), sourceCopy, targetCopy)
    }

    fun build(network: Network, source: Neuron, target: Neuron): Synapse {
        return Synapse(network, source, target, template.learningRule, template)
    }

}

class SmellSensorGene5(template: SmellSensor): Gene5<SmellSensor>(template) {

    override fun copy(): SmellSensorGene5 {
        return SmellSensorGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): SmellSensor {
        return SmellSensor(template).apply { parent = odorWorldEntity }
    }

}

class ObjectSensorGene5(template: ObjectSensor): Gene5<ObjectSensor>(template) {

    override fun copy(): ObjectSensorGene5 {
        return ObjectSensorGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): ObjectSensor {
        return ObjectSensor(template).apply { parent = odorWorldEntity }
    }

}

class StraightMovementGene5(template: StraightMovement): Gene5<StraightMovement>(template) {

    override fun copy(): StraightMovementGene5 {
        return StraightMovementGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): StraightMovement {
        return StraightMovement(template).apply { parent = odorWorldEntity }
    }

}

class TurningGene5(template: Turning): Gene5<Turning>(template) {

    override fun copy(): TurningGene5 {
        return TurningGene5(template.copy())
    }

    fun build(odorWorldEntity: OdorWorldEntity): Turning {
        return Turning(template).apply { parent = odorWorldEntity }
    }

}

inline fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene5 {
    return NodeGene5(Neuron(null).apply(options))
}

inline fun connectionGene(source: NodeGene5, target: NodeGene5, options: Synapse.() -> Unit = { }): ConnectionGene5 {
    return ConnectionGene5(Synapse(null, null as Neuron?).apply(options), source, target)
}

inline fun smellSensorGene(options: SmellSensor.() -> Unit = { }): SmellSensorGene5 {
    return SmellSensorGene5(SmellSensor().apply(options))
}

inline fun objectSensorGene(options: ObjectSensor.() -> Unit = { }): ObjectSensorGene5 {
    return ObjectSensorGene5(ObjectSensor().apply(options))
}

inline fun straightMovementGene(options: StraightMovement.() -> Unit = { }): StraightMovementGene5 {
    return StraightMovementGene5(StraightMovement().apply(options))
}

inline fun turningGene(options: Turning.() -> Unit = { }): TurningGene5 {
    return TurningGene5(Turning().apply(options))
}

inline fun entity(type: EntityType, crossinline template: OdorWorldEntity.() -> Unit = { }): (OdorWorld) -> OdorWorldEntity {
    return { world -> OdorWorldEntity(world, type).apply(template) }
}

class Chromosome5<T, G : Gene5<T>>(val genes: MutableList<G>): CopyableObject {
    @Suppress("UNCHECKED_CAST")
    override fun copy(): Chromosome5<T, G> {
        return Chromosome5(genes.map { it.copy() as G }.toMutableList())
    }
}

open class Memoization(protected val refList: LinkedList<Memoize<*>>) {

    private var isInitial = refList.isEmpty()

    private var refIterator = refList.iterator()

    protected fun <T> memoize(initializeValue: () -> T): Memoize<T> {
        return if (isInitial) {
            Memoize(initializeValue()).also { refList.add(it) }
        } else {
            @Suppress("UNCHECKED_CAST")
            refIterator.next() as Memoize<T>
        }
    }


}

class Memoize<T>(var current: T) {
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun copy(): Memoize<T> {
        return Memoize(current.let {
            when (it) {
                is CopyableObject -> it.copy()
                is MutableList<*> -> mutableListOf(it)
                else -> it
            } as T
        })
    }
}


class ProductMap(private val map: HashMap<Any, Any> = HashMap()) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T, G: Gene5<T>> get(gene: G) = map[gene] as T?

    @Suppress("UNCHECKED_CAST")
    operator fun <T, P, B: (P) -> T> get(builder: B) = map[builder] as T?

    operator fun <T, G: Gene5<T>> set(gene: G, product: T) {
        map[gene] = product as Any
    }

    operator fun <T, P, B: (P) -> T> set(builder: B, product: T) {
        map[builder] = product as Any
    }

}

class EvaluationContext(val workspace: Workspace, val mapping: ProductMap, val evalRand: Random) {

    val <T, G: Gene5<T>, C: Chromosome5<T, G>> Memoize<C>.products: List<T> get() {
        return current.genes.map { mapping[it]!! }
    }

    val <T, P> ((P) -> T).product: T get() {
        return mapping[this]!!
    }

    fun coupling(couplingManager: CouplingManager.() -> Unit) {
        workspace.couplingManager.apply(couplingManager)
    }

}

class Environment5(val evaluationContext: EvaluationContext, private val evalFunction: EvaluationContext.() -> Double) {

    fun eval() = evaluationContext.evalFunction()

}

class MutationContext {

    inline fun <T, G: Gene5<T>>Chromosome5<T, G>.eachMutate(mutationTask: T.() -> Unit) {
        genes.forEach { it.template.mutationTask() }
    }

    inline fun <T> Gene5<T>.mutate(mutationTask: T.() -> Unit) {
        template.mutationTask()
    }

}

class EnvironmentBuilder private constructor(
        refList: LinkedList<Memoize<*>>,
        private val template: EnvironmentBuilder.() -> Unit,
        val seed: Int = Random.nextInt(),
        val random: Random = Random(seed)
): Memoization(refList) {

    constructor(builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder)

    constructor(seed: Int, builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder, seed)

    private val mutationTasks = mutableListOf<MutationContext.() -> Unit>()

    private val mutationContext = MutationContext()

    private lateinit var evalFunction: EvaluationContext.() -> Double

    private lateinit var builder: WorkspaceBuilder

    private lateinit var prettyBuilder: WorkspaceBuilder


    fun copy(): EnvironmentBuilder {
        val newSeed = random.nextInt()
        return EnvironmentBuilder(LinkedList(refList.map { it.copy() }), template, newSeed).apply(template)
    }

    fun onMutate(task: MutationContext.() -> Unit) {
        mutationTasks.add(task)
    }

    fun mutate() {
        mutationTasks.forEach { mutationContext.it() }
    }

    fun onBuild(template: WorkspaceBuilder.() -> Unit) {
        builder = WorkspaceBuilder().apply(template)
    }

    fun onPrettyBuild(template: WorkspaceBuilder.() -> Unit) {
        prettyBuilder = WorkspaceBuilder().apply(template)
    }

    private fun buildWith(builder: WorkspaceBuilder): Environment5 {
        val workspace = Workspace()
        builder.builders.forEach { it(workspace) }
        builder.couplings.forEach { workspace.couplingManager.it(builder.productMapping) }
        val newSeed = random.nextInt()
        return Environment5(EvaluationContext(workspace, builder.productMapping, Random(newSeed)), evalFunction)
    }

    fun build() = buildWith(builder)

    fun prettyBuild() = buildWith(prettyBuilder)

    fun onEval(eval: EvaluationContext.() -> Double) {
        evalFunction = eval
    }

    fun <T, G : Gene5<T>> chromosome(count: Int, genes: (index: Int) -> G): Memoize<Chromosome5<T, G>> {
        return memoize { Chromosome5(List(count) { genes(it) }.toMutableList()) }
    }

    fun <T, G : Gene5<T>> chromosome(vararg genes: G): Memoize<Chromosome5<T, G>> {
        return memoize { Chromosome5(mutableListOf(*genes)) }
    }

    fun <T, G : Gene5<T>> chromosome(genes: Iterable<G>): Memoize<Chromosome5<T, G>> {
        return memoize { Chromosome5(genes.toMutableList()) }
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

        operator fun <T1, T2, G1 : Gene5<T1>, G2 : Gene5<T2>> Memoize<Chromosome5<T1, G1>>.plus(
                other: Memoize<Chromosome5<T2, G2>>
        ) = listOf(this, other)

        fun couple(producers: Collection<Gene5<*>>, consumers: Collection<Gene5<*>>) {
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

        fun <T, G : Gene5<T>> couple(
                producers: Memoize<Chromosome5<T, G>>,
                consumers: List<Memoize<out Chromosome5<out Any?, out Gene5<out Any?>>>>
        ) {
            couple(producers.current.genes, consumers.flatMap { it.current.genes })
        }

        fun <T, G : Gene5<T>> couple(
                producers: List<Memoize<out Chromosome5<out Any?, out Gene5<out Any?>>>>,
                consumers: Memoize<Chromosome5<T, G>>
        ) {
            couple(producers.flatMap { it.current.genes }, consumers.current.genes)
        }

        fun couple(
                producers: List<Memoize<out Chromosome5<out Any?, out Gene5<out Any?>>>>,
                consumers: List<Memoize<out Chromosome5<out Any?, out Gene5<out Any?>>>>
        ) {
            couple(producers.flatMap { it.current.genes }, consumers.flatMap { it.current.genes })
        }

        fun <T1, T2, G1 : Gene5<T1>, G2 : Gene5<T2>> couple(
                producers: Memoize<Chromosome5<T1, G1>>,
                consumers: Memoize<Chromosome5<T2, G2>>
        ) {
            couple(producers.current.genes, consumers.current.genes)
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

            @JvmName("unaryPlusSmellSensorSmellSensorGene5")
            operator fun Memoize<Chromosome5<SmellSensor, SmellSensorGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { gene ->
                    entity.addSensor(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusObjectSensorObjectSensorGene5")
            operator fun Memoize<Chromosome5<ObjectSensor, ObjectSensorGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { gene ->
                    entity.addSensor(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusStraightMovementStraightMovementGene5")
            operator fun Memoize<Chromosome5<StraightMovement, StraightMovementGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { gene ->
                    entity.addEffector(gene.build(entity).also { productMapping[gene] = it })
                } }
            }

            @JvmName("unaryPlusTurningTurningGene5")
            operator fun Memoize<Chromosome5<Turning, TurningGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { gene ->
                    entity.addEffector(gene.build(entity).also { productMapping[gene] = it })
                } }
            }
        }

    }

    inner class NetworkAgentBuilder {

        private val tasks = LinkedList<(Network) -> Unit>()

        private inline fun <C: Chromosome5<T, G>, G: Gene5<T>, T> Memoize<C>.addGene(
                crossinline adder: (gene: G, net: Network) -> T
        ) {
            tasks.add { net ->
                current.genes.forEach {
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
        operator fun <C: Chromosome5<Neuron, NodeGene5>> Memoize<C>.unaryPlus() {
            addGene { gene, net ->
                gene.build(net).also { neuron ->
                    net.addLooseNeuron(neuron)
                }
            }
        }

        private inline fun <T, G: Gene5<T>, C: Chromosome5<T, G>> Memoize<C>.option(
                crossinline options: List<T>.() -> Unit,
                crossinline adder: (gene: G, net: Network) -> T
        ): (Network) -> Unit {
            return { net ->
                current.genes.map { gene ->
                    adder(gene, net).also { productMapping[gene] = it }
                }.options()
            }
        }

        operator fun <C: Chromosome5<Neuron, NodeGene5>> Memoize<C>.invoke(options: List<Neuron>.() -> Unit) =
                option(options) { gene, net ->
                    gene.build(net).also { neuron ->
                        net.addLooseNeuron(neuron)
                    }
                }

        fun <C: Chromosome5<Neuron, NodeGene5>> Memoize<C>.asGroup(
                options: NeuronGroup.() -> Unit = {  }
        ): (Network) -> Unit {
            return { net ->
                current.genes.map { gene ->
                    gene.build(net).also { productMapping[gene] = it }
                }.let { net.addNeuronGroup(NeuronGroup(net, it).apply(options)) }
            }
        }

        @JvmName("unaryPlusSynapse")
        operator fun <C: Chromosome5<Synapse, ConnectionGene5>> Memoize<C>.unaryPlus() {
            addGene { gene, net ->
                gene.build(net, productMapping[gene.source]!!, productMapping[gene.target]!!)
                        .also { synapse -> net.addLooseSynapse(synapse) }
            }
        }
    }

}