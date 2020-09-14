package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.sse
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

class GeneProductMap(private val map: HashMap<Gene5<*>, Any> = HashMap()) {

    operator fun <T, G: Gene5<T>> get(gene: G) = map[gene] as T?

    operator fun <T, G: Gene5<T>> set(gene: G, product: T) {
        map[gene] = product as Any
    }

}

class Evaluator(val workspace: Workspace, val mapping: GeneProductMap) {

    val <T, G: Gene5<T>> Chromosome5<T, G>.products: List<T> get() {
        return genes.map { mapping[it]!! }
    }

}

class Environment5(private val evaluator: Evaluator, private val evalFunction: Evaluator.() -> Double) {

    fun eval() = evaluator.evalFunction()

}

class EnvironmentBuilder private constructor(refList: MutableList<Memoize<*>>, private val template: EnvironmentBuilder.() -> Unit):
        Memoization(refList)  {

    constructor(builder: EnvironmentBuilder.() -> Unit): this(mutableListOf(), builder)

    val mutationTasks = mutableListOf<() -> Unit>()

    lateinit var evalFunction: Evaluator.() -> Double

    lateinit var builder: WorkspaceBuilder

    fun copy(): EnvironmentBuilder {
        return EnvironmentBuilder(refList, template).apply(template)
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

    fun onBuild(template: WorkspaceBuilder.() -> Unit) {
        builder = WorkspaceBuilder().apply(template)
    }

    fun build(): Environment5 {
        val workspace = Workspace()
        builder.builders.forEach { it(workspace) }
        return Environment5(Evaluator(workspace, builder.geneProductMapping), evalFunction)
    }

    fun onEval(eval: Evaluator.() -> Double) {
        evalFunction = eval
    }

}

fun environmentBuilder(builder: EnvironmentBuilder.() -> Unit) = EnvironmentBuilder(builder).apply(builder)

class WorkspaceBuilder {

    val builders = ArrayList<(Workspace) -> Unit>()

    fun network(builder: NetworkAgentBuilder.() -> Unit): NetworkAgentBuilder {
        return NetworkAgentBuilder().apply(builder)
    }

    fun odorworld() {

    }

    val tasks = LinkedList<(Network) -> Unit>()

    val geneProductMapping = GeneProductMap()

    private fun <C: Chromosome5<T, G>, G: Gene5<T>, T> Memoize<C>.addGene(adder: (gene: G, net: Network) -> T) {
        tasks.add { net ->
            current.genes.forEach {
                geneProductMapping[it] = adder(it, net)
            }
        }
    }

    operator fun NetworkAgentBuilder.unaryPlus() {
        builders.add { workspace: Workspace ->
            workspace.addWorkspaceComponent(NetworkComponent("TempNet", Network().also { network ->
                tasks.forEach { task -> task(network) }
            }))
        }
    }

    inner class NetworkAgentBuilder {

        @JvmName("unaryPlusNeuron")
        operator fun <C: Chromosome5<Neuron, NodeGene5>> Memoize<C>.unaryPlus() {
            addGene { gene, net ->
                gene.build(net).also { neuron ->
                    net.addLooseNeuron(neuron)
                }
            }
        }

        @JvmName("unaryPlusSynapse")
        operator fun <C: Chromosome5<Synapse, ConnectionGene5>> Memoize<C>.unaryPlus() {
            addGene { gene, net ->
                gene.build(net, geneProductMapping[gene.source]!!, geneProductMapping[gene.target]!!)
                        .also { synapse -> net.addLooseSynapse(synapse) }
            }
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
    val thing = environmentBuilder {

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

        onEval {
            inputs.current.products.activations = listOf(1.0, 0.0)
            inputs.current.products.forEach { it.isClamped = true }
            repeat(20) {
                workspace.simpleIterate()
            }

            nodes.current.products.activations sse listOf(1.0, 0.0)
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
    println(b.eval())
    val c = a.copy()
    c.mutate()
    val d = c.build()
    println(d.eval())
    println("end")

}
