package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.activations
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable
import kotlin.math.exp

/**
 * Softmax. From wiki: "after applying softmax, each component will be in the interval (0,1),
 * and the components will add up to 1, so that they can be interpreted as probabilities"
 *
 * This applies the activation rules of the underlying nodes before normalizing so be sure to check that it is
 * appropriate, e.g. that any min and max value is appropriate.
 */
class SoftmaxGroup @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: SoftmaxParams = SoftmaxParams()
) : NeuronGroup() {

    var params by GuiEditable(
        label = "Softmax Parameters",
        description = "Parameters for the Softmax Group",
        initValue = params.apply { creationMode = false },
        order = 50
    )

    constructor(numNeurons: Int) : this(List(numNeurons) { Neuron() })

    @XStreamConstructor
    private constructor() : this(listOf())

    init {
        label = "Softmax"
        addNeurons(neurons.onEach { (it.updateRule as? LinearRule)?.clippingType = LinearRule.ClippingType.NoClipping })
    }

    context(Network)
    override fun update() {
        neuronList.forEach { it.accumulateInputs() }
        neuronList.forEach { it.update() }
        // These are often called "logits", that is, a set of unnormalized values
        val exponentials = neuronList.activations.map { exp(it / params.T) }
        val total = exponentials.sum()
        neuronList.forEachIndexed { i, n -> n.activation = (exponentials[i]) / total }
    }

    override fun copy() = SoftmaxGroup(neuronList.map { it.copy() }, params.copy())
}

@CustomTypeName("Softmax")
class SoftmaxParams : NeuronGroupParams() {

    @UserParameter(
        label = "Temperature",
        description = """Above 1 is a "hotter", more chaotic, and thus flatter distribution. 0 to 1 is a "cooler", more predictable, sharper distribution.""",
        minimumValue = 0.0,
        increment = .1,
        order = 10
    )
    var T = 1.0

    override fun create(): SoftmaxGroup {
        return SoftmaxGroup(List(numNeurons) {
            Neuron().apply {
                upperBound = 1.0
                lowerBound = 0.0
            }
        }, this)
    }

    override fun copy(): SoftmaxParams {
        return SoftmaxParams().also {
            commonCopy(it)
            it.T = T
        }
    }
}