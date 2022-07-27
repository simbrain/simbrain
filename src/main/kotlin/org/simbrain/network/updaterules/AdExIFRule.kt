/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.updaterules

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.AdexData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * An implementation of adaptive exponential integrate and fire. This version
 * of integrate and fire includes an exponential term as a part of the
 * differential equation as well as an adaptation term which lowers the
 * membrane potential in response to successive spikes.
 * See Toboul &#38; Brette 2005.
 *
 * @see [...](http://www.scholarpedia.org/article/Adaptive_exponential_integrate-and-fire_model)
 *
 *
 * @author Zoë Tosi
 */
open class AdExIFRule : SpikingNeuronUpdateRule(), NoisyUpdateRule {
    /**
     * Reset voltage (mV). Defaults to 3-spike bursting behavior at .8 nA
     * current. See Touboul & Brette 2005 -48.5: 2 spike burst -47.2: 4 spike
     * burst -48: chaotic spike response
     */
    @UserParameter(
        label = "Reset voltage (mV)",
        description = "This represents the voltage to which the membrane potential will be reset after "
                + "an action potential has fired.",
        increment = .1,
        order = 3,
        tab = "Membrane Voltage",
        minimumValue = -70.0,
        maximumValue = -35.0,
        probDist = "Normal",
        probParam1 = -50.0,
        probParam2 = 5.0
    )
    var v_Reset = -47.7

    /**
     * Threshold voltage (mV). This determines when a neuron will start a
     * divergent change in voltage that will tend toward infinity and is not the
     * voltage at which we consider the neuron to have spiked. External factors
     * can still cause an action potential to fail even if v_mem > v_Th.
     */
    @UserParameter(
        label = "Threshold voltage (mV)",
        description = "This determines when a neuron will start a divergent change in voltage that will tend "
                + "toward infinity and is not the voltage at which we consider the neuron to have spiked.",
        increment = .1,
        order = 2,
        tab = "Membrane Voltage"
    )
    var v_Th = -50.4

    /**
     * The peak voltage after which we say with certainty that an action
     * potential has occurred (mV).
     */
    @UserParameter(
        label = "Peak Voltage (mV)",
        description = "The peak voltage after which we say with certainty that an action potential has occurred (mV).",
        increment = .1,
        order = 1,
        tab = "Membrane Voltage"
    )
    var v_Peak = 20.0

    /**
     * Leak Conductance (nS).
     */
    @UserParameter(
        label = "Leak Conductance (nS)",
        description = "The inverse of the resistance of the channels through which current leaks from the neuron.",
        increment = .1,
        order = 6,
        tab = "Input Currents"
    )
    private var g_L = 30.0

    /**
     * Maximal excitatory conductance. (nS)
     */
    @UserParameter(
        label = "Max Ex. Conductance (nS)",
        description = "The excitatory conductance if all excitatory channels are open.",
        increment = .1,
        order = 7,
        tab = "Input Currents"
    )
    var g_e_bar = 10.0

    /**
     * Maximal inhibitory conductance. (nS)
     */
    @UserParameter(
        label = "Max In. Conductance (nS)",
        description = "The inhibitory conductance if all inhibitory channels are open.",
        increment = .1,
        order = 8,
        tab = "Input Currents"
    )
    var g_i_bar = 10.0

    /**
     * Leak strength (mV).
     */
    @UserParameter(
        label = "Leak Reversal (mV)",
        description = "The membrane potential at which leak currents would no longer have "
                + "any effect on the neuron's membrane potential.",
        increment = .1,
        order = 9,
        tab = "Input Currents"
    )
    var leakReversal = -70.6

    /**
     * Excitatory reversal. (mV).
     */
    @UserParameter(
        label = "Excitatory Reversal (mV)",
        description = "The membrane potential at which impinging excitatory (depolarizing) "
                + "inputs reach equilibrium.",
        increment = .1,
        order = 10,
        tab = "Input Currents"
    )
    var exReversal = 0.0

    /**
     * Inhibitory reversal. (mV)
     */
    @UserParameter(
        label = "Inbitatory Reversal (mV)",
        description = "The membrane potential at which impinging inhibitory (hyperpolarizing) "
                + "inputs reach equilibrium.",
        increment = .1,
        order = 11,
        tab = "Input Currents"
    )
    var inReversal = -75.0

    /**
     * Membrane potential (mV).
     */
    private var v_mem = leakReversal

    /**
     * Adaptation reset parameter (nA).
     */
    @UserParameter(
        label = "Reset (nA)",
        description = "Adaptation reset parameter (nA)",
        increment = .1,
        order = 12,
        tab = "Adaptation",
        probDist = "Uniform",
        probParam1 = 0.01,
        probParam2 = .3
    )
    private var b = 0.0805

    /**
     * Adaptation time constant (ms).
     */
    @UserParameter(
        label = "Time constant (ms)",
        description = "Controls the rate at which the neuron attains its resting potential.",
        increment = .1,
        order = 13,
        tab = "Adaptation"
    )
    private var tauW = 40.0

    /**
     * mV
     */
    @UserParameter(
        label = "Slope Factor", description = "A value which regulates the overall effect of the exponential term on "
                + "the membrane potential equation.", increment = .1, order = 6, tab = "Membrane Voltage"
    )
    private var slopeFactor = 2.0

    /**
     * Adaptation coupling parameter (nS).
     */
    @UserParameter(
        label = "Coupling Const.",
        description = "This represents the voltage to which the membrane potential will be reset after "
                + "an action potential has fired.",
        increment = .1,
        order = 14,
        tab = "Adaptation"
    )
    private var a = 4.0

    /**
     * Membrane Capacitance (pico Farads).
     */
    @UserParameter(
        label = "Capacitance (μF)",
        description = "A paramter designating the overall ability of the neuron's membrane to retain a charge.",
        increment = .1,
        order = 4,
        tab = "Membrane Voltage",
        minimumValue = 181.0,
        maximumValue = 381.0,
        probDist = "Normal",
        probParam1 = 281.0,
        probParam2 = 28.1
    )
    private var memCapacitance = 281.0

    /**
     * Background current being directly injected into the neuron (nA).
     */
    @UserParameter(
        label = "Background Current (nA)",
        description = "A tunable parameter in some ways similar to a bias parameter for non-spiking neurons.",
        increment = .1,
        order = 5,
        tab = "Membrane Voltage"
    )
    private var i_bg = 0.0

    /**
     * An option to add noise.
     */
    private var addNoise = false

    /**
     * The noise generator randomizer.
     */
    private var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * An absolute refractory period. Not normally a part of AdEx, but can
     * optionally be used to promote network stability.
     */
    var refractoryPeriod = 1.0
    override fun createScalarData(): ScalarDataHolder {
        return AdexData()
    }

    override fun apply(neuron: Neuron, dat: ScalarDataHolder) {
        val data = dat as AdexData
        if (v_mem >= v_Peak) {
            v_mem = v_Reset
            neuron.forceSetActivation(v_Reset)
        }
        // Retrieve integration time constant in case it has changed...
        val dt = neuron.network.timeStep
        //        final double ref = neuron.getNetwork().getTimeType()
        //                == TimeType.DISCRETE ? refractoryPeriod / dt
        //                        : refractoryPeriod;
        val refractory = neuron.lastSpikeTime + refractoryPeriod >= neuron.network.time

        // Retrieve membrane potential from host neuron's activation
        // in case some outside entity has explicitly changed the membrane
        // potential between updates.
        v_mem = neuron.activation

        // Retrieve incoming ex/in currents or proportion of open channels
        data.exConductance = 0.0
        data.inhibConductance = 0.0
        // TODO
//        for (ii in neuron.fanIn.indices) {
//            neuron.fanIn[ii].updateOutput()
//            val `val` = neuron.fanIn[ii].psr
//            if (neuron.polarity === SimbrainConstants.Polarity.INHIBITORY) {
//                data.inhibConductance = data.inhibConductance + `val`
//            } else {
//                data.exConductance = data.exConductance + `val`
//            }
//        }

        // Calculate incoming excitatory and inhibitory voltage changes
        val iSyn_ex = g_e_bar * data.exConductance * (exReversal - v_mem)
        val iSyn_in = -g_i_bar * data.inhibConductance * (inReversal - v_mem)

        // Calculate voltage changes due to leak
        val i_leak = g_L * (leakReversal - v_mem)
        var ibg = i_bg

        // Add noise if there is any to be added
        if (addNoise) {
            ibg += noiseGenerator.sampleDouble()
        }

        // Calc dV/dt for membrane potential
        var dVdt =
            g_L * slopeFactor * Math.exp((v_mem - v_Th) / slopeFactor) + i_leak + iSyn_ex + iSyn_in + ibg - data.w


        // Factor in membrane capacitance...
        dVdt /= memCapacitance

        // Calculate adaptation change
        val dwdt = (a * (v_mem - leakReversal) - data.w) / tauW

        // Integrate membrane potential and adaptation parameter using
        // Euler integration
        v_mem += dVdt * dt
        data.w = data.w + dwdt * dt

        // Spike?
        if (v_mem >= v_Peak) {
            v_mem = v_Peak
            data.w = data.w + b * CURRENT_CONVERTER
            if (!refractory) {
                neuron.isSpike = true
            } else {
                neuron.isSpike = false
            }
        } else {
            neuron.isSpike = false
        }

        // Set the buffer to the membrane potential
        neuron.activation = v_mem
    }

    override fun deepCopy(): AdExIFRule {
        val cpy = AdExIFRule()
        cpy.a = a
        cpy.addNoise = addNoise
        cpy.b = b
        cpy.g_L = g_L
        cpy.leakReversal = leakReversal
        cpy.memCapacitance = memCapacitance
        cpy.noiseGenerator = noiseGenerator.deepCopy()
        cpy.slopeFactor = slopeFactor
        cpy.tauW = tauW
        cpy.v_mem = v_mem
        cpy.v_Reset = v_Reset
        cpy.v_Th = v_Th
        return cpy
    }

    override val name: String
        get() = "AdEx Integrate and Fire"

    override fun getNoiseGenerator(): ProbabilityDistribution {
        return noiseGenerator
    }

    override fun setNoiseGenerator(noise: ProbabilityDistribution) {
        noiseGenerator = noise
    }

    override fun getAddNoise(): Boolean {
        return addNoise
    }

    override fun setAddNoise(noise: Boolean) {
        addNoise = noise
    }

    override fun getGraphicalLowerBound(): Double {
        return leakReversal - 20
    }

    override fun getGraphicalUpperBound(): Double {
        return v_Th + 10
    }

    fun getI_bg(): Double {
        return i_bg / CURRENT_CONVERTER
    }

    fun setI_bg(i_bg: Double) {
        // Conversion so that bg currents can be entered as nano amperes
        // instead of as pico amperes, which is more consistent with units
        // used elsewhere in Simbrain.
        this.i_bg = CURRENT_CONVERTER * i_bg
    }

    companion object {
        /**
         * A converter from pA to nA, since most other sims in Simbrain use
         * nano Amps.
         */
        const val CURRENT_CONVERTER = 1000.0
    }
}