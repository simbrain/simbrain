package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.activations
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Normalize activations on each input. Simply divides by total input.
 *
 */
class NormalizationGroup @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: NormalizationGroupParams = NormalizationGroupParams()
) : NeuronGroup() {

    // TODO: Choice about what type of normalization to use, e.g. min-max or L2 norm

    var params by GuiEditable(
        label = "Normalization Parameters",
        description = "Parameters for the Normalization Group",
        initValue = params.apply { creationMode = false },
        order = 50
    )

    constructor(numNeurons: Int,  params: NormalizationGroupParams = NormalizationGroupParams())
            : this(List(numNeurons) { Neuron() }, params)

    @XStreamConstructor
    private constructor() : this(listOf())

    init {
        label = "Normalization"
        addNeurons(neurons.onEach { (it.updateRule as? LinearRule)?.clippingType = LinearRule.ClippingType.NoClipping })
    }

    context(Network)
    override fun update() {
        neuronList.forEach { it.accumulateInputs() }
        neuronList.forEach { it.update() }
        val total = neuronList.activations.sum()
        if (total != 0.0) {
            neuronList.forEach{ it.activation /= total }
        } else {
            neuronList.forEach{ it.activation = 1.0/neuronList.count() }
        }
    }

    override fun copy() = NormalizationGroup(neuronList.map { it.copy() }, params.copy())

    @CustomTypeName("Normalization")
    class NormalizationGroupParams : NeuronGroupParams() {

        override fun create(): NormalizationGroup {
            return NormalizationGroup(numNeurons, this)
        }

        override fun copy(): NormalizationGroupParams {
            return NormalizationGroupParams().also {
                commonCopy(it)
            }
        }
    }
}

