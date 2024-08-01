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
 * TODO
 */
class RiseAndDecay : SpikeResponder() {

    /**
     * The time constant of decay and recovery (ms).
     */
    @UserParameter(
        label = "Time constant",
        description = "Time constant for rising decay (ms). Roughly the time it takes to rise to max value then decay" +
                "to near-baseline. Larger time constants produce slower changes.",
        increment = .1,
        order = 2
    )
    var timeConstant = 3.0

    override fun copy(): RiseAndDecay {
        val rad = RiseAndDecay()
        rad.timeConstant = timeConstant
        return rad
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector as WeightMatrix
        val na = connector.source as NeuronArray
        val responseData = responderData as RiseAndDecayMatrixData
        val spikeData = na.dataHolder as SpikingMatrixData
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrow()) {
                for (j in 0 until wm.weightMatrix.ncol()) {
                    val (psr, recovery) = riseAndDecay(
                        spikeData.spikes[j],
                        wm.psrMatrix[i, j],
                        responseData.recoveryMatrix[i,j],
                        wm.weightMatrix[i, j],
                        timeStep
                    )
                    wm.psrMatrix.set(i, j, psr)
                    responseData.recoveryMatrix.set(i,j, recovery)
                }
            }
        }
    }

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return RiseAndDecayMatrixData(rows, cols)
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        val data = responderData as RiseAndDecayData
        val (psr, recovery) = riseAndDecay(
            synapse.source.isSpike,
            synapse.psr,
            data.recovery,
            synapse.strength,
            timeStep
        )
        synapse.psr = psr
        data.recovery = recovery
    }

    context(Network)
    private fun riseAndDecay(spiked: Boolean,
                             psr: Double,
                             recovery: Double,
                             strength: Double,
                             timeStep: Double): Pair<Double, Double> {
        return Pair(
            (psr + ((timeStep / timeConstant) * (Math.E * strength * recovery * (1 - psr) - psr))),
            if (spiked && probabilisticSpikeCheck()) {
                1.0
            } else {
                recovery + timeStep / timeConstant * -recovery
            }
        )
    }

    override fun createResponderData(): ScalarDataHolder {
        return RiseAndDecayData()
    }

    override val description: String = "Rise and Decay"

    override val name: String
        get() = "Rise and Decay"
}


class RiseAndDecayData(
    @UserParameter(
        label = "Recovery"
    )
    var recovery: Double = 0.0,
) : ScalarDataHolder {
    override fun copy(): RiseAndDecayData {
        return RiseAndDecayData(recovery)
    }
}


class RiseAndDecayMatrixData(val rows: Int, val cols: Int) : MatrixDataHolder {
    var recoveryMatrix = Matrix(rows, cols)
    override fun copy() = RiseAndDecayMatrixData(rows, cols).also {
        it.recoveryMatrix = recoveryMatrix.clone()
    }
}