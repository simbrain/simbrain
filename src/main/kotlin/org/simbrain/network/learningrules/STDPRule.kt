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
 * Models spike time dependent plasticity STDP.
 *
 * Assumes source and target neurons are spiking neurons.
 *
 * See: Jean-Philippe Thivierge and Paul Cisek (2008), Nonperiodic Synchronization in Heterogeneous Networks of  Spiking Neurons
 * and the Scholarpedia article on STDP.
 *
 * Also on anti-hebbian stdp: https://journals.physiology.org/doi/pdf/10.1152/jn.00551.2006
 */
open class STDPRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData> {

    @UserParameter(label = "Tau minus", description = "Time constant " + "for LTD.", increment = .1, order = 0)
    var tauMinus: Double = 60.0

    @UserParameter(label = "Tau plus", description = "Time constant " + "for LTP.", increment = .1, order = 1)
    var tauPlus: Double = 30.0

    @UserParameter(
        label = "W+",
        description = "Learning rate for " + "LTP case. Controls magnitude of LTP changes.",
        increment = .1,
        order = 2
    )
    open var wPlus: Double = 10.0

    @UserParameter(
        label = "W-",
        description = "Learning rate for " + "LTP case. Controls magnitude of LTD changes.",
        increment = .1,
        order = 3
    )
    open var wMinus: Double = 10.0

    @UserParameter(label = "Learning rate", description = "General learning " + "rate.", increment = .1, order = 4)
    var learningRate: Double = 0.01

    @UserParameter(
        label = "Hebbian",
        description = "If true, hebbian learning, else anti-hebbian",
        order = 10
    )
    var isHebbian: Boolean = true

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "STDP"

    constructor()

    constructor(
        w_plus: Double,
        w_minus: Double,
        tau_plus: Double,
        tau_minus: Double,
        learningRate: Double,
        continuous: Boolean
    ) {
        this.wPlus = w_plus
        this.wMinus = w_minus
        this.tauPlus = tau_plus
        this.tauMinus = tau_minus
        this.learningRate = learningRate
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        val duplicateSynapse = STDPRule()
        duplicateSynapse.tauMinus = tauMinus
        duplicateSynapse.tauPlus = tauPlus
        duplicateSynapse.wMinus = wMinus
        duplicateSynapse.wPlus = wPlus
        duplicateSynapse.learningRate = learningRate
        duplicateSynapse.isHebbian = isHebbian
        return duplicateSynapse
    }

    open var deltaW: Double = 0.0

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        if (synapse.source.isSpike || synapse.target.isSpike) {
            val deltaT = ((synapse.source.lastSpikeTime
                    - synapse.target.lastSpikeTime)
                    * (if (isHebbian) 1 else -1))
            if (deltaT < 0) {
                // LTP Case
                deltaW = wPlus * exp(deltaT / tauPlus) * learningRate
            } else if (deltaT > 0) {
                // LTD Case
                deltaW = -wMinus * exp(-deltaT / tauMinus) * learningRate
            }

            synapse.strength +=  deltaW * timeStep
        }

    }
}
