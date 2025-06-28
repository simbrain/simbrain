package org.simbrain.network.neurongroups

import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.sortLeftRightTopBottom
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.CustomTypeName

/**
 * A group of neurons using a common [NeuronUpdateRule]. After creation the update rule may be changed but
 * neurons should not be added. Intermediate between a [NeuronCollection] which is just an
 * assemblage of potentially heterogeneous neurons that can be treated as a group, and a
 * [org.simbrain.network.matrix.NeuronArray] which is an array that can be updated using static update methods.
 *
 * A primary abstraction for larger network structures. Layers in feed-forward networks are neuron
 * groups. Self-organizing-maps subclass this class. Etc. Since all update rules are the same groups can be characterized
 * as spiking vs. non-spiking.
 */
open class NeuronGroup() : AbstractNeuronCollection() {

    constructor(neurons: List<Neuron>) : this() {
        addNeurons(neurons.sortLeftRightTopBottom())
        applyLayout()
    }

    constructor(numNeurons: Int) : this(List(numNeurons) { Neuron() })

    override var updateRule: NeuronUpdateRule<*, *>
        get() = neuronList.first().updateRule
        set(value) {
            neuronList.forEach { it.updateRule = value.copy() }
        }

    override suspend fun delete(): List<NetworkModel> {
        // val deletedNeurons = neuronList.toList().flatMap { it.delete() }
        return buildList {
            addAll(super.delete())
            addAll(neuronList.toList().flatMap { it.delete() })
        }
    }

    context(Network)
    override fun update() {
        neuronList.forEach { it.accumulateInputs() }
        neuronList.forEach { it.update() }
        neuronList.forEach { it.clearInput() }
        super.update()
    }

    override fun clear() {
        super.clear()
        neuronList.forEach { it.clear() }
    }

    override fun copy() = NeuronGroup().also {
        it.addNeurons(neuronList.map(Neuron::copy))
        it.commonCopyFrom(this)
    }
}

@CustomTypeName("Bare Neuron Group")
class BasicNeuronGroupParams: NeuronGroupParams() {

    override fun create(): NeuronGroup {
        return NeuronGroup(List(numNeurons) { Neuron() })
    }

    override fun copy(): CopyableObject {
        return BasicNeuronGroupParams().also {
            commonCopy(it)
        }
    }
}