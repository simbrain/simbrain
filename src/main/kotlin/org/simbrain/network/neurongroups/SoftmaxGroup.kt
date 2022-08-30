package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronGroup

/**
 * Softmax. From wiki: "after applying softmax, each component will be in the interval (0,1),
 * and the components will add up to 1, so that they can be interpreted as probabilities"
 *
 * This applies the activation rules of the underlying nodes before normalizing so be sure to check that it is
 * appropriate, e.g. that any min and max value is appropriate.
 */
class SoftmaxGroup(net: Network, val numNeurons: Int): NeuronGroup(net, numNeurons) {

    init {
        label = "Softmax"
    }

    override fun update() {
        super.update()
        val exponentials = neuronList
            .activations.map{a -> Math.exp(a)}
        val total = exponentials.sum()
        neuronList.forEachIndexed { i, n -> n.activation = exponentials[i]/total }
    }

    override fun deepCopy(newParent: Network): NeuronGroup {
        return SoftmaxGroup(newParent, this.numNeurons)
    }

    override fun getTypeDescription(): String? {
        return "Softmax"
    }

}