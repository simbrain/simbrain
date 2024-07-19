package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.activations
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.CustomTypeName
import kotlin.math.exp

/**
 * Softmax. From wiki: "after applying softmax, each component will be in the interval (0,1),
 * and the components will add up to 1, so that they can be interpreted as probabilities"
 *
 * This applies the activation rules of the underlying nodes before normalizing so be sure to check that it is
 * appropriate, e.g. that any min and max value is appropriate.
 */
class SoftmaxGroup(neurons: List<Neuron>): NeuronGroup(), CopyableObject {

    constructor(numNeurons: Int): this(List(numNeurons) { Neuron() })

    @XStreamConstructor
    private constructor() : this(listOf())

    init {
        label = "Softmax"
        addNeurons(neurons)
    }

    context(Network)
    override fun update() {
        neuronList.forEach { it.accumulateInputs() }
        neuronList.forEach { it.update() }
        val exponentials = neuronList.activations.map { exp(it) }
        val total = exponentials.sum()
        neuronList.forEachIndexed { i, n -> n.activation = exponentials[i]/total }
    }

    override fun copy() = SoftmaxGroup(neuronList.map { it.copy() })
}

@CustomTypeName("Softmax")
class SoftmaxParams: NeuronGroupParams() {

    override fun create(): SoftmaxGroup {
        return SoftmaxGroup(List(numNeurons) { Neuron() })
    }

    override fun copy(): CopyableObject {
        return SoftmaxParams().also {
            commonCopy(it)
        }
    }
}