/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.learningrules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/**
 * An implementation of Log-STDP as introduced in:
 *
 *
 * Gilson M, Fukai T (2011) Stability versus Neuronal Specialization for STDP:
 * Long-Tail Weight Distributions Solve the Dilemma. PLoS ONE 6(10): e25339
 * doi:10.1371/journal.pone.0025339
 *
 *
 * Log-STDP pushes weight values toward a log normal distribution. This can
 * help with weight divergence, which is common to Add and Mlt STDP. It also
 * allows for specialization of synapses, and fits very well with experimental
 * data concerning synaptic efficacy.
 *
 * @author Zoë Tosi
 */
class LogSTDPRule : STDPRule() {
    /**
     * The value that for a given synapse, if the absolute value of the synapses
     * weight is below this threshold it is governed by a different LTD rule
     * than if it were being subjected to LTD, but had a weight greater than
     * this value. LTD for weights below this value linearly approach 0 as the
     * weight approaches 0. LTD for weights above this value are related
     * logarithmically to the synapse strength.
     *
     *
     * J_0 in the cited paper.
     */
    var smallWtThreshold: Double = 1.0

    /**
     * A constant for LTP. c_+ in the cited paper.
     */
    override var w_plus: Double = 2.0

    /**
     * A constant for LTD. c_- in the cited paper.
     */
    override var w_minus: Double = 1.0

    /**
     * The degree to which the distribution is pushed logarithmically. Has an
     * effect on how strongly LTD pushes larger weights toward the small weight
     * threshold.
     *
     *
     * alpha in the cited paper.
     */
    var logSaturation: Double = 5.0

    /**
     * A moderating constant for LTP, causing LTP to behave noticeably
     * differently for weights greater than ltpMod * smallWtThreshold.
     *
     *
     * beta in the cited paper.
     */
    var ltpMod: Double = 10.0

    /**
     * The variance of the noise applied to weight changes.
     */
    var noiseVar: Double = 0.6

    override var delta_w: Double = 0.0

    private val dist = NormalDistribution(0.0, noiseVar)

    /**
     * Updates the synapse's strength using Log-STDP.
     */
    context(Network)
    override fun apply(s: Synapse, data: EmptyScalarData) {
        val sourceSpiking = s.source.updateRule.isSpikingRule
        val targetSpiking = s.target.updateRule.isSpikingRule
        if (!sourceSpiking || !targetSpiking) {
            return  // STDP is non-sensical if one of the units doesn't spike...
        }
        //        final double delay = synapse.getDelay() * timeStep;
        //        if (synapse.getStrength() >= 0) {
        val delta_t = s.source.lastSpikeTime - (s.target.lastSpikeTime)

        //        } else {
        //        	delta_t = tar.getLastSpikeTime()
        //        			- (src.getLastSpikeTime());
        //        }
        if (s.strength >= 0) {
            val noise = 1 + dist.sampleDouble()
            if (delta_t < 0) {
                calcW_plusTerm(s)
                delta_w = timeStep * learningRate * (w_plus * exp(delta_t / tau_plus)) * (1 + noise)
            } else if (delta_t > 0) {
                calcW_minusTerm(s)
                delta_w = timeStep * learningRate * (-w_minus * exp(-delta_t / tau_minus)) * (1 + noise)
            } else {
                delta_w = 0.0
            }
        } else if (s.strength <= 0) {
            delta_w = if (delta_t > 0) {
                learningRate * 1.5 * exp(-delta_t / tau_plus)
            } else if (delta_t < 0) {
                learningRate * -1 * exp(delta_t / tau_minus)
            } else {
                0.0
            }
        }
        s.strength = s.clip(s.strength - delta_w)
    }


    /**
     * @param s
     * @return
     */
    private fun calcW_plusTerm(s: Synapse): Double {
        w_plus = w_plus * exp(-abs(s.strength) / (smallWtThreshold * ltpMod))
        // if (s.getStrength() > 0) {
        // if (s.getStrength() >= s.getUpperBound()) {
        // w_plus = 0;
        // } else {
        // w_plus *= Math.exp(-20 * Math.pow(s.getStrength()
        // / (s.getUpperBound() - s.getStrength()), 2));
        // }
        // } else {
        // if (s.getStrength() <= s.getLowerBound()) {
        // w_plus = 0;
        // } else {
        // w_plus *= Math.exp(-20 * Math.pow(s.getStrength()
        // / (s.getLowerBound() - s.getStrength()), 2));
        // }
        // }
        return w_plus
    }

    /**
     * @param s
     * @return
     */
    private fun calcW_minusTerm(s: Synapse): Double {
        val wt = abs(s.strength)
        if (wt <= smallWtThreshold) {
            w_minus = w_minus * wt / smallWtThreshold
        } else {
            val numerator = ln(1 + (logSaturation * ((wt / smallWtThreshold) - 1)))
            w_minus = w_minus * (1 + (numerator / logSaturation))
        }
        // if (s.getStrength() < 0) {
        // if (s.getStrength() >= s.getUpperBound()) {
        // w_minus = 0;
        // } else {
        // w_minus *= Math.exp(-0.25 * Math.pow((s.getLowerBound()
        // - s.getStrength()) / (s.getUpperBound()
        // - s.getStrength()), 2));
        // }
        // } else {
        // if (s.getStrength() <= s.getLowerBound()) {
        // w_minus = 0;
        // } else {
        // w_minus *= Math.exp(-0.25 * Math.pow((s.getUpperBound()
        // - s.getStrength()) / (s.getLowerBound()
        // - s.getStrength()), 2));
        // }
        // }
        return w_minus
    }
}
