package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


abstract class Gene5<T> protected constructor(val template: T, protected val mutationTasks: MutableList<T.() -> Unit>)
    : CopyableObject {

    fun mutate() {
        mutationTasks.forEach { template.it() }
    }

    abstract override fun copy(): Gene5<T>

    fun onMutate(options: T.() -> Unit) {
        mutationTasks.add(options)
    }

}

class NodeGene5 private constructor(template: Neuron, mutationTasks: MutableList<Neuron.() -> Unit>)
    : Gene5<Neuron>(template, mutationTasks) {

    constructor(template: Neuron) : this(template, mutableListOf())

    override fun copy(): Gene5<Neuron> {
        return NodeGene5(template.deepCopy()!!, mutationTasks)
    }

    fun build(network: Network): Neuron {
        return Neuron(network, template)
    }

}

class ConnectionGene5 private constructor(
        template: Synapse,
        val source: NodeGene5,
        val target: NodeGene5,
        mutationTasks: MutableList<Synapse.() -> Unit>
)
    : Gene5<Synapse>(template, mutationTasks) {

    constructor(template: Synapse, source: NodeGene5, target: NodeGene5)
            : this(template, source, target, mutableListOf())

    override fun copy(): ConnectionGene5 {
        return ConnectionGene5(template.copy(), source, target, mutationTasks)
    }

    fun build(network: Network, source: Neuron, target: Neuron): Synapse {
        return Synapse(network, source, target, template.learningRule, template)
    }

}

private fun Synapse.copy(): Synapse {
    return TODO("implement")
}

class PeripheralGene5<T: PeripheralAttribute> private constructor(
        template: T,
        mutationTasks: MutableList<T.() -> Unit>
): Gene5<T>(template, mutationTasks) {

    constructor(template: T) : this(template, mutableListOf())

    override fun copy(): PeripheralGene5<T> {
        val newPeripheralAttribute = template.copy()!! as T
        return PeripheralGene5(newPeripheralAttribute)
    }

//    fun build(network: Network, odorWorldEntity: OdorWorldEntity): PeripheralGeneType<T> {
//        return PeripheralGeneType(Neuron(network, neuron), )
//    }

}

fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene5 {
    return NodeGene5(Neuron(null).apply(options))
}

fun connectionGene(source: NodeGene5, target: NodeGene5, options: Synapse.() -> Unit = { }): ConnectionGene5 {
    return ConnectionGene5(Synapse(null, null as Neuron?).apply(options), source, target)
}

fun smellSensorGene(options: SmellSensor.() -> Unit): PeripheralGene5<SmellSensor> {
    return PeripheralGene5(SmellSensor(OdorWorldEntity(null)))
}

class Chromosome5<T, G : Gene5<T>>(val genes: MutableList<G>): CopyableObject {
    override fun copy(): Chromosome5<T, G> {
        return Chromosome5(genes.map { it.copy() as G }.toMutableList())
    }
}

fun <T, G : Gene5<T>> chromosome(count: Int, genes: () -> G): Chromosome5<T, G> {
    return Chromosome5(List(count) { genes() }.toMutableList())
}

fun <T, G : Gene5<T>> chromosome(genes: () -> MutableList<G>): Chromosome5<T, G> {
    return Chromosome5(genes())
}

open class Memoization(protected val refList: MutableList<Memoize<*>>) {

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

class Memoize<T>(val current: T)

class Agent5(val network: Network, val odorWorldEntity: OdorWorldEntity)

class SimBuilder private constructor(refList: MutableList<Memoize<*>>, private val builder: SimBuilder.() -> Unit):
        Memoization(refList)  {

    constructor(builder: SimBuilder.() -> Unit): this(mutableListOf(), builder)

    val mutationTasks = mutableListOf<() -> Unit>()

    fun copy(): SimBuilder {
        return SimBuilder(refList, builder).apply(builder)
    }

    fun onMutate(task: () -> Unit) {
        mutationTasks.add(task)
    }

    fun mutate() {
        mutationTasks.forEach { it() }
    }

    infix fun NodeGene5.connects(target: NodeGene5): Chromosome5<Synapse, ConnectionGene5> {
        TODO("Not yet implemented")
    }

    val builders = LinkedList<WorkspaceBuilder>()

    fun onBuild(builder: WorkspaceBuilder.() -> Unit) {
        builders.add(WorkspaceBuilder().apply(builder))
    }

    fun build(): Workspace {
        val workspace by lazy { Workspace() }
        builders.forEach { it.builders.forEach { it(workspace) } }
        return workspace
    }

}

fun sim(builder: SimBuilder.() -> Unit) = SimBuilder(builder).apply(builder)

class WorkspaceBuilder {

    val builders = ArrayList<(Workspace) -> Unit>()

    fun network(builder: NetworkAgentBuilder.() -> Unit): NetworkAgentBuilder {
        return NetworkAgentBuilder().apply(builder)
    }

    fun odorworld() {

    }

    operator fun NetworkAgentBuilder.unaryPlus() {
        builders.add { workspace: Workspace ->
            workspace.addWorkspaceComponent(NetworkComponent("TempNet", Network().also { network ->
                tasks.forEach { task -> task(network) }
            }))
        }
    }

    class NetworkAgentBuilder {

        val tasks = LinkedList<(Network) -> Unit>()

        val neuronMapping = HashMap<NodeGene5, Neuron>()

        operator fun <T: Chromosome5<*, *>> Memoize<T>.unaryPlus(): Memoize<T> {
            tasks.add { net ->
                this.current.genes.forEach {
                    if (it is NodeGene5) {
                        it.build(net).also { neuron ->
                            net.addLooseNeuron(neuron)
                            neuronMapping[it] = neuron
                        }
                    } else if (it is ConnectionGene5) {
                        it.build(net, neuronMapping[it.source]!!, neuronMapping[it.target]!!)
                                .also { synapse -> net.addLooseSynapse(synapse) }
                    }
                }
            }
            return this
        }
    }

}

class WorkspaceBuilderCoupling {

    infix fun PeripheralGene5<*>.connects(other: NodeGene5) {

    }

    operator fun invoke(block: WorkspaceBuilderCoupling.() -> Unit) {

    }

}

fun main() {
    val thing = sim {

        val frontSensor by lazy {
            smellSensorGene {
                theta = 0.0
                radius = 24.0
            }
        }

        val backSensor by lazy {
            smellSensorGene {
                theta = 3.14159
                radius = 24.0
            }
        }

        val inputs = memoize { chromosome(2) { nodeGene() } }
        val nodes = memoize { chromosome(2) { nodeGene().apply {
            onMutate {
                updateRule.let { if (it is BiasedUpdateRule) it.bias += Random().nextDouble() }
            }
        } } }
        val connections = memoize { chromosome { ArrayList<ConnectionGene5>() } }
        val sensors = memoize { chromosome { mutableListOf(frontSensor, backSensor) } }
//            val couplingManager: Memoize<WorkspaceBuilderCoupling> = memoize { TODO() }

        onMutate {
            nodes.current.genes.forEach { it.mutate() }
            connections.current.genes.forEach { it.mutate() }
            val source = nodes.current.genes.shuffled().first()
            val target = nodes.current.genes.shuffled().first()
            connections.current.genes.add(connectionGene(source, target))

            val thing = smellSensorGene {  }.also { sensors.current.genes.add(it) }
            val thing2 = nodeGene().also {
                it.onMutate {
                    updateRule.let { if (it is BiasedUpdateRule) it.bias + Random().nextDouble() }
                }
                nodes.current.genes.add(it)
            }

//                couplingManager.current {
//                    thing connects thing2
//                }

        }

        onBuild {
            +network {
                +inputs
                +nodes
                +connections
            }
        }

    }


    val a = thing.copy()
    a.mutate()
    val b = a.build()
    val c = a.copy()
    c.mutate()
    val d = c.build()

    println("end")

}
