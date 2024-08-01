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
        order = 10
    )
    var U = 0.5

    /**
     * Depression constant.
     */
    @UserParameter(
        label = "Mean Depression ",
        description = "Time constant for neurotransmitter depression.",
        minimumValue = 0.0,
        increment = .1,
        order = 20
    )
    var D = 1100.0

    /**
     * Facilitation constant.
     */
    @UserParameter(label = "Mean Facilitation ", description = "Time constant for facilitating effects.", order = 30)
    var F = 50.0

    @UserParameter(label = "Spike Responder", description = "Short term plasticity sets the max response of this responder", order = 50)
    var spikeResponderLocal: SpikeResponder = JumpAndDecay().apply { useConvolution = true }

    /**
     * Does not actually copy this UDF object. Since UDF has values always
     * drawn from a distribution, it simply gives a new UDF object which
     * proceeds to draw its parameters from the same distributions.
     */
    override fun copy(): UDF {
        val copy = UDF()
        copy.U = U
        copy.D = D
        copy.F = F
        return copy
    }

    override val description: String
        get() {
            return "UDF (Short-term Plasticity)"
        }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        val udfData = responderData as UDFScalarDataHolder
        var u by udfData::u
        var R by udfData::R
        if (synapse.source.isSpike && probabilisticSpikeCheck()) {
            val ISI = synapse.source.lastSpikeTime - time
            u = U + u * (1 - U) * exp(ISI / F)
            R = 1 + (R - u * R - 1) * exp(ISI / D)
            val jumpHeight = R * synapse.strength * u
            synapse.psr = when (val sr = spikeResponderLocal) {
                is JumpAndDecay -> sr.jumpAndDecay(true, synapse.psr, jumpHeight, timeStep)
                else -> throw IllegalStateException("UDF can only be used with JumpAndDecay")
            }
        } else {
            spikeResponderLocal.apply(synapse, responderData)
        }
    }

    override fun createResponderData(): UDFScalarDataHolder {
        return UDFScalarDataHolder(U, 1.0)
    }

    override val name: String
        get() = "STP (UDF)"

    /**
     *
     * An intelligent randomization strategy for the responder’s parameters based on a source synapse.
     * Currently, there is no obvious way to integrate this into the GUI.
     *
     * Initializes this UDF object based on the synapse it governs. UDF draws
     * its values from different distributions based on the polarity of the
     * source and target neurons.
     *
     * @param s the synapse which is used to determine what polarities of
     * neurons the synapse connects and draw values based on that.
     */
    fun init(s: Synapse) {
        when {
            s.source.polarity === Polarity.EXCITATORY
                    && s.target.polarity === Polarity.EXCITATORY -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble()
                D = NormalDistribution(mean = 1100.0, standardDeviation = 550.0).sampleDouble()
                F = NormalDistribution(mean = 50.0, standardDeviation = 25.0).sampleDouble()
            }
            s.source.polarity === Polarity.EXCITATORY
                    && s.target.polarity === Polarity.INHIBITORY -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble()
                D = NormalDistribution(mean = 125.0, standardDeviation = 62.5).sampleDouble()
                F = NormalDistribution(mean = 120.0, standardDeviation = 60.0).sampleDouble()
            }
            s.source.polarity === Polarity.INHIBITORY
                    && s.target.polarity === Polarity.EXCITATORY -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble()
                D = NormalDistribution(mean = 700.0, standardDeviation = 350.0).sampleDouble()
                F = NormalDistribution(mean = 20.0, standardDeviation = 10.0).sampleDouble()
            }
            s.source.polarity === Polarity.INHIBITORY
                    && s.target.polarity === Polarity.EXCITATORY -> {
                U = NormalDistribution(mean = 0.32, standardDeviation = 0.16).sampleDouble()
                D = NormalDistribution(mean = 144.0, standardDeviation = 72.0).sampleDouble()
                F = NormalDistribution(mean = 60.0, standardDeviation = 30.0).sampleDouble()
            }
            else -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble()
                D = NormalDistribution(mean = 1100.0, standardDeviation = 550.0).sampleDouble()
                F = NormalDistribution(mean = 50.0, standardDeviation = 25.0).sampleDouble()
            }
        }
    }
}

class UDFScalarDataHolder(
    @UserParameter(label = "U", description = "Use/Facilitation variable", order = 1)
    var u: Double,
    @UserParameter(label = "R", description = "Depression variable", order = 2)
    var R: Double
) : ScalarDataHolder {
    override fun copy(): UDFScalarDataHolder {
        return UDFScalarDataHolder(u, R)
    }
}