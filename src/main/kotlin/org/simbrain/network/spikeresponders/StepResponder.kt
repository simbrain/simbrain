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
 * Responds to a spike with a step response for a set number of iterations.
 */
class StepResponder(
    /**
     * Response height: The value by which the strength of the synapse is scaled
     * to determine the post synaptic response.
     */
    @UserParameter(
        label = "Response height", description = "This value is multiplied by"
                + " the strength to determine the total instantaneous rise in a post-synaptic"
                + " response to an action potential or spike.", increment = .1, order = 1
    )
    var responseHeight: Double = 1.0,

    /**
     * Response duration (ms).
     */
    @UserParameter(label = "Response time", description = "Response duration (ms)", increment = 1.0, order = 1)
    var responseDuration: Int = 1

) : SpikeResponder() {

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val weightMatrix = connector as WeightMatrix
        val lastSpikeTimes = ((weightMatrix.source as NeuronArray).dataHolder as SpikingMatrixData).lastSpikeTimes
        for (i in 0 until connector.psrMatrix.ncol()) {
            for (j in 0 until connector.psrMatrix.nrow()) {
                if (lastSpikeTimes[i] + responseDuration * timeStep >= time && probabilisticSpikeCheck()) {
                    connector.psrMatrix[j, i] = responseHeight * connector.weightMatrix[j, i]
                } else {
                    connector.psrMatrix[j, i] = 0.0
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        if (synapse.source.lastSpikeTime + responseDuration * timeStep >= time && probabilisticSpikeCheck()) {
            synapse.psr = responseHeight * synapse.strength
        } else {
            synapse.psr = 0.0
        }
    }


    override fun copy(): StepResponder {
        val st = StepResponder()
        st.responseHeight = responseHeight
        st.responseDuration = responseDuration
        return st
    }

    override val description: String = "Step"

    override val name: String
        get() = "Step"
}
