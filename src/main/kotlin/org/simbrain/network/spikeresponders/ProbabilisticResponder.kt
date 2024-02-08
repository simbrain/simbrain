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
 * Probabilistic spike responders produces a response with some probability. If a response is produced it is set
 * equal to the pre-synaptic weight.
 */
class ProbabilisticResponder : SpikeResponder() {

    @UserParameter(
        label = "Activation Probability",
        description = "Probability of producing an output; must be between 0 and 1.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 1
    )
    var activationProbability = .5

    override fun deepCopy(): ProbabilisticResponder {
        val pr = ProbabilisticResponder()
        pr.activationProbability = activationProbability
        return pr
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrow()) {
                for (j in 0 until wm.weightMatrix.ncol()) {
                    val psr = probResponder(spikeData.spikes[j]) * wm.weightMatrix[i,j]
                    wm.psrMatrix.set(i,j,psr)
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        synapse.psr = probResponder(synapse.source.isSpike) * synapse.strength
    }

    private fun probResponder(spiked: Boolean) : Double {
        return if (spiked) {
            if (Math.random() > 1 - activationProbability) {
                1.0
            } else {
                0.0
            }
        } else {
            0.0
        }
    }

    override val description: String = "Probabilistic"

    override val name: String
        get() = "Probabilistic"
}