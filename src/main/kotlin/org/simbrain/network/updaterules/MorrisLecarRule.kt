package org.simbrain.network.updaterules

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.MorrisLecarData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * @author Zoë Tosi
 */
class MorrisLecarRule : SpikingNeuronUpdateRule(), NoisyUpdateRule {
    /**
     * Calcium channel conductance (micro Siemens/cm^2).
     */
    @UserParameter(
        label = "Ca²⁺ Conductance (µS/cm²)",
        description = "Calcium conductance. If higher, voltage pulled more quickly to Ca2+ equilibrium.",
        increment = .1,
        order = 7,
        tab = "Ion Properties"
    )
    private var g_Ca = 4.0

    /**
     * Potassium channel conductance (micro Siemens/cm^2).
     */
    @UserParameter(
        label = "K⁺ Conductance (µS/cm²)",
        description = "Potassium conductance. If higher, voltage pulled more quickly to K+ equilibrium.",
        increment = .1,
        order = 8,
        tab = "Ion Properties"
    )
    private var g_K = 8.0

    /**
     * Leak conductance (micro Siemens/cm^2).
     */
    @UserParameter(
        label = "Leak Conductance (µS/cm²)",
        description = "Leak conductance. If higher, voltage pulled more quickly to Leak equilibrium.",
        increment = .1,
        order = 9,
        tab = "Ion Properties"
    )
    private var g_L = 2.0

    /**
     * Resting potential calcium (mV).
     */
    @UserParameter(
        label = "Ca²⁺ Equilibrium (mV)",
        description = "Calcium equilibrium.",
        increment = .1,
        order = 10,
        tab = "Ion Properties"
    )
    private var vRest_Ca = 120.0

    /**
     * Resting potential potassium (mV).
     */
    @UserParameter(
        label = "K⁺ Equilibrium (mV)",
        description = "An option to add noise.",
        increment = .1,
        order = 11,
        tab = "Ion Properties"
    )
    private var vRest_k = -80.0

    /**
     * Resting potential for leak current (mV).
     */
    @UserParameter(
        label = "Leak Equilibrium (mV)",
        description = "An option to add noise.",
        increment = .1,
        order = 12,
        tab = "Ion Properties"
    )
    private var vRest_L = -60.0

    /**
     * Membrane capacitance per unit area (micro Farads/cm^2).
     */
    @UserParameter(
        label = "Capacitance (µF/cm²)",
        description = "Behaves like a time constant. Higher capacitance leads to slower changes "
                + "in the cell.",
        increment = .1,
        order = 1,
        tab = "Membrane Properties"
    )
    private var cMembrane = 5.0

    /**
     * Membrane voltage constant 1.
     */
    @UserParameter(
        label = "Voltage Const. 1",
        description = "How does calcium respond to voltage.",
        increment = .1,
        order = 2,
        tab = "Membrane Properties"
    )
    private var v_m1 = -1.2

    /**
     * Membrane voltage constant 2.
     */
    @UserParameter(
        label = "Voltage Const. 2",
        description = "How does calcium respond to voltage.",
        increment = .1,
        order = 3,
        tab = "Membrane Properties"
    )
    private var v_m2 = 18.0

    /**
     * Potassium channel constant 1.
     */
    @UserParameter(
        label = "K⁺  Const. 1",
        description = "V3 on the Scholarpedia page, which roughly corresponds to how potassium current "
                + "responds to membrane voltage.",
        increment = .1,
        order = 13,
        tab = "K\u207A consts."
    )
    private var v_w1 = 2.0

    /**
     * Potassium channel constant 2.
     */
    @UserParameter(
        label = "K⁺  Const. 2",
        description = "V4 on the Scholarpedia page.",
        increment = .1,
        order = 14,
        tab = "K\u207A consts."
    )
    private var v_w2 = 17.4

    /**
     * Potassium channel time constant/decay rate (s^-1).
     */
    @UserParameter(
        label = "K⁺ φ",
        description = "Potassium channel time constant/decay rate. If higher, potassium changes more slowly.",
        increment = .1,
        order = 15,
        tab = "K\u207A consts."
    )
    private var phi = 0.06667 // 1/15

    /**
     * Background current (nA).
     */
    @UserParameter(
        label = "Background Current (nA)",
        description = "A constant level of current that can be set.",
        increment = .1,
        order = 5,
        tab = "Membrane Properties"
    )
    private var i_bg = 46.0

    /**
     * Threshold for neurotransmitter release (mV)
     */
    @UserParameter(
        label = "Threshold (mV)",
        description = "Voltages above this make the neuron spike",
        increment = .1,
        order = 4,
        tab = "Membrane Properties"
    )
    private var threshold = 40.0

    /**
     * Add noise to neuron.
     */
    override var addNoise = false

    /**
     * A source of noise (nA).
     */
    override var noiseGenerator: ProbabilityDistribution = NormalDistribution(0.0, 1.0)
    override fun apply(neuron: Neuron, dat: ScalarDataHolder) {
        val data = dat as MorrisLecarData
        val dt = neuron.network.timeStep
        val i_syn = neuron.input
        // Under normal circumstances this will cause no change.
        var vMembrane = neuron.activation
        val dVdt = dVdt(vMembrane, i_syn, data.w_K)
        val dWdt = dWdt(vMembrane, data.w_K)
        val vmFut = vMembrane + dt * dVdt
        val wKFut = data.w_K + dt * dWdt
        vMembrane = vMembrane + dt / 2 * (dVdt + dVdt(vmFut, i_syn, data.w_K))
        data.w_K = data.w_K + dt / 2 * (dWdt + dWdt(vMembrane, wKFut))
        neuron.isSpike = vMembrane > threshold
        neuron.activation = vMembrane
    }

    private fun dVdt(vMembrane: Double, i_syn: Double, w_K: Double): Double {
        val i_Ca = g_Ca * membraneFunction(vMembrane) * (vMembrane - vRest_Ca)
        val i_K = g_K * w_K * (vMembrane - vRest_k)
        val i_L = g_L * (vMembrane - vRest_L)
        val i_ion = i_Ca + i_K + i_L
        var i_noise = 0.0
        if (addNoise) {
            i_noise = noiseGenerator.sampleDouble()
        }
        return (i_bg - i_ion + i_syn + i_noise) / cMembrane
    }

    private fun dWdt(vMembrane: Double, w_K: Double): Double {
        return phi * lambdaFunction(vMembrane) * (k_fractionFunction(vMembrane) - w_K)
    }

    override fun createScalarData(): ScalarDataHolder {
        return MorrisLecarData()
    }

    private fun membraneFunction(vMembrane: Double): Double {
        return 0.5 * (1 + Math.tanh((vMembrane - v_m1) / v_m2))
    }

    private fun k_fractionFunction(vMembrane: Double): Double {
        return 0.5 * (1 + Math.tanh((vMembrane - v_w1) / v_w2))
    }

    private fun lambdaFunction(vMembrane: Double): Double {
        return Math.cosh((vMembrane - v_w1) / (2 * v_w2))
    }

    override fun deepCopy(): MorrisLecarRule {
        val cpy = MorrisLecarRule()
        cpy.addNoise = addNoise
        cpy.g_Ca = g_Ca
        cpy.g_K = g_K
        cpy.cMembrane = cMembrane
        cpy.g_L = g_L
        cpy.i_bg = i_bg
        cpy.phi = phi
        cpy.v_m1 = v_m1
        cpy.v_m2 = v_m2
        cpy.v_w1 = v_w1
        cpy.v_w2 = v_w2
        cpy.threshold = threshold
        cpy.vRest_Ca = vRest_Ca
        cpy.vRest_k = vRest_k
        cpy.vRest_L = vRest_L
        cpy.noiseGenerator = noiseGenerator.deepCopy()
        return cpy
    }

    override val name: String
        get() = "Morris-Lecar"

    fun setNoiseAmplitude(amp: Double) {
        (noiseGenerator as NormalDistribution).standardDeviation = amp
    }

    fun getG_Ca(): Double {
        return g_Ca
    }

    fun setG_Ca(g_Ca: Double) {
        this.g_Ca = g_Ca
    }

    fun getG_K(): Double {
        return g_K
    }

    fun setG_K(g_K: Double) {
        this.g_K = g_K
    }

    fun getG_L(): Double {
        return g_L
    }

    fun setG_L(g_L: Double) {
        this.g_L = g_L
    }

    fun getvRest_Ca(): Double {
        return vRest_Ca
    }

    fun setvRest_Ca(vRest_Ca: Double) {
        this.vRest_Ca = vRest_Ca
    }

    fun getvRest_k(): Double {
        return vRest_k
    }

    fun setvRest_k(vRest_k: Double) {
        this.vRest_k = vRest_k
    }

    fun getvRest_L(): Double {
        return vRest_L
    }

    fun setvRest_L(vRest_L: Double) {
        this.vRest_L = vRest_L
    }

    fun getcMembrane(): Double {
        return cMembrane
    }

    fun setcMembrane(cMembrane: Double) {
        this.cMembrane = cMembrane
    }

    fun getV_m1(): Double {
        return v_m1
    }

    fun setV_m1(v_m1: Double) {
        this.v_m1 = v_m1
    }

    fun getV_m2(): Double {
        return v_m2
    }

    fun setV_m2(v_m2: Double) {
        this.v_m2 = v_m2
    }

    fun getV_w1(): Double {
        return v_w1
    }

    fun setV_w1(v_w1: Double) {
        this.v_w1 = v_w1
    }

    fun getV_w2(): Double {
        return v_w2
    }

    fun setV_w2(v_w2: Double) {
        this.v_w2 = v_w2
    }

    fun getPhi(): Double {
        return phi
    }

    fun setPhi(phi: Double) {
        this.phi = phi
    }

    fun getI_bg(): Double {
        return i_bg
    }

    fun setI_bg(i_bg: Double) {
        this.i_bg = i_bg
    }

    fun getThreshold(): Double {
        return threshold
    }

    fun setThreshold(threshold: Double) {
        this.threshold = threshold
    }
}