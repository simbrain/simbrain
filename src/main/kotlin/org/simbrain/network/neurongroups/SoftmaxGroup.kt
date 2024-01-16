package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.math.exp

/**
 * Softmax. From wiki: "after applying softmax, each component will be in the interval (0,1),
 * and the components will add up to 1, so that they can be interpreted as probabilities"
 *
 * This applies the activation rules of the underlying nodes before normalizing so be sure to check that it is
 * appropriate, e.g. that any min and max value is appropriate.
 */
class SoftmaxGroup(net: Network, neurons: List<Neuron>): NeuronGroup(net), CopyableObject {

    constructor(net: Network, numNeurons: Int): this(net, List(numNeurons) { Neuron(net) })

    @XStreamConstructor
    private constructor(parentNetwork: Network): this(parentNetwork, listOf())

    init {
        label = "Softmax"
        addNeurons(neurons)
    }

    override fun update() {
        neuronList.forEach { it.updateInputs() }
        neuronList.forEach { it.update() }
        val exponentials = neuronList.activations.map { exp(it) }
        val total = exponentials.sum()
        neuronList.forEachIndexed { i, n -> n.activation = exponentials[i]/total }
    }

    override fun copy() = SoftmaxGroup(network, neuronList.map { it.deepCopy() })
}

class SoftmaxParams: NeuronGroupParams() {

    override fun create(net: Network): SoftmaxGroup {
        return SoftmaxGroup(net, List(numNeurons) { Neuron(net) })
    }

    override fun copy(): CopyableObject {
        return SoftmaxParams().also {
            commonCopy(it)
        }
    }
}