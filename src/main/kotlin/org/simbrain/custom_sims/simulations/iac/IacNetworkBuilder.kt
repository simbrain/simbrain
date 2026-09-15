/**
 * A small DSL for hand-built interactive activation and competition (IAC) networks. A network is declared as
 * labelled pools whose nodes inhibit each other, plus instance nodes that excite (and are excited by) one node
 * in each property pool. The builder creates the neurons, neuron collections, and synapses, and lays each pool
 * out on a grid at a given location, so simulations only list pools and facts. Shared sidebar documentation for
 * the IAC simulations lives in [iacSidebarText].
 */
package org.simbrain.custom_sims.simulations.iac

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.IACRule
import org.simbrain.util.point
import java.awt.geom.Point2D
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A labelled pool of mutually inhibitory nodes. Index it by node label to reference a node when declaring instances.
 */
class IacPool internal constructor(val collection: NeuronCollection) {

    val label: String get() = collection.label.orEmpty()

    val neurons: List<Neuron> get() = collection.neuronList

    operator fun get(nodeLabel: String): Neuron = neurons.firstOrNull { it.label == nodeLabel }
        ?: error("Pool \"$label\" has no node labelled \"$nodeLabel\"")
}

class IacNetwork internal constructor(val network: Network, val pools: List<IacPool>) {

    val neurons: List<Neuron> get() = pools.flatMap { it.neurons }

    fun pool(label: String): IacPool = pools.firstOrNull { it.label == label } ?: error("No pool labelled \"$label\"")
}

/**
 * Declares an IAC network inside [network]. Weights and neuron parameters given here apply to every node and link
 * the block creates; individual links can still be overridden with [IacNetworkBuilder.connect].
 */
suspend fun Network.iacNetwork(
    excitatory: Double = 1.0,
    inhibitory: Double = -1.0,
    decay: Double = 0.05,
    rest: Double = 0.1,
    lowerBound: Double = -1.0,
    upperBound: Double = 1.0,
    block: suspend IacNetworkBuilder.() -> Unit
): IacNetwork {
    val builder = IacNetworkBuilder(this, excitatory, inhibitory, decay, rest, lowerBound, upperBound)
    builder.block()
    return builder.build()
}

class IacNetworkBuilder internal constructor(
    private val network: Network,
    private val excitatory: Double,
    private val inhibitory: Double,
    private val decay: Double,
    private val rest: Double,
    private val lowerBound: Double,
    private val upperBound: Double
) {

    private val pools = mutableListOf<IacPool>()
    private val synapses = mutableListOf<Synapse>()

    /**
     * Adds a pool of nodes with the given labels, all inhibiting each other, laid out on a grid centered at [at].
     */
    suspend fun pool(
        label: String,
        vararg nodes: String,
        at: Point2D,
        columns: Int = defaultColumns(nodes.size),
        hSpacing: Double = DEFAULT_H_SPACING,
        vSpacing: Double = DEFAULT_V_SPACING
    ): IacPool = addPool(label, nodes.map { newNeuron(it) }, at, columns, hSpacing, vSpacing)

    /**
     * Adds a pool of instance nodes. Each [InstanceScope.instance] call inside [block] creates one node that excites,
     * and is excited by, the property nodes it lists. Instance nodes inhibit each other like any other pool.
     */
    suspend fun instances(
        label: String,
        at: Point2D,
        columns: Int? = null,
        hSpacing: Double = DEFAULT_H_SPACING,
        vSpacing: Double = DEFAULT_V_SPACING,
        block: InstanceScope.() -> Unit
    ): IacPool {
        val scope = InstanceScope().apply(block)
        return addPool(label, scope.neurons, at, columns ?: defaultColumns(scope.neurons.size), hSpacing, vSpacing)
    }

    /**
     * Adds a symmetric link with an explicit weight, for the occasional fact that the pool structure cannot express.
     */
    fun connect(a: Neuron, b: Neuron, weight: Double) {
        synapses += Synapse(a, b).apply { strength = weight }
        synapses += Synapse(b, a).apply { strength = weight }
    }

    inner class InstanceScope {
        internal val neurons = mutableListOf<Neuron>()

        fun instance(label: String, vararg properties: Neuron): Neuron {
            val node = newNeuron(label)
            neurons += node
            properties.forEach { connect(node, it, excitatory) }
            return node
        }

        fun instance(vararg properties: Neuron): Neuron = instance("", *properties)
    }

    private fun newNeuron(label: String) = Neuron().apply {
        this.label = label
        updateRule = IACRule().apply {
            decay = this@IacNetworkBuilder.decay
            rest = this@IacNetworkBuilder.rest
            lowerBound = this@IacNetworkBuilder.lowerBound
            upperBound = this@IacNetworkBuilder.upperBound
        }
    }

    private suspend fun addPool(
        label: String,
        neurons: List<Neuron>,
        at: Point2D,
        columns: Int,
        hSpacing: Double,
        vSpacing: Double
    ): IacPool {
        network.addNetworkModels(neurons, usePlacementManager = false)
        val collection = NeuronCollection(neurons).apply {
            this.label = label
            layout = GridLayout(hSpacing, vSpacing, columns)
            applyLayout(point(0, 0))
            location = at
        }
        network.addNetworkModel(collection, usePlacementManager = false)
        for (source in neurons) {
            for (target in neurons) {
                if (source !== target) {
                    synapses += Synapse(source, target).apply { strength = inhibitory }
                }
            }
        }
        return IacPool(collection).also { pools += it }
    }

    internal suspend fun build(): IacNetwork {
        network.addNetworkModels(synapses)
        return IacNetwork(network, pools.toList())
    }

    private fun defaultColumns(size: Int) = ceil(sqrt(size.toDouble())).toInt().coerceAtLeast(1)

    companion object {
        const val DEFAULT_H_SPACING = 110.0
        const val DEFAULT_V_SPACING = 65.0
    }
}
