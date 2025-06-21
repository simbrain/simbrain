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

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **HebbianThresholdSynapse**.
 */
class HebbianThresholdRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {
    // TODO: check description
    /**
     * Learning rate.
     */
    @UserParameter(
        label = "Learning rate",
        description = "Learning rate for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var learningRate: Double = 0.1

    /**
     * Output threshold.
     */
    @UserParameter(
        label = "Threshold",
        description = "Output threshold for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var outputThreshold: Double = .5

    /**
     * Output threshold momentum.
     */
    @UserParameter(
        label = "Threshold Momentum",
        description = "Output threshold momentum for Hebb threshold rule",
        increment = .1,
        order = 1
    )
    var outputThresholdMomentum: Double = .1

    /**
     * Use sliding output threshold.
     */
    @UserParameter(
        label = "Sliding Threshold",
        description = "Use sliding output threshold for Hebb threshold rule",
        order = 1
    )
    var useSlidingOutputThreshold: Boolean = false

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Hebbian Threshold"

    override fun copy(): HebbianThresholdRule {
        val h = HebbianThresholdRule()
        h.learningRate = learningRate
        h.outputThreshold = outputThreshold
        h.outputThresholdMomentum = outputThresholdMomentum
        h.useSlidingOutputThreshold = useSlidingOutputThreshold
        return h
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        val input = synapse.source.activation
        val output = synapse.target.activation

        if (useSlidingOutputThreshold) {
            outputThreshold += (outputThresholdMomentum * ((output * output) - outputThreshold))
        }
        synapse.strength += learningRate * input * output * (output - outputThreshold)
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val input = (connector.source as NeuronArray).activations
            val output = (connector.target as NeuronArray).activations
            
            // Matrix-based Hebbian Threshold: vectorized version
            // Note: For simplicity, we use a global threshold for all neurons in matrix version
            // In a more sophisticated implementation, we might maintain per-neuron thresholds
            
            if (useSlidingOutputThreshold) {
                // Update threshold based on average output squared
                var sum = 0.0
                for (i in 0 until output.nrow()) {
                    val value = output[i, 0]
                    sum += value * value
                }
                val avgOutputSquared = sum / output.nrow()
                outputThreshold += (outputThresholdMomentum * (avgOutputSquared - outputThreshold))
            }
            
            // For each target neuron (row)
            for (i in 0 until wm.nrow()) {
                val outputActivation = output[i, 0]
                for (j in 0 until wm.ncol()) {
                    val inputActivation = input[j, 0]
                    wm[i, j] += learningRate * inputActivation * outputActivation * (outputActivation - outputThreshold)
                }
            }
        }
    }
}
