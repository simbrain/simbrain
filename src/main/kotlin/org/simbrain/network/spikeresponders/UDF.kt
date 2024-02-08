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
package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.exp

/**
 * An experimental no-GUI only implementation of the UDF synapse. This is a
 * stop gap implementation. UDF isn't really a spike responder, as it determines
 * the jump-height of a convolved jump and decay spike responder.
 *
 * See http://www.scholarpedia.org/article/Short-term_synaptic_plasticity
 *
 * @author Zoë Tosi
 */
class UDF : SpikeResponder() {
    //TODO: Make this like a real thing... where you can set parameters instead of it doing it automatically, but giving the illusion of control
    /**
     * Use constant.
     */
    @UserParameter(
        label = "Mean Use ",
        description = "Baseline use and strength of facilitation.",
        minimumValue = 0.0,
        increment = .1,
        order = 1
    )
    private var U = 0.5

    /**
     * Depression constant.
     */
    @UserParameter(
        label = "Mean Depression ",
        description = "Time constant for neurotransmitter depression.",
        minimumValue = 0.0,
        increment = .1,
        order = 1
    )
    private var D = 1100.0

    /**
     * Facilitation constant.
     */
    @UserParameter(label = "Mean Facilitation ", description = "Time constant for facilitating effects.", order = 1)
    private var F = 50.0

    /**
     * Psr decay time constant
     */
    @UserParameter(label = "PSR Decay Constant", description = "Time constant for facilitating effects.", order = 1)
    private val tau = 3.0

    /**
     * The time of the last spike (recorded here since
     * SpikingNeuronUpdateRule writes over its own copy).
     */
    private var lastSpikeTime = 0.0

    /**
     * Use/Facilitation variable
     */
    private var u = 0.0

    /**
     * Depression variable.
     */
    private var R = 1.0

    /**
     * The actual spike responder for the post synaptic response for UDF.
     */
    private val spikeDecay = ConvolvedJumpAndDecay()

    /**
     * Whether or not this is the first time this is being updated. If so it
     * initializes the variables.
     */
    private var firstTime = true
    var rand = NormalDistribution()

    /**
     * Does not actually copy this UDF object. Since UDF has values always
     * drawn from a distribution, it simply gives a new UDF object which
     * proceeds to draw its parameters from the same distributions.
     */
    override fun deepCopy(): UDF {
        // TODO
        return UDF()
    }

    override val description: String
        get() {
            return "UDF (Short-term Plasticity)"
        }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        if (firstTime) {
            init(synapse)
            spikeDecay.timeConstant = tau
            firstTime = false
        }
        val A: Double
        if (synapse.source.isSpike) {
            val ISI = lastSpikeTime - time
            u = U + u * (1 - U) * exp(ISI / F)
            R = 1 + (R - u * R - 1) * exp(ISI / D)
            A = R * synapse.strength * u
            lastSpikeTime = time
            synapse.psr = spikeDecay.convolvedJumpAndDecay(synapse.source.isSpike, synapse.psr, synapse.strength, timeStep)
        } else {
            spikeDecay.apply(synapse, responderData)
        }
    }


    override val name: String
        get() = "STP (UDF)"

    /**
     * Initializes this UDF object based on the synapse it governs. UDF draws
     * its values from different distributions based on the polarity of the
     * source and target neurons.
     *
     * @param s the synapse which is used to determine what polarities of
     * neurons the synapse connects and draw values based on that.
     */
    fun init(s: Synapse) {
        if (s.source.polarity === Polarity.EXCITATORY
            && s.target.polarity === Polarity.EXCITATORY
        ) {
            rand.mean = 0.5
            rand.standardDeviation = 0.25
            U = rand.sampleDouble()
            rand.mean = 1100.0
            rand.standardDeviation = 550.0
            D = rand.sampleDouble()
            rand.mean = 50.0
            rand.standardDeviation = 25.0
            F = rand.sampleDouble()
            spikeDecay.timeConstant = 3.0
        } else if (s.source.polarity === Polarity.EXCITATORY
            && s.target.polarity === Polarity.INHIBITORY
        ) {
            rand.mean = 0.05
            rand.standardDeviation = 0.025
            U = rand.sampleDouble()
            rand.mean = 125.0
            rand.standardDeviation = 62.5
            D = rand.sampleDouble()
            rand.mean = 120.0
            rand.standardDeviation = 60.0
            F = rand.sampleDouble()
            spikeDecay.timeConstant = 3.0
        } else if (s.source.polarity === Polarity.INHIBITORY
            && s.target.polarity === Polarity.EXCITATORY
        ) {
            rand.mean = 0.25
            rand.standardDeviation = 0.125
            U = rand.sampleDouble()
            rand.mean = 700.0
            rand.standardDeviation = 350.0
            D = rand.sampleDouble()
            rand.mean = 20.0
            rand.standardDeviation = 10.0
            F = rand.sampleDouble()
            spikeDecay.timeConstant = 6.0
        } else if (s.source.polarity === Polarity.INHIBITORY
            && s.target.polarity === Polarity.EXCITATORY
        ) {
            rand.mean = 0.32
            rand.standardDeviation = 0.16
            U = rand.sampleDouble()
            rand.mean = 144.0
            rand.standardDeviation = 72.0
            D = rand.sampleDouble()
            rand.mean = 60.0
            rand.standardDeviation = 30.0
            F = rand.sampleDouble()
            spikeDecay.timeConstant = 6.0
        } else {
            rand.mean = 0.5
            rand.standardDeviation = 0.25
            U = rand.sampleDouble()
            rand.mean = 1100.0
            rand.standardDeviation = 550.0
            D = rand.sampleDouble()
            rand.mean = 50.0
            rand.standardDeviation = 25.0
            F = rand.sampleDouble()
            spikeDecay.timeConstant = 3.0
        }
        u = U
    }
}