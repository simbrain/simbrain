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

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter

/**
 * When a spike occurs the jump to a max value (the synaptic strength) and then decay to baseline value..
 */
class JumpAndDecay : SpikeResponder() {

    /**
     * Base line value.
     */
    @UserParameter(
        label = "Base-Line",
        description = "The post-synaptic response value when no spike have occurred. Alternatively, the "
                + "post synaptic response to which decays to over time.",
        increment = .1,
        order = 2
    )
    var baseLine = 0.0

    @UserParameter(
        label = "Time Constant",
        description = "Time constant of decay (ms). Roughly the time it takes to decay to\n" +
                "near-baseline. Larger time constants produce slower decay.",
        increment = .1,
        order = 3
    )
    var timeConstant = 3.0

    @UserParameter(
        label = "Use Convolution",
        description = "If true the current spike response adds the psr from the previous iteration, which smoothes out the response.",
        order = 4
    )
    var useConvolution = false

    override fun copy(): JumpAndDecay {
        val jad = JumpAndDecay()
        jad.baseLine = baseLine
        jad.timeConstant = timeConstant
        return jad
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrow()) {
                for (j in 0 until wm.weightMatrix.ncol()) {
                    val psr = jumpAndDecay(
                            spikeData.spikes[j],
                            wm.psrMatrix[i, j],
                            wm.weightMatrix[i, j],
                            timeStep
                        )
                    wm.psrMatrix.set(i, j, psr)
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        synapse.psr = jumpAndDecay(
            synapse.source.isSpike, synapse.psr, synapse.strength, timeStep
        )
    }

    context(Network)
    fun jumpAndDecay(
        spiked: Boolean,
        psr: Double,
        jumpHeight: Double,
        timeStep: Double): Double {
        return if (spiked && probabilisticSpikeCheck()) {
            jumpHeight + (if (useConvolution) psr else 0.0)
        } else {
            psr + timeStep * ((baseLine - psr) / timeConstant)
        }
    }

    override val description: String = "Jump and Decay"

    override val name: String
        get() = "Jump and Decay"
}