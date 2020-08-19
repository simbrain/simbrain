package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
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

    fun onMutation(options: T.() -> Unit) {
        mutationTasks.add { apply(options) }
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

data class PeripheralGeneType<T: PeripheralAttribute>(val neuron: Neuron, val peripheral: T)

class PeripheralGene5<T: PeripheralAttribute> private constructor(
        template: PeripheralGeneType<T>,
        mutationTasks: MutableList<PeripheralGeneType<T>.() -> Unit>
): Gene5<PeripheralGeneType<T>>(template, mutationTasks) {

    constructor(template: PeripheralGeneType<T>) : this(template, mutableListOf())

    val neuron get() = template.neuron
    val peripheral get() = template.peripheral

    override fun copy(): PeripheralGene5<T> {
        val newNeuron = neuron.deepCopy()!! // TODO: custom copy
        val newPeripheralAttribute = peripheral.copy()!! as T
        val newData = PeripheralGeneType(newNeuron, newPeripheralAttribute)
        return PeripheralGene5(newData)
    }

//    fun build(network: Network, odorWorldEntity: OdorWorldEntity): PeripheralGeneType<T> {
//        return PeripheralGeneType(Neuron(network, neuron), )
//    }

}

fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene5 {
    return NodeGene5(Neuron(null).apply(options))
}

fun connectionGene(source: NodeGene5, target: NodeGene5, options: Synapse.() -> Unit = { }): ConnectionGene5 {
    return ConnectionGene5(Synapse(null).apply(options), source, target)
}

fun smellSensorGene(options: PeripheralGeneType<SmellSensor>.() -> Unit): PeripheralGene5<SmellSensor> {
    return PeripheralGene5(PeripheralGeneType(Neuron(null), SmellSensor(OdorWorldEntity(null))))
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

class Memoize<T>(val current: T)

class GenomeBuilder(private val refList: MutableList<Memoize<out CopyableObject>>) {

    var isInitial = true

    var refIterator = refList.iterator()

    val mutationTasks = mutableListOf<() -> Unit>()

    val chromosomes = mutableListOf<Chromosome5<*, *>>()

    fun <T: CopyableObject> memoize(initializeValue: () -> T): Memoize<T> {
        return if (isInitial) {
            Memoize(initializeValue()).also { refList.add(it) }
        } else {
            refIterator.next() as Memoize<T>
        }
    }

    fun copy(): GenomeBuilder {
        return GenomeBuilder(refList.map { Memoize(it.current.copy()) }.toMutableList())
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

    fun onBuild(builder: () -> Unit) {

    }

}

fun genomeBuilder(builder: GenomeBuilder.() -> Unit): GenomeBuilder {
    return TODO()
}

class Agent5(val network: Network, val odorWorldEntity: OdorWorldEntity)

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
                }
            }
        }
        return this
    }
}

class WorkspaceBuilder {

    val workspace by lazy { Workspace() }

    fun network(builder: NetworkAgentBuilder.() -> Unit): NetworkAgentBuilder {
        return TODO()
    }

    fun odorworld() {

    }

    operator fun NetworkAgentBuilder.unaryPlus() {
        workspace.addWorkspaceComponent(NetworkComponent("TempNet", Network().also { network ->
            tasks.forEach { task -> task(network) }
        }))
    }

}

fun workspace(builder: WorkspaceBuilder.() -> Unit) {

}

fun main() {
    workspace {

        val genome = genomeBuilder {

            val frontSensor by lazy {
                smellSensorGene {
                    peripheral.theta = 0.0
                    peripheral.radius = 24.0
                }
            }

            val backSensor by lazy {
                smellSensorGene {
                    peripheral.theta = 3.14159
                    peripheral.radius = 24.0
                }
            }

            val nodes = memoize { chromosome(2) { nodeGene() } }
            val connections = memoize { chromosome { ArrayList<ConnectionGene5>() } }
            val sensors = memoize { chromosome { mutableListOf(frontSensor, backSensor) } }

            onMutate {
                nodes.current.genes.forEach { it.mutate() }
                connections.current.genes.forEach { it.mutate() }
                val source = nodes.current.genes.shuffled().first()
                val target = nodes.current.genes.shuffled().first()
                connections.current.genes.add(connectionGene(source, target))
            }

            onBuild {
                +network {
                    +nodes
                    +connections
                }
            }

        }


    }
}
