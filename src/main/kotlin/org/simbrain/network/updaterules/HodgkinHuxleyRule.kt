package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
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
class HodgkinHuxleyRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), NoisyUpdateRule {
    /**
     * Sodium Channels
     */
    @UserParameter(label = "Sodium Channels", description = "Sodium Channels", order = 1)
    private var perNaChannels = 100f

    /**
     * Potassium
     */
    @UserParameter(label = "Potassium Channels", description = "Sodium Channels", order = 2)
    private var perKChannels = 100f

    /**
     * Resting Membrane Potential
     */
    private val resting_v = 65.0

    /**  */
    private var dv = 0.0

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
     * voltage-dependent gating parameters
     */
    var n: Double = 0.0
        private set
    var m: Double = 0.0
        private set
    var h: Double = 0.0
        private set

    /**
     * corresponding deltas
     */
    private var dn: Double
    private var dm: Double
    private var dh: Double

    /**
     * // rate constants
     */
    private var an: Double
    private var bn: Double
    private var am: Double
    private var bm: Double
    private var ah: Double
    private var bh: Double

    /**
     * Ek-Er, Ena - Er, Eleak - Er
     */
    private var vk: Double
    private var vna: Double
    private val vl: Double

    /**  */
    private var n4 = 0.0

    /**  */
    private var m3h = 0.0

    /**
     * Sodium current
     */
    private var na_current = 0.0

    /**
     * Potassium current
     */
    private var k_current = 0.0

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

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        // Advances the model by dt and returns the new voltage

        val v = neuron.input
        bh = 1 / (exp((v + 30) / 10) + 1)
        ah = 0.07 * exp(v / 20)
        dh = (ah * (1 - h) - bh * h) * timeStep
        bm = 4 * exp(v / 18)
        am = 0.1 * (v + 25) / (exp((v + 25) / 10) - 1)
        bn = 0.125 * exp(v / 80)
        an = 0.01 * (v + 10) / (exp((v + 10) / 10) - 1)
        dm = (am * (1 - m) - bm * m) * timeStep
        dn = (an * (1 - n) - bn * n) * timeStep

        n4 = n * n * n * n
        m3h = m * m * m * h

        na_current = gna * m3h * (v - vna)
        k_current = gk * n4 * (v - vk)

        dv = -1 * timeStep * (k_current + na_current + gl * (v - vl)) / cm

        neuron.activation = -1 * (v + dv + resting_v)
        h += dh
        m += dm
        n += dn

        // if (vClampOn)
        // v = vClampValue;

        // getV() converts the model's v to present day convention
    }

    // Initializer quickly hacked from old init. Zoë this is in your hands to fix! :)
    init {
        val v = -70.0 // Arbitrary starting voltage
        val dv = .001 // Arbitrary starting dv.  Not sure how to set.
        vna = -115.0
        vk = 12.0
        vl = -10.613
        gna = (perNaChannels * 120 / 100).toDouble()
        gk = (perKChannels * 36 / 100).toDouble()
        gl = 0.3

        bh = 1 / (exp((v + 30) / 10) + 1)
        ah = 0.07 * exp(v / 20)
        bm = 4 * exp(v / 18)
        am = 0.1 * (v + 25) / (exp((v + 25) / 10) - 1)
        bn = 0.125 * exp(v / 80)
        an = 0.01 * (v + 10) / (exp((v + 10) / 10) - 1)
        dh = (ah * (1 - h) - bh * h) * dv
        dm = (am * (1 - m) - bm * m) * dv
        dn = (an * (1 - n) - bn * n) * dv

        // start these parameters in steady state
        n = an / (an + bn)
        m = am / (am + bm)
        h = ah / (ah + bh)
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    fun get_n4(): Double {
        return n4
    }

    fun get_m3h(): Double {
        return m3h
    }

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

    // The -1 is to correct for the fact that in the H & H paper, the currents
    // are reversed.
    fun get_na_current(): Double {
        return -1 * na_current
    }

    fun get_k_current(): Double {
        return -1 * k_current
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
     *
     * @param voltage
     * @return
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