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
import kotlin.math.exp
import kotlin.math.sign

/**
 * **STDPSynapse** models spike time dependent plasticity.
 *
 *
 * Only works if source and target neurons are spiking neurons.
 *
 *
 * Drew on: Jean-Philippe Thivierge and Paul Cisek (2008), Journal of
 * Neuroscience. Nonperiodic Synchronization in Heterogeneous Networks of
 * Spiking Neurons. Also drew on the Scholarpedia article.
 */
open class STDPRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData> {
    // TODO: check description
    /**
     * Time constant for LTD.
     */
    @UserParameter(label = "Tau minus", description = "Time constant " + "for LTD.", increment = .1, order = 0)
    var tau_minus: Double = 60.0

    /**
     * Time constant for LTP.
     */
    @UserParameter(label = "Tau plus", description = "Time constant " + "for LTP.", increment = .1, order = 1)
    var tau_plus: Double = 30.0

    /**
     * Learning rate for LTP case. Controls magnitude of LTP changes.
     */
    @UserParameter(
        label = "W+",
        description = "Learning rate for " + "LTP case. Controls magnitude of LTP changes.",
        increment = .1,
        order = 2
    )
    open var w_plus: Double = 10.0

    /**
     * Learning rate for LTP case. Controls magnitude of LTD changes.
     */
    @UserParameter(
        label = "W-",
        description = "Learning rate for " + "LTP case. Controls magnitude of LTD changes.",
        increment = .1,
        order = 3
    )
    open var w_minus: Double = 10.0

    /**
     * General learning rate.
     */
    @UserParameter(label = "Learning rate", description = "General learning " + "rate.", increment = .1, order = 4)
    var learningRate: Double = 0.01

    /**
     * Sets whether or not STDP acts directly on W or dW/dt
     */
    @UserParameter(
        label = "Smooth STDP",
        description = "Whether STDP acts directly on weight or on its derivative instead",
        order = 5
    )
    var isContinuous: Boolean = false

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "STDP"

    constructor()

    constructor(toCpy: STDPRule) : this(
        toCpy.w_plus, toCpy.w_minus, toCpy.tau_plus,
        toCpy.tau_minus, toCpy.learningRate, toCpy.isContinuous
    )

    constructor(
        w_plus: Double,
        w_minus: Double,
        tau_plus: Double,
        tau_minus: Double,
        learningRate: Double,
        continuous: Boolean
    ) {
        this.w_plus = w_plus
        this.w_minus = w_minus
        this.tau_plus = tau_plus
        this.tau_minus = tau_minus
        this.learningRate = learningRate
        this.isContinuous = continuous
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        val duplicateSynapse = STDPRule()
        duplicateSynapse.tau_minus = tau_minus
        duplicateSynapse.tau_plus = tau_plus
        duplicateSynapse.w_minus = w_minus
        duplicateSynapse.w_plus = w_plus
        duplicateSynapse.learningRate = learningRate
        duplicateSynapse.isHebbian = isHebbian
        return duplicateSynapse
    }

    var isHebbian: Boolean = true

    open var delta_w: Double = 0.0

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val strength = synapse.strength
        if (synapse.source.isSpike || synapse.target.isSpike) {
            try {
                val delta_t = ((synapse.source.lastSpikeTime
                        - synapse.target.lastSpikeTime)
                        * (if (isHebbian) 1 else -1))
                if (delta_t < 0) {
                    delta_w = w_plus * exp(delta_t / tau_plus) * learningRate
                } else if (delta_t > 0) {
                    delta_w = -w_minus * exp(-delta_t / tau_minus) * learningRate
                }
            } catch (cce: ClassCastException) {
                cce.printStackTrace()
                println("Don't use non-spiking neurons with STDP!")
            }
            if (!isContinuous && sign(strength) == -1.0) {
                synapse.strength = strength - delta_w * timeStep
            } else {
                synapse.strength = strength + delta_w * timeStep
            }
        }

        if (isContinuous && sign(strength) == -1.0) {
            synapse.strength = strength - delta_w * timeStep
        } else {
            synapse.strength = strength + delta_w * timeStep
        }
    }
}
