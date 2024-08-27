package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SigmoidFunctionEnum

/**
 * **Continuous Sigmoidal Rule** provides various squashing function
 * ouputs for a neuron whose activation is numerically integrated continuously
 * over time.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class ContinuousSigmoidalRule : AbstractSigmoidalRule {
    /**
     * The **time constant** of these neurons. If **timeConstant *
     * leakConstant == network time-step** (or vice versa), behavior is
     * equivalent to discrete sigmoid. The larger the time constant relative to
     * the time-step, the more slowly inputs will be integrated.
     */
    @UserParameter(
        label = "Time Constant",
        description = "The time constant controls how quickly the numerical integration occurs.",
        increment = .1,
        order = 1
    )
    var timeConstant: Double = DEFAULT_TIME_CONSTANT

    /**
     * The leak constant: how strongly the neuron will be attracted to its base
     * activation. If **timeConstant * leakConstant == network time-step**
     * (or vice versa), behavior is equivalent to discrete sigmoid.
     */
    @UserParameter(label = "Leak Constant", description = "An option to add noise.", increment = .1, order = 2)
    var leakConstant: Double = DEFAULT_LEAK_CONSTANT

    /**
     * The net value of this neuron. This is the value that is integrated over
     * time and then passed to the squashing function. NOTE: the net inputs are
     * integrated and that value is passed through a squashing function to give
     * the neurons activation. The activation post-squashing is NOT what is
     * being numerically integrated.
     */
    private var netActivation = 0.0

    private var inputTerm = 0.0

    /**
     * Default sigmoidal.
     */
    constructor() : super()

    /**
     * Construct a sigmoid update with a specified implementation.
     *
     */
    constructor(sFunction: SigmoidFunctionEnum) : super() {
        this.type = sFunction
    }

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
     *
     * **c * dx_i/dt = -ax_i(t) + sum(w_ij * r_j(t)**
     *
     *
     * Discretizing using euler integration:
     *
     *
     * **x_i(t + dt) = x_i(t) - (ax_i(t) * dt/c) + (dt/c)*sum(w_ij * r_j(t))**
     *
     *
     * Factorting out x_i(t):
     *
     *
     * **x_i(t + dt) = x_i(t) * (1 - a*dt/c) + (dt/c) * sum(w_ij * r_j(t))**
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

    val noBytes: Int
        get() = // bump to interface...
            // [ buff | netInp | netAct | leak | tau | UB | LB | slope ]
            56 + 8 // Do some reflection here... 8 is for buffer

    private var offset = 0

    fun update(offset: Int, arr: DoubleArray) {
        arr[offset] = arr[offset + 2] * (1 - arr[offset + 3] * arr[offset + 4]) + arr[offset + 4] * arr[offset + 1]
        // arr[offset] = sFunction.valueOf(arr[offset], arr[offset + 5], arr[offset + 6], arr[offset + 7]);
    }

    fun writeToArr(net: Network, arr: DoubleArray, _offset: Int): Int {
        offset = _offset
        arr[_offset + 1] = inputTerm
        arr[_offset + 2] = netActivation
        arr[_offset + 3] = leakConstant
        arr[_offset + 4] = net.timeStep / timeConstant
        arr[_offset + 5] = upperBound
        arr[_offset + 6] = lowerBound
        arr[_offset + 7] = this.slope
        return _offset + 8 // padding
    }

    context(Network)
    fun writeFromArr(neu: Neuron, arr: DoubleArray) {
        neu.activation = arr[offset]
        netActivation = arr[offset + 2]
        leakConstant = arr[offset + 3]
        timeConstant = 1 / arr[offset + 4] * timeStep
        inputTerm = arr[offset + 1]
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

    companion object {
        /**
         * Default time constant (ms).
         */
        const val DEFAULT_TIME_CONSTANT: Double = 10.0

        /**
         * Default leak constant [.leak].
         */
        const val DEFAULT_LEAK_CONSTANT: Double = 1.0
    }
}