package org.simbrain.util.geneticalgorithms

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.Event
import org.simbrain.util.propertyeditor.CopyableObject
import java.beans.PropertyChangeSupport
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Subclasses are genes that express products that can be added to a [Network].
 */
sealed class NetworkGene<P: NetworkModel>: Gene<P>() {
    abstract fun buildWithContext(context: NetworkGeneticsContext): P
}

/**
 * Create a node gene in a chromosome using a template.
 */
class NodeGene private constructor(val template: Neuron = Neuron(null)): NetworkGene<Neuron>() {

    constructor(config: Neuron.() -> Unit): this(Neuron(null)) {
        template.apply(config)
    }

    val fanOut = LinkedHashSet<ConnectionGene>()
    val fanIn = LinkedHashSet<ConnectionGene>()

    override val product = CompletableFuture<Neuron>()

    val events = NodeGeneEvents(this)

    fun mutate(config: Neuron.() -> Unit) {
        template.apply(config)
    }

    override fun copy(): NodeGene {
        val newGene = NodeGene(template.deepCopy())
        events.fireCopied(newGene)
        return newGene
    }

    override fun buildWithContext(context: NetworkGeneticsContext): Neuron = completeWith {
      Neuron(context.network, template)
    }

}

class NodeGeneEvents(nodeGene: NodeGene) : Event(PropertyChangeSupport(nodeGene)) {
    fun onCopy(handler: Consumer<NodeGene>) = "Copy".itemAddedEvent(handler)
    fun fireCopied(copied: NodeGene) = "Copy"(new = copied)
}


class ConnectionGene(private val template: Synapse, val source: NodeGene, val target: NodeGene) : NetworkGene<Synapse>() {

    override val product = CompletableFuture<Synapse>()

    private lateinit var sourceCopy: NodeGene
    private lateinit var targetCopy: NodeGene

    init {
        source.fanOut.add(this)
        target.fanIn.add(this)
        source.events.onCopy { sourceCopy = it }
        target.events.onCopy { targetCopy = it }
    }

    fun mutate(block: Synapse.() -> Unit) {
        template.apply(block)
    }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(template), sourceCopy, targetCopy)
    }

    override fun buildWithContext(context: NetworkGeneticsContext): Synapse {
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

/**
 * Provides DSL helpers for network genetics.
 */
class NetworkGeneticsContext(val network: Network) {

    fun <P, G: NetworkGene<P>> express(chromosome: Chromosome<P, G>): List<P> = chromosome.map {
        it.buildWithContext(this).also { product ->
            if (product is Neuron) network.addNetworkModel(product)
        }
    }

    operator fun <P, G: NetworkGene<P>> Chromosome<P, G>.unaryPlus(): List<P> = express(this)

    fun Chromosome<Neuron, NodeGene>.asGroup(block: NeuronGroup.() -> Unit = { }) = fun(network: Network): NeuronGroup {
        return map { it.buildWithContext(this@NetworkGeneticsContext) }
            .let { NeuronGroup(network, it).apply(block) }
            .also { network.addNetworkModel(it) }
    }

    fun Chromosome<Neuron, NodeGene>.asNeuronCollection(block: NeuronCollection.() -> Unit = { }) = fun(network: Network):
            NeuronCollection {
        return map { it.buildWithContext(this@NetworkGeneticsContext) }
            .let {
                it.forEach(network::addNetworkModel)
                NeuronCollection(network, it).apply(block) }
            .also { network.addNetworkModel(it) }
    }

    operator fun <T> ((Network) -> T).unaryPlus(): T = this(network)

}

operator fun Network.invoke(block: NetworkGeneticsContext.() -> Unit) {
    NetworkGeneticsContext(this).apply(block)
}


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
