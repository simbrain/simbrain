package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData

/**
 * **RunningAverageNeuron** keeps a running average of current and past
 * activity.
 *
 *
 * TODO: Currently explodes. Fix and improve. See
 * http://en.wikipedia.org/wiki/Moving_average
 */
class RunningAverageRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    /**
     * @return Rate constant.
     */
    /**
     * @param rateConstant Parameter to be set.
     */
    /**
     * Rate constant variable.
     */
    var rateConstant: Double = .5

    /**
     * Last activation.
     */
    private var `val` = 0.0

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    fun init(neuron: Neuron?) {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    override fun deepCopy(): RunningAverageRule {
        val cn = RunningAverageRule()
        cn.rateConstant = rateConstant
        return cn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        // "val" on right is activation at last time step
        `val` = rateConstant * neuron.input + (1 - rateConstant) * `val`
        neuron.activation = `val`
    }

    override val name: String = "Running average"
}