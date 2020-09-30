package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.streams.toList


abstract class Gene5<T> protected constructor(val template: T) : CopyableObject

class NodeGene5 (template: Neuron) : Gene5<Neuron>(template) {

    private val copyListeners = LinkedList<(NodeGene5) -> Unit>()

    fun onCopy(task: (NodeGene5) -> Unit) {
        copyListeners.add(task)
    }

    override fun copy(): NodeGene5 {
        val newGene = NodeGene5(template.deepCopy())
        copyListeners.forEach { it(newGene) } // like firing an event
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

class Chromosome5<T, G : Gene5<T>>(val genes: MutableList<G>): CopyableObject {
    override fun copy(): Chromosome5<T, G> {
        return Chromosome5(genes.map { it.copy() as G }.toMutableList())
    }
}

fun <T, G : Gene5<T>> chromosome(count: Int, genes: (index: Int) -> G): Chromosome5<T, G> {
    return Chromosome5(List(count) { genes(it) }.toMutableList())
}

fun <T, G : Gene5<T>> chromosome(vararg genes: G): Chromosome5<T, G> {
    return Chromosome5(mutableListOf(*genes))
}

inline fun entity(type: EntityType, crossinline template: OdorWorldEntity.() -> Unit = { }): (OdorWorld) -> OdorWorldEntity {
    return { world -> OdorWorldEntity(world, type).apply(template) }
}

open class Memoization(protected val refList: LinkedList<Memoize<*>>) {

    var isInitial = refList.isEmpty()

    var refIterator = refList.iterator()

    fun <T> memoize(initializeValue: () -> T): Memoize<T> {
        return if (isInitial) {
            Memoize(initializeValue()).also { refList.add(it) }
        } else {
            refIterator.next() as Memoize<T>
        }
    }


}

class Memoize<T>(var current: T) {
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

    operator fun <T, G: Gene5<T>> get(gene: G) = map[gene] as T?

    operator fun <T, P, B: (P) -> T> get(builder: B) = map[builder] as T?

    operator fun <T, G: Gene5<T>> set(gene: G, product: T) {
        map[gene] = product as Any
    }

    operator fun <T, P, B: (P) -> T> set(builder: B, product: T) {
        map[builder] = product as Any
    }

}

class EvaluationContext(val workspace: Workspace, val mapping: ProductMap) {

    val <T, G: Gene5<T>, C: Chromosome5<T, G>> Memoize<C>.products: List<T> get() {
        return current.genes.map { mapping[it]!! }
    }

    val <T, P> ((P) -> T).products: T get() {
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
        private val template: EnvironmentBuilder.() -> Unit
): Memoization(refList) {

    constructor(builder: EnvironmentBuilder.() -> Unit): this(LinkedList(), builder)

    private val mutationTasks = mutableListOf<MutationContext.() -> Unit>()

    private val mutationContext = MutationContext()

    private lateinit var evalFunction: EvaluationContext.() -> Double

    private lateinit var builder: WorkspaceBuilder

    private lateinit var prettyBuilder: WorkspaceBuilder


    fun copy(): EnvironmentBuilder {
        return EnvironmentBuilder(LinkedList(refList.map { it.copy() }), template).apply(template)
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
        return Environment5(EvaluationContext(workspace, builder.productMapping), evalFunction)
    }

    fun build() = buildWith(builder)

    fun prettyBuild() = buildWith(prettyBuilder)

    fun onEval(eval: EvaluationContext.() -> Double) {
        evalFunction = eval
    }

}

fun environmentBuilder(builder: EnvironmentBuilder.() -> Unit) = EnvironmentBuilder(builder).apply(builder)

class WorkspaceBuilder {

    val builders = ArrayList<(Workspace) -> Unit>()

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
                tasks2.add { entity -> current.genes.forEach { entity.addSensor(it.build(entity)) } }
            }

            @JvmName("unaryPlusObjectSensorObjectSensorGene5")
            operator fun Memoize<Chromosome5<ObjectSensor, ObjectSensorGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { entity.addSensor(it.build(entity)) } }
            }

            @JvmName("unaryPlusStraightMovementStraightMovementGene5")
            operator fun Memoize<Chromosome5<StraightMovement, StraightMovementGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { entity.addEffector(it.build(entity)) } }
            }

            @JvmName("unaryPlusTurningTurningGene5")
            operator fun Memoize<Chromosome5<Turning, TurningGene5>>.unaryPlus() {
                tasks2.add { entity -> current.genes.forEach { entity.addEffector(it.build(entity)) } }
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

fun main() {
    val environmentBuilder = environmentBuilder {

        val inputs = memoize {
            chromosome(3) {
                nodeGene()
            }
        }

        val hiddens = memoize {
            chromosome(2) {
                nodeGene()
            }
        }

        val outputs = memoize {
            chromosome(3) {
                nodeGene {
                    updateRule.let {
                        if (it is LinearRule) {
                            it.lowerBound = 0.0
                        }
                    }
                }
            }
        }

        val connections = memoize {
            chromosome<Synapse, ConnectionGene5>()
        }

        val sensors = memoize {
            chromosome(3) {
                objectSensorGene {
                    setObjectType(EntityType.SWISS)
                    theta = it * 2 * Math.PI / 3
                    radius = 32.0
                }
            }
        }

        val straightMovement = memoize {
            chromosome(
                    straightMovementGene()
            )
        }

        val turning = memoize {
            chromosome(
                    turningGene { direction = -0.1 },
                    turningGene { direction = 0.1 }
            )
        }

        val mouse = entity(EntityType.MOUSE) {
            setCenterLocation(100.0, 200.0)
        }

        val cheese = entity(EntityType.SWISS) {
            setCenterLocation(150.0, 200.0)
        }

        onBuild {
            +odorworld {
                +mouse {
                    +sensors
                    +straightMovement
                    +turning
                }
                +cheese
            }
            +network {
                +inputs
                +hiddens
                +outputs
            }
        }

        onMutate {
            hiddens.current.eachMutate {
                updateRule.let {
                    if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }
            connections.current.eachMutate {
                strength += (Random().nextDouble() - 0.5 ) * 0.2
            }
            val source = (inputs.current.genes + hiddens.current.genes).shuffled().first()
            val target = (outputs.current.genes + hiddens.current.genes).shuffled().first()
            connections.current.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5 ) * 0.2
            })
        }

        onEval {
            var score = 0.0
            coupling {
                createOneToOneCouplings(
                        mouse.products.sensors.map { sensor ->
                            (sensor as ObjectSensor).getProducer("getCurrentValue")
                        },
                        inputs.products.map { neuron ->
                            neuron.getConsumer("setActivation")
                        }
                )
                createOneToOneCouplings(
                        inputs.products.map { neuron ->
                            neuron.getProducer("getActivation")
                        },
                        mouse.products.effectors.map { effector ->
                            effector.getConsumer("setAmount")
                        }
                )
            }
            cheese.products.onCollide { other ->
                if (other === mouse.products) {
                    score += 1
                }
                cheese.products.randomizeLocation()
            }
            repeat(100) {
                workspace.simpleIterate()
            }
            score + mouse.products.sensors.sumByDouble { (it as ObjectSensor).currentValue }
        }

    }

    val population = generateSequence(environmentBuilder.copy()) { it.copy() }.take(100).toList()

    val result = sequence {
        var next = population
        while (true) {
            val current = next.parallelStream().map {
                val build = it.build()
                val score = build.eval()
                Pair(it, score)
            }.toList().sortedBy { it.second }
            val survivors = current.take(current.size / 2)
            next = survivors.map { it.first } + survivors.parallelStream().map { it.first.copy().apply { mutate() } }.toList()
            yield(current)
        }
    }.onEach { println(it.first().second) }.take(1000).takeWhile { it[0].second < 2 }

    println(result.last().first())
}
