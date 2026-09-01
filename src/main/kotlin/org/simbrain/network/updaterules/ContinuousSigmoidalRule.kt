/**
 * Leaky-integrator sigmoidal rule. The integrated net activation lives in [ContinuousSigmoidalData] so it
 * can be inspected, serialized with the neuron, and exposed as a membrane potential to electrical
 * connections such as gap junctions.
 */
package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.MembranePotentialProvider
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * **Continuous Sigmoidal Rule** provides various squashing function
 * ouputs for a neuron whose activation is numerically integrated continuously
 * over time.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class ContinuousSigmoidalRule() : AbstractSigmoidalRule<ContinuousSigmoidalData, EmptyMatrixData>(),
    MembranePotentialProvider {

    /**
     * The **time constant** of these neurons. If **timeConstant *
     * leakConstant == network time-step** (or vice versa), behavior is
     * equivalent to discrete sigmoid. The larger the time constant relative to
     * the time-step, the more slowly inputs will be integrated.
     */
    @UserParameter(
        label = "Time constant",
        description = "The time constant controls how quickly the numerical integration occurs.",
        increment = .1,
        order = 1
    )
    var timeConstant: Double = 10.0

    /**
     * The leak constant: how strongly the neuron will be attracted to its base
     * activation. If **timeConstant * leakConstant == network time-step**
     * (or vice versa), behavior is equivalent to discrete sigmoid.
     */
    @UserParameter(label = "Leak constant", description = "An option to add noise.", increment = .1, order = 2)
    var leakConstant: Double = 1.0

    /**
     * Shifts the squashing function without entering the integrator, so the output is
     * squash(netActivation + outputBias). Unlike the neuron's bias, which is added to the input and
     * integrated through the leaky dynamics, this bias takes effect immediately.
     */
    var outputBias by GuiEditable(
        initValue = 0.0,
        label = "Output bias",
        description = "Added to the integrated net activation just before the squashing function. " +
            "Unlike the neuron bias, it is not integrated and shifts the output immediately.",
        increment = .1,
        order = 3
    )

    override fun copy(): ContinuousSigmoidalRule {
        var sn = ContinuousSigmoidalRule()
        sn = (super.copy(sn) as ContinuousSigmoidalRule)
        sn.timeConstant = timeConstant
        sn.leakConstant = leakConstant
        sn.outputBias = outputBias
        return sn
    }

    /**
     * Where x_i(t) is the net activation of neuron i at time t, r(t) is the
     * output activation after being put through a sigmoid squashing function at
     * time t, a is the leak constant, and c is the time constant:
     *
     * c * dx_i/dt = -ax_i(t) + sum(w_ij * r_j(t)
     *
     * Discretizing using euler integration:
     *
     * x_i(t + dt) = x_i(t) - (ax_i(t) * dt/c) + (dt/c)*sum(w_ij * r_j(t))
     *
     * Factorting out x_i(t)
     *
     * x_i(t + dt) = x_i(t) * (1 - a*dt/c) + (dt/c) * sum(w_ij * r_j(t))
     */
    context(Network)
    override fun apply(neuron: Neuron, data: ContinuousSigmoidalData) {
        val dt: Double = timeStep

        val inputTerm = if (addNoise) {
            dt / timeConstant * (neuron.input + noiseGenerator.sampleDouble())
        } else {
            dt / timeConstant * (neuron.input)
        }

        data.netActivation = data.netActivation * (1 - (leakConstant * dt / timeConstant)) + inputTerm

        neuron.activation = type.valueOf(data.netActivation + outputBias, upperBound, lowerBound, this.slope)
    }

    override fun createScalarData(): ContinuousSigmoidalData {
        return ContinuousSigmoidalData()
    }

    override fun membranePotential(neuron: Neuron): Double {
        return (neuron.dataHolder as? ContinuousSigmoidalData)?.netActivation ?: 0.0
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override fun getDerivative(value: Double): Double {
        val up = upperBound
        val lw = lowerBound
        val diff = up - lw
        return type.derivVal(value, up, lw, diff)
    }

    override val name: String
        get() = "Sigmoidal (Continuous)"

}

class ContinuousSigmoidalData(
    @UserParameter(
        label = "Net activation",
        description = "The integrated net input that is passed through the squashing function."
    )
    var netActivation: Double = 0.0
) : ScalarDataHolder {

    override fun copy(): ContinuousSigmoidalData {
        return ContinuousSigmoidalData(netActivation)
    }

    override fun clear() {
        netActivation = 0.0
    }
}
