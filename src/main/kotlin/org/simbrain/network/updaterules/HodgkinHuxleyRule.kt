package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.MembranePotentialProvider
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.exp

/**
 * Hodgkin-Huxley Neuron.
 *
 *
 * Adapted from software written by Anthony Fodor, with help from Jonathan
 * Vickrey.
 */
class HodgkinHuxleyRule : NeuronUpdateRule<HodgkinHuxleyData, EmptyMatrixData>(), NoisyUpdateRule,
    MembranePotentialProvider {

    override fun membranePotential(neuron: Neuron): Double = neuron.activation
    /**
     * Sodium Channels
     */
    @UserParameter(label = "Sodium channels", description = "Sodium channels", order = 1)
    private var perNaChannels = 100f

    /**
     * Potassium
     */
    @UserParameter(label = "Potassium channels", description = "Potassium channels", order = 2)
    private var perKChannels = 100f

    /**
     * Resting Membrane Potential
     */
    private val resting_v = 65.0

    // remember that H&H voltages are -1 * present convention
    // TODO: should eventually calculate this instead of setting it
    // convert between internal use of V and the user's expectations
    // the V will be membrane voltage using present day conventions
    // see p. 505 of Hodgkin & Huxley, J Physiol. 1952, 117:500-544
    /**
     * Membrane Capacitance
     */
    var cm: Double = 1.0

    /**
     * Constant leak permeabilities
     */
    private var gk: Double
    private var gna: Double
    private val gl: Double

    /**
     * Ek-Er, Ena - Er, Eleak - Er
     */
    private var vk: Double
    private var vna: Double
    private val vl: Double

    /**  */
    var temp: Double = 0.0

    /**  */
    var vClampOn: Boolean = false

    /**  */
    var vClampValue: Float = convertV(0f)

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to the neuron.
     */
    override var addNoise: Boolean = false

    override fun createScalarData() = HodgkinHuxleyData()

    context(Network)
    override fun apply(neuron: Neuron, data: HodgkinHuxleyData) {
        // Advances the model by dt and returns the new voltage

        val v = neuron.input
        val rates = GatingRates(v)
        val dh = (rates.ah * (1 - data.h) - rates.bh * data.h) * timeStep
        val dm = (rates.am * (1 - data.m) - rates.bm * data.m) * timeStep
        val dn = (rates.an * (1 - data.n) - rates.bn * data.n) * timeStep

        val n4 = data.n * data.n * data.n * data.n
        val m3h = data.m * data.m * data.m * data.h

        val naCurrent = gna * m3h * (v - vna)
        val kCurrent = gk * n4 * (v - vk)
        // The -1 is to correct for the fact that in the H & H paper, the currents are reversed.
        data.naCurrent = -1 * naCurrent
        data.kCurrent = -1 * kCurrent

        val dv = -1 * timeStep * (kCurrent + naCurrent + gl * (v - vl)) / cm

        neuron.activation = -1 * (v + dv + resting_v)
        data.h += dh
        data.m += dm
        data.n += dn

        // if (vClampOn)
        // v = vClampValue;

        // getV() converts the model's v to present day convention
    }

    // Initializer quickly hacked from old init. Zoë this is in your hands to fix! :)
    init {
        vna = -115.0
        vk = 12.0
        vl = -10.613
        gna = (perNaChannels * 120 / 100).toDouble()
        gk = (perKChannels * 36 / 100).toDouble()
        gl = 0.3
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    @get:Synchronized
    @set:Synchronized
    var ena: Float
        get() = (-1 * (vna + resting_v)).toFloat()
        set(Ena) {
            vna = -1 * Ena - resting_v
        }

    @get:Synchronized
    @set:Synchronized
    var ek: Float
        get() = (-1 * (vk + resting_v)).toFloat()
        set(Ek) {
            vk = -1 * Ek - resting_v
        }

    // negative values set to zero
    @Synchronized
    fun setPerNaChannels(perNaChannels: Float) {
        var perNaChannels = perNaChannels
        if (perNaChannels < 0) {
            perNaChannels = 0f
        }
        this.perNaChannels = perNaChannels
        gna = (120 * perNaChannels / 100).toDouble()
    }

    fun getPerNaChannels(): Float {
        return perNaChannels
    }

    @Synchronized
    fun setPerKChannels(perKChannels: Float) {
        var perKChannels = perKChannels
        if (perKChannels < 0) {
            perKChannels = 0f
        }
        this.perKChannels = perKChannels
        gk = (36 * perKChannels / 100).toDouble()
    }

    fun getPerKChannels(): Float {
        return perKChannels
    }

    /**
     * Converts a voltage from the modern convention to the convention used by
     * the program.
     */
    fun convertV(voltage: Float): Float {
        return (-1 * voltage - resting_v).toFloat()
    }

    fun get_vClampValue(): Float {
        return (-1 * (vClampValue + resting_v)).toFloat()
    }

    fun set_vClampValue(vClampValue: Float) {
        this.vClampValue = convertV(vClampValue)
    }

    override fun copy(): NeuronUpdateRule<*, *> {
        val hhr = HodgkinHuxleyRule()
        hhr.set_vClampValue(this.get_vClampValue())
        hhr.addNoise = this.addNoise
        hhr.cm = cm
        hhr.ek = ek
        hhr.ena = ena
        hhr.noiseGenerator = this.noiseGenerator
        hhr.setPerKChannels(this.getPerKChannels())
        hhr.setPerNaChannels(this.getPerNaChannels())
        hhr.temp = temp
        hhr.vClampOn = vClampOn
        return hhr
    }

    override val name: String
        get() = "Hodgkin-Huxley"

}

/** Hodgkin and Huxley's voltage-dependent rate constants at membrane voltage [v], in the model's convention. */
private class GatingRates(v: Double) {
    val ah = 0.07 * exp(v / 20)
    val bh = 1 / (exp((v + 30) / 10) + 1)
    val am = 0.1 * (v + 25) / (exp((v + 25) / 10) - 1)
    val bm = 4 * exp(v / 18)
    val an = 0.01 * (v + 10) / (exp((v + 10) / 10) - 1)
    val bn = 0.125 * exp(v / 80)
}

/**
 * One neuron's [HodgkinHuxleyRule] state: the voltage-dependent gating variables, which start in their steady state
 * at an arbitrary resting voltage, and the last sodium and potassium currents, for display. Kept per neuron so that
 * neurons sharing a rule object each integrate their own gates.
 */
class HodgkinHuxleyData(
    @UserParameter(label = "n", description = "Potassium activation gate", order = 1)
    var n: Double = steadyState.run { an / (an + bn) },
    @UserParameter(label = "m", description = "Sodium activation gate", order = 2)
    var m: Double = steadyState.run { am / (am + bm) },
    @UserParameter(label = "h", description = "Sodium inactivation gate", order = 3)
    var h: Double = steadyState.run { ah / (ah + bh) }
) : ScalarDataHolder {

    @UserParameter(label = "Sodium current", displayOnly = true, order = 4)
    var naCurrent = 0.0

    @UserParameter(label = "Potassium current", displayOnly = true, order = 5)
    var kCurrent = 0.0

    override fun copy() = HodgkinHuxleyData(n, m, h).also {
        it.naCurrent = naCurrent
        it.kCurrent = kCurrent
    }

    private companion object {
        // Arbitrary starting voltage
        val steadyState = GatingRates(-70.0)
    }
}
