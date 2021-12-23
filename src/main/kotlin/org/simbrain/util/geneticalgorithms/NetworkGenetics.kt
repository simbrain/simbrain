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
import org.simbrain.util.Event
import org.simbrain.util.propertyeditor.CopyableObject
import java.beans.PropertyChangeSupport
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Subclasses are genes that express products that can be added to a [Network].
 */
sealed class NetworkGene<P: NetworkModel, G: NetworkGene<P, G>>: Gene<P, G>() {
    abstract fun buildWithContext(context: NetworkGeneticsContext): P
}

/**
 * Create a node gene in a chromosome using a template.
 */
class NodeGene private constructor(override val chromosome: Chromosome<Neuron, NodeGene>, val template: Neuron = Neuron(null)): NetworkGene<Neuron, NodeGene>() {

    constructor(chromosome: Chromosome<Neuron, NodeGene>, config: Neuron.() -> Unit): this(chromosome, Neuron(null)) {
        template.apply(config)
    }

    val fanOut = LinkedHashSet<ConnectionGene>()
    val fanIn = LinkedHashSet<ConnectionGene>()

    override val product = CompletableFuture<Neuron>()

    val events = NodeGeneEvents(this)

    fun mutate(config: Neuron.() -> Unit) {
        template.apply(config)
    }

    override fun delete() {
        super.delete()
        val toDelete = fanIn + fanOut
        toDelete.forEach { it.delete() }
    }

    override fun copy(chromosome: Chromosome<Neuron, NodeGene>): NodeGene {
        val newGene = NodeGene(chromosome, template.deepCopy())
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


class ConnectionGene(override val chromosome: Chromosome<Synapse, ConnectionGene>, private val template: Synapse, val source: NodeGene, val target: NodeGene) : NetworkGene<Synapse, ConnectionGene>() {

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

    override fun delete() {
        super.delete()
        source.fanOut.remove(this)
        target.fanOut.remove(this)
    }

    override fun copy(chromosome: Chromosome<Synapse, ConnectionGene>): ConnectionGene {
        return ConnectionGene(chromosome, Synapse(template), sourceCopy, targetCopy)
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

class LayoutGene(override val chromosome: Chromosome<Layout, LayoutGene>, private val template: LayoutWrapper) : Gene<Layout, LayoutGene>(), TopLevelGene<Layout> {

    override val product = CompletableFuture<Layout>()

    // Todo: handle types of layout

    override fun copy(chromosome: Chromosome<Layout, LayoutGene>): LayoutGene {
        return LayoutGene(chromosome, template.copy())
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

    fun <P, G: NetworkGene<P, G>> express(chromosome: Chromosome<P, G>): List<P> = chromosome.genes.map {
        it.buildWithContext(this).also { product ->
            if (product is Neuron) network.addNetworkModel(product)
        }
    }

    operator fun <P, G: NetworkGene<P, G>> Chromosome<P, G>.unaryPlus(): List<P> = express(this)

    fun Chromosome<Neuron, NodeGene>.asGroup(block: NeuronGroup.() -> Unit = { }) = fun(network: Network): NeuronGroup {
        return genes.map { it.buildWithContext(this@NetworkGeneticsContext) }
            .let { NeuronGroup(network, it).apply(block) }
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
fun Chromosome<Neuron, NodeGene>.nodeGene(options: Neuron.() -> Unit = { }): NodeGene {
    return NodeGene(this, options)
}
inline fun Chromosome<Synapse, ConnectionGene>.connectionGene(source: NodeGene, target: NodeGene, options: Synapse.() -> Unit = { }): ConnectionGene {
    return ConnectionGene(this, Synapse(null, null as Neuron?).apply(options), source, target)
}
inline fun Chromosome<Layout, LayoutGene>.layoutGene(options: GridLayout.() -> Unit = { }): LayoutGene {
    val layout = GridLayout().apply(options)
    return LayoutGene(this, LayoutWrapper(layout, layout.hSpacing, layout.vSpacing))
}
