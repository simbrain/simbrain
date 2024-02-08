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
package org.simbrain.network.learningrules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * Implementation of the model described by Pfister, J-P, Gerstner, W: Triplets
 * of Spikes in a Model of Spike Timing-Dependent Plasticity. J. Neurosci. 26,
 * 9673–9682 (2006).
 *
 *
 * Only works if source and target neurons are spiking neurons.
 *
 * @author Oliver J. Coleman
 */
class PfisterGerstner2006Rule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>(), Cloneable {
    @UserParameter(
        label = "Tau+",
        description = "Decay rate for r1 trace",
        minimumValue = 1.0,
        maximumValue = 100.0,
        increment = .1,
        order = 0
    )
    protected var tauPlus: Double = 16.8

    @UserParameter(
        label = "Tau x",
        description = "Decay rate for r2 trace",
        minimumValue = 1.0,
        maximumValue = 100.0,
        increment = .1,
        order = 1
    )
    protected var tauX: Double = 1.0

    @UserParameter(
        label = "Tau-",
        description = "Decay rate for o1 trace",
        minimumValue = 1.0,
        maximumValue = 100.0,
        increment = .1,
        order = 2
    )
    protected var tauNeg: Double = 33.7

    @UserParameter(
        label = "Tau y",
        description = "Decay rate for o2 trace",
        minimumValue = 1.0,
        maximumValue = 100.0,
        increment = .1,
        order = 3
    )
    protected var tauY: Double = 48.0

    /**
     * @return Amplitude of the weight change for a pre-post spike pair.
     */
    /**
     * @param a2p Amplitude of the weight change for a pre-post spike pair.
     */
    @UserParameter(
        label = "A2+",
        description = "Amplitude of the weight change for a pre-post spike pair.",
        minimumValue = 0.0,
        maximumValue = 0.1,
        increment = .1,
        order = 4
    )
    var a2P: Double = 0.0046

    /**
     * @return Amplitude of the weight change for a post-pre spike pair.
     */
    /**
     * @param a2n Amplitude of the weight change for a post-pre spike pair.
     */
    @UserParameter(
        label = "A2-",
        description = "Amplitude of the weight change for a post-pre spike pair.",
        minimumValue = 0.0,
        maximumValue = 0.1,
        increment = .1,
        order = 5
    )
    var a2N: Double = 0.003

    /**
     * @return Amplitude of the triplet term for potentiation.
     */
    /**
     * @param a3p Amplitude of the triplet term for potentiation.
     */
    @UserParameter(
        label = "A3+",
        description = "Amplitude of the triplet term for potentiation.",
        minimumValue = 0.0,
        maximumValue = 0.1,
        increment = .1,
        order = 6
    )
    var a3P: Double = 0.0091

    /**
     * @return Amplitude of the triplet term for depression.
     */
    /**
     * @param a3n Amplitude of the triplet term for depression.
     */
    @UserParameter(
        label = "A3-",
        description = "Amplitude of the triplet term for depression.",
        minimumValue = 0.0,
        maximumValue = 0.1,
        increment = .1,
        order = 7
    )
    var a3N: Double = 0.0

    // Spike traces.
    private var r1 = 0.0
    private var r2 = 0.0
    private var o1 = 0.0
    private var o2 = 0.0

    // Cached multipliers for trace decays.
    private var tauPlusMult = 0.0
    private var tauXMult = 0.0
    private var tauNegMult = 0.0
    private var tauYMult = 0.0

    override fun init(synapse: Synapse) {
        tauPlusMult = 1 / tauPlus
        tauXMult = 1 / tauX
        tauNegMult = 1 / tauNeg
        tauYMult = 1 / tauY
    }

    override val name: String
        get() = "Pfister and Gerstner, 2006"

    override fun deepCopy(): SynapseUpdateRule<*, *> {
        // We're only using primitive fields so clone() works.
        try {
            return clone() as PfisterGerstner2006Rule
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val preSpiked = synapse.source.isSpike
        val postSpiked = synapse.target.isSpike

        // Need current values for these traces for strength update equations
        // below.
        val r2p = r2
        val o2p = o2

        // Update trace values.
        if (preSpiked) {
            r1 = 1.0
            r2 = 1.0
        } else {
            r1 -= r1 * tauPlusMult * timeStep
            r2 -= r2 * tauXMult * timeStep
        }
        if (postSpiked) {
            o1 = 1.0
            o2 = 1.0
        } else {
            o1 -= o1 * tauNegMult * timeStep
            o2 -= o2 * tauYMult * timeStep
        }

        // Update efficacy if a pre or post spike occurred.
        if (preSpiked) {
            synapse.strength = synapse.strength - o1 * (a2N + a3N * r2p)
        }

        if (postSpiked) {
            synapse.strength = synapse.strength + r1 * (a2P + a3P * o2p)
        }
    }

    var tauPlusDecay: Double
        /**
         * @return Decay rate for r1 trace.
         */
        get() = 1 / tauPlusMult
        /**
         * @param tauPlus Decay rate for r1 trace.
         */
        set(tauPlus) {
            this.tauPlusMult = 1 / tauPlus
        }

    var tauXDecay: Double
        /**
         * @return Decay rate for r2 trace.
         */
        get() = 1 / tauXMult
        /**
         * @param tauX Decay rate for r2 trace.
         */
        set(tauX) {
            this.tauXMult = 1 / tauX
        }

    var tauNegDecay: Double
        /**
         * @return Decay rate for o1 trace.
         */
        get() = 1 / tauNegMult
        /**
         * @param tauNeg Decay rate for o1 trace.
         */
        set(tauNeg) {
            this.tauNegMult = 1 / tauNeg
        }

    var tauYDecay: Double
        /**
         * @return Decay rate for o2 trace.
         */
        get() = 1 / tauYMult
        /**
         * @param tauY Decay rate for o2 trace.
         */
        set(tauY) {
            this.tauYMult = 1 / tauY
        }
}
