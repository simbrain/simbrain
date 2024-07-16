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
import smile.math.matrix.Matrix

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
    @UserParameter(label = "Response time", description = "Response duration (ms)", increment = .1, order = 1)
    var responseDuration: Int = 1

) : SpikeResponder() {

    context(Network)
    override fun apply(conn: Connector, data: MatrixDataHolder) {
        val wm = conn.let { if (it is WeightMatrix) it else return }
        val na = conn.source.let { if (it is NeuronArray) it else return }
        val stepResponseData = data.let { if (it is StepMatrixData) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            spikeData.spikes.forEachIndexed { col, spiked ->
                if (spiked) {
                    for (row in 0 until stepResponseData.counterMatrix.nrow()) {
                        stepResponseData.counterMatrix.set(row, col, responseDuration.toDouble())
                        wm.psrMatrix.set(row, col, responseHeight * wm.weightMatrix.get(row, col))
                    }
                } else {
                    for (row in 0 until stepResponseData.counterMatrix.nrow()) {
                        stepResponseData.counterMatrix.set(row, col, stepResponseData.counterMatrix.get(row, col) - 1)
                        if (stepResponseData.counterMatrix.get(row, col) < 0) {
                            stepResponseData.counterMatrix.set(row, col, 0.0)
                        }
                    }
                }
                for (i in 0 until stepResponseData.counterMatrix.nrow())
                    for (j in 0 until stepResponseData.counterMatrix.ncol()) {
                        if (stepResponseData.counterMatrix.get(i, j) <= 0) {
                            wm.psrMatrix.set(i, j, 0.0)
                        }
                    }
            }
        }

    }

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return StepMatrixData(rows, cols)
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        val data = responderData as StepResponderData
        if (synapse.source.isSpike) {
            data.counter = responseDuration
            synapse.psr = responseHeight * synapse.strength
        } else {
            data.counter = data.counter - 1
            if (data.counter < 0) {
                data.counter = 0
            }
        }
        if (data.counter <= 0) {
            synapse.psr = 0.0
        }
    }

    override fun createResponderData(): ScalarDataHolder {
        return StepResponderData()
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

class StepResponderData(
    @UserParameter(
        label = "Counter", description = "Used to count down the step function. Each iteration is as long as whatever" +
                "the network time step"
    )
    var counter: Int = 0,
) : ScalarDataHolder {
    override fun copy(): StepResponderData {
        return StepResponderData(counter)
    }
}

class StepMatrixData(val rows: Int, val cols: Int) : MatrixDataHolder {
    var counterMatrix = Matrix(rows, cols)
    override fun copy() = StepMatrixData(rows, cols).also {
        it.counterMatrix = counterMatrix.clone()
    }
}