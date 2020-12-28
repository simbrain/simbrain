package org.simbrain.network.util

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkUpdateAction
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.component1
import org.simbrain.util.component2
import java.awt.geom.Point2D

var List<Neuron?>.activations
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            if (neuron.isClamped) {
                neuron.forceSetActivation(value)
            } else {
                neuron.activation = value
            }
        }
    }

var List<Neuron?>.labels
    get() = map { it?.label ?: "" }
    set(values) = values.forEachIndexed { index, label ->
        this[index]?.let { it.label = label }
    }

var List<Neuron>.auxValues
    get() = map { it.auxValue }
    set(values) = values.forEachIndexed { index, value ->
        this[index].auxValue = value
    }

fun Network.addNeuron(block: Neuron.() -> Unit = { }) = Neuron(this)
    .apply(block)
    .also(this::addLooseNeuron)

fun Network.addSynapse(source: Neuron, target: Neuron, block: Synapse.() -> Unit = { }) = Synapse(source, target)
    .apply(block)
    .also(this::addLooseSynapse)

fun Network.addNeuronGroup(count: Int, template: Neuron.() -> Unit = { }) = NeuronGroup(this, List(count) {
    Neuron(this).apply(template)
}).also { addNeuronGroup(it) }

fun Network.addNeuronGroup(count: Int, location: Point2D? = null, template: Neuron.() -> Unit = { }): NeuronGroup {
    return NeuronGroup(this, List(count) {
        Neuron(this).apply(template)
    }).also {
        addNeuronGroup(it)
        if (location != null) {
            val (x, y) = location
            it.setLocation(x, y)
        }
    }
}

fun Network.connectAllToAll(source: NeuronGroup, target: NeuronGroup): List<Synapse> {
    return AllToAll().connectAllToAll(source.neuronList, target.neuronList)
}

fun networkUpdateAction(description: String, longDescription: String = description, action: () -> Unit)
    = object : NetworkUpdateAction {
        override fun invoke() = action()
        override fun getDescription(): String = description
        override fun getLongDescription(): String = longDescription
    }

fun createNeuronGroupTemplate(template: NeuronGroup.() -> Unit) = fun Network.(
    count: Int,
    template: Neuron.() -> Unit
) = NeuronGroup(this, List(count) {
    Neuron(this).apply(template)
}).also { addNeuronGroup(it) }


fun <R> Network.withConnectionStrategy(
    connectionStrategy: ConnectionStrategy,
    block: NetworkWithConnectionStrategy.() -> R
): R {
    return NetworkWithConnectionStrategy(this, connectionStrategy).run(block)
}

data class NetworkWithConnectionStrategy(private val network: Network, private val connectionStrategy: ConnectionStrategy) {
    fun connect(source: List<Neuron>, target: List<Neuron>): List<Synapse> {
        return connectionStrategy.connectNeurons(network, source, target)
    }
}