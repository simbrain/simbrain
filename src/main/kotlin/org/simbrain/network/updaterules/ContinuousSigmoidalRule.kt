package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **Continuous Sigmoidal Rule** provides various squashing function
 * ouputs for a neuron whose activation is numerically integrated continuously
 * over time.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class ContinuousSigmoidalRule() : AbstractSigmoidalRule() {

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
     * The net value of this neuron. This is the value that is integrated over
     * time and then passed to the squashing function. NOTE: the net inputs are
     * integrated and that value is passed through a squashing function to give
     * the neurons activation. The activation post-squashing is NOT what is
     * being numerically integrated.
     */
    private var netActivation = 0.0

    private var inputTerm = 0.0

    override fun copy(): ContinuousSigmoidalRule {
        var sn = ContinuousSigmoidalRule()
        sn = (super.copy(sn) as ContinuousSigmoidalRule)
        sn.timeConstant = timeConstant
        sn.leakConstant = leakConstant
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
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        val dt: Double = timeStep

        inputTerm = if (addNoise) {
            dt / timeConstant * (neuron.input + noiseGenerator.sampleDouble())
        } else {
            dt / timeConstant * (neuron.input)
        }

        netActivation = netActivation * (1 - (leakConstant * dt / timeConstant)) + inputTerm

        neuron.activation = type.valueOf(netActivation, upperBound, lowerBound, this.slope)
    }


    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override fun getDerivative(value: Double): Double {
        val up = upperBound
        val lw = lowerBound
        val diff = up - lw
        return type.derivVal(value, up, lw, diff)
    }

    override fun clear(neuron: Neuron) {
        super.clear(neuron)
        netActivation = 0.0
    }

    override val name: String
        get() = "Sigmoidal (Continuous)"

}