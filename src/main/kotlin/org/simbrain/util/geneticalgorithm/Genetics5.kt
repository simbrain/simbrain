package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


abstract class Gene5<T> protected constructor(val template: T) : CopyableObject

class NodeGene5 (template: Neuron) : Gene5<Neuron>(template) {

    private val copyListeners = LinkedList<(NodeGene5) -> Unit>()

    fun onCopy(task: (NodeGene5) -> Unit) {
        copyListeners.add(task)
    }

    override fun copy(): NodeGene5 {
        val newGene = NodeGene5(template.deepCopy())
        copyListeners.forEach { it(newGene) }
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

class PeripheralGene5<T: PeripheralAttribute> (template: T): Gene5<T>(template) {

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

fun <T, G : Gene5<T>> chromosome(count: Int, genes: (index: Int) -> G): Chromosome5<T, G> {
    return Chromosome5(List(count) { genes(it) }.toMutableList())
}

fun <T, G : Gene5<T>> chromosome(vararg genes: G): Chromosome5<T, G> {
    return Chromosome5(mutableListOf(*genes))
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

class Agent5(val network: Network, val odorWorldEntity: OdorWorldEntity)

class GeneProductMap(private val map: HashMap<Gene5<*>, Any> = HashMap()) {

    operator fun <T, G: Gene5<T>> get(gene: G) = map[gene] as T?

    operator fun <T, G: Gene5<T>> set(gene: G, product: T) {
        map[gene] = product as Any
    }

}

class EvaluationContext(val workspace: Workspace, val mapping: GeneProductMap) {

    val <T, G: Gene5<T>, C: Chromosome5<T, G>> Memoize<C>.products: List<T> get() {
        return current.genes.map { mapping[it]!! }
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
        return Environment5(EvaluationContext(workspace, builder.geneProductMapping), evalFunction)
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

    val geneProductMapping = GeneProductMap()

    fun network(builder: NetworkAgentBuilder.() -> Unit): NetworkAgentBuilder {
        return NetworkAgentBuilder().apply(builder)
    }

    fun odorworld() {

    }

    inner class NetworkAgentBuilder {

        val tasks = LinkedList<(Network) -> Unit>()

        private fun <C: Chromosome5<T, G>, G: Gene5<T>, T> Memoize<C>.addGene(adder: (gene: G, net: Network) -> T) {
            tasks.add { net ->
                current.genes.forEach {
                    geneProductMapping[it] = adder(it, net)
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

        private fun <T, G: Gene5<T>, C: Chromosome5<T, G>> Memoize<C>.option(
                options: List<T>.() -> Unit,
                adder: (gene: G, net: Network) -> T
        ): (Network) -> Unit {
            return { net ->
                current.genes.map { gene ->
                    adder(gene, net).also { geneProductMapping[gene] = it }
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
                    gene.build(net).also { geneProductMapping[gene] = it }
                }.let { net.addNeuronGroup(NeuronGroup(net, it).apply(options)) }
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