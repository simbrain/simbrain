package org.simbrain.util.geneticalgorithms

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Helper functions to create genes
 */
fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene {
    return NodeGene(options)
}
inline fun connectionGene(source: NodeGene, target: NodeGene, options: Synapse.() -> Unit = { }): ConnectionGene {
    return ConnectionGene(Synapse(null, null as Neuron?).apply(options), source, target)
}
inline fun layoutGene(options: GridLayout.() -> Unit = { }): LayoutGene {
    val layout = GridLayout().apply(options)
    return LayoutGene(LayoutWrapper(layout, layout.hSpacing, layout.vSpacing))
}

/**
 * Subclasses are genes that express products that can be added to a [Network].
 */
sealed class NetworkGene<P: NetworkModel>: Gene<P>() {

    abstract fun buildWithContext(context: NetworkGeneticsContext): P

}

class NodeGene private constructor(private val template: Neuron = Neuron(null)): NetworkGene<Neuron>() {

    constructor(config: Neuron.() -> Unit): this(Neuron(null)) {
        template.apply(config)
    }

    override val product = CompletableFuture<Neuron>()

    private val copyListeners = LinkedList<(NodeGene) -> Unit>()

    fun onCopy(task: (NodeGene) -> Unit) {
        copyListeners.add(task)
    }

    fun mutate(config: Neuron.() -> Unit) {
        template.apply(config)
    }

    private fun fireCopied(newGene: NodeGene) {
        copyListeners.forEach { it(newGene) }
    }

    override fun copy(): NodeGene {
        val newGene = NodeGene(template.deepCopy())
        fireCopied(newGene)
        return newGene
    }

    override fun buildWithContext(context: NetworkGeneticsContext): Neuron = completeWith {
      Neuron(context.network, template)
    }

}

class ConnectionGene(private val template: Synapse, val source: NodeGene, val target: NodeGene) : NetworkGene<Synapse>() {

    override val product = CompletableFuture<Synapse>()

    private lateinit var sourceCopy: NodeGene
    private lateinit var targetCopy: NodeGene

    init {
        source.onCopy { sourceCopy = it }
        target.onCopy { targetCopy = it }
    }

    fun mutate(block: Synapse.() -> Unit) {
        template.apply(block)
    }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(template), sourceCopy, targetCopy)
    }

    override fun buildWithContext(context : NetworkGeneticsContext): Synapse {
        return Synapse(context.network, source.product.get(), target.product.get(), template.learningRule, template)
            .also {
            context.network.addNetworkModel(it)
            product.complete(it)
        }
    }

}

/**
 * Needed so we can evolve different types of layout.
 */
class LayoutWrapper(var layout: Layout, var hSpacing: Double, var vSpacing: Double): CopyableObject {

    override fun copy() = LayoutWrapper(layout.copy(), hSpacing, vSpacing)

}

class LayoutGene(private val template: LayoutWrapper) : Gene<Layout>(), TopLevelGene<Layout> {

    override val product = CompletableFuture<Layout>()

    // Todo: handle types of layout

    override fun copy(): LayoutGene {
        return LayoutGene(template.copy())
    }

    fun mutate(config: LayoutWrapper.() -> Unit) {
        template.apply(config)
    }

    override fun TopLevelBuilderContext.build(): Layout = completeWith {
        template.layout.apply {
            when(this) {
                is GridLayout -> {
                    hSpacing = template.hSpacing
                    vSpacing = template.vSpacing
                }
                is HexagonalGridLayout -> {
                    hSpacing = template.hSpacing
                    vSpacing = template.vSpacing
                }
                is LineLayout -> {
                    spacing = when (orientation) {
                        LineLayout.LineOrientation.VERTICAL -> template.vSpacing
                        LineLayout.LineOrientation.HORIZONTAL -> template.hSpacing
                    }
                }
            }
        }
    }

}

operator fun Network.invoke(block: NetworkGeneticsContext.() -> Unit) {
    NetworkGeneticsContext(this).apply(block)
}

class NetworkGeneticsContext(val network: Network) {

    fun <T, G: NetworkGene<T>> express(chromosome: Chromosome<T, G>): List<T> = chromosome.genes.map {
        it.buildWithContext(this).also { product ->
            if (product is Neuron) network.addNetworkModel(product)
        }
    }

    operator fun <T, G: NetworkGene<T>> Chromosome<T, G>.unaryPlus(): List<T> = express(this)

    fun Chromosome<Neuron, NodeGene>.asGroup(block: NeuronGroup.() -> Unit = { }) = fun(network: Network): NeuronGroup {
        return genes.map { it.buildWithContext(this@NetworkGeneticsContext) }
            .let { NeuronGroup(network, it).apply(block) }
            .also { network.addNetworkModel(it) }
    }

    operator fun <T> ((Network) -> T).unaryPlus(): T = this(network)

}
